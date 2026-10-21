CREATE TABLE tratamientos_conservacion (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 actividad_id uuid UNIQUE REFERENCES actividades(id) ON DELETE SET NULL,
 version integer NOT NULL DEFAULT 1 CHECK(version=1),
 fase varchar(25) NOT NULL DEFAULT 'PENDIENTE'
   CHECK(fase IN ('PENDIENTE','PROCESANDO','COPIAS_PENDIENTES','COMPLETADO')),
 vence_en timestamptz, procesado_en timestamptz,
 CHECK(fase<>'COMPLETADO' OR (actividad_id IS NULL AND vence_en IS NULL AND procesado_en IS NULL))
);
CREATE TABLE estadisticas_anonimas (
 anio integer NOT NULL,
 modalidad varchar(10) NOT NULL CHECK(modalidad IN ('CLASE','EVENTO')),
 version integer NOT NULL CHECK(version=1),
 participantes bigint NOT NULL CHECK(participantes>=10),
 PRIMARY KEY(anio,modalidad,version)
);
-- Inventario de toda copia recuperable: una copia sin clasificación se considera afectada.
-- Ningún endpoint HTTP escribe estas tablas. Solo el operador de conservación.
CREATE TABLE catalogo_copias (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 soporte varchar(10) NOT NULL CHECK(soporte IN ('BACKUP','WAL','EXPORT')),
 referencia text NOT NULL UNIQUE CHECK(length(btrim(referencia))>0),
 creada_en timestamptz NOT NULL,
 saneada_hasta timestamptz,
 estado varchar(15) NOT NULL DEFAULT 'RECUPERABLE'
   CHECK(estado IN ('RECUPERABLE','RETIRADA')),
 verificada_en timestamptz, evidencia text,
 CHECK(estado<>'RETIRADA' OR (verificada_en IS NOT NULL AND length(btrim(evidencia))>0))
);
CREATE TABLE politica_copias (
 id integer PRIMARY KEY CHECK(id=1),
 inventario_verificado_en timestamptz,
 respaldo_saneado_en timestamptz,
 evidencia_retiro text
);
INSERT INTO politica_copias(id) VALUES (1);
CREATE INDEX actividades_vencidas_idx ON actividades(anonimizar_en,id)
 WHERE anonimizar_en IS NOT NULL;

-- Permitir borrado exclusivamente durante el saneamiento de una actividad vencida.
-- Las actualizaciones y las eliminaciones ordinarias siguen siendo inválidas.
CREATE OR REPLACE FUNCTION proteger_revision_asistencia() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' AND current_setting('geoasist.retention_cutoff',true) IS NOT NULL
    AND current_setting('geoasist.retention_cutoff',true)<>''
    AND EXISTS (
      SELECT 1 FROM intentos_asistencia i JOIN sesiones s ON s.id=i.sesion_id
       JOIN actividades a ON a.id=s.actividad_id
       WHERE i.id=OLD.intento_id AND a.anonimizar_en<=
         current_setting('geoasist.retention_cutoff')::timestamptz) THEN
   RETURN OLD;
 END IF;
 RAISE EXCEPTION 'Decisión de revisión inmutable';
END $$;

CREATE FUNCTION construir_estadisticas(ids uuid[], corte timestamptz) RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE inserted integer;
BEGIN
 -- Celdas anuales selladas: sin totales, sin desglose de carrera/espacio, sin actualizaciones
 -- incrementales que permitan diferencias. Se descartan grupos menores o lotes posteriores.
 INSERT INTO estadisticas_anonimas(anio,modalidad,version,participantes)
 SELECT extract(year FROM a.cerrada_en AT TIME ZONE 'UTC')::integer,
        a.modalidad,1,count(DISTINCT x.persona_id)
   FROM actividades a JOIN sesiones s ON s.actividad_id=a.id
   JOIN asistencias x ON x.sesion_id=s.id
  WHERE a.id=ANY(ids)
    AND date_trunc('year',a.cerrada_en AT TIME ZONE 'UTC')+INTERVAL '6 years'
        <= corte AT TIME ZONE 'UTC'
  GROUP BY 1,2
 HAVING count(DISTINCT x.persona_id)>=10 AND count(DISTINCT a.id)>=2
 ON CONFLICT DO NOTHING;
 GET DIAGNOSTICS inserted=ROW_COUNT;
 RETURN inserted;
END $$;

CREATE FUNCTION completar_tratamientos() RETURNS integer LANGUAGE plpgsql AS $$
DECLARE completed integer;
BEGIN
 UPDATE tratamientos_conservacion t
    SET fase='COMPLETADO',actividad_id=NULL,vence_en=NULL,procesado_en=NULL
  WHERE t.fase='COPIAS_PENDIENTES' AND t.actividad_id IS NULL
    AND EXISTS (SELECT 1 FROM politica_copias p WHERE p.id=1
      AND p.inventario_verificado_en>=t.procesado_en
      AND p.respaldo_saneado_en>=t.procesado_en
      AND length(btrim(p.evidencia_retiro))>0)
    AND NOT EXISTS (SELECT 1 FROM catalogo_copias c
      WHERE c.estado='RECUPERABLE'
        AND (c.saneada_hasta IS NULL OR c.saneada_hasta<t.procesado_en));
 GET DIAGNOSTICS completed=ROW_COUNT;
 RETURN completed;
END $$;

CREATE FUNCTION anonimizar_vencidas(corte timestamptz, maximo integer DEFAULT 100)
RETURNS integer LANGUAGE plpgsql AS $$
DECLARE ids uuid[]; persons uuid[]; spaces uuid[]; removed integer;
BEGIN
 IF maximo<1 OR maximo>1000 OR corte IS NULL THEN RAISE EXCEPTION 'Lote inválido'; END IF;
 -- Serializa lotes y su inventario; los comandos de asistencia mantienen su orden habitual.
 PERFORM pg_advisory_xact_lock(8008001);
 SELECT array_agg(id ORDER BY id) INTO ids FROM (
   SELECT id FROM actividades WHERE anonimizar_en<=corte ORDER BY id
     LIMIT maximo FOR UPDATE) a;
 IF ids IS NULL THEN PERFORM completar_tratamientos(); RETURN 0; END IF;
 PERFORM id FROM sesiones WHERE actividad_id=ANY(ids) ORDER BY id FOR UPDATE;
 SELECT array_agg(DISTINCT p.id ORDER BY p.id) INTO persons FROM personas p
 WHERE p.tipo='EXTERNA' AND (
   EXISTS(SELECT 1 FROM invitados_evento g WHERE g.actividad_id=ANY(ids) AND g.persona_id=p.id)
   OR EXISTS(SELECT 1 FROM intentos_asistencia i JOIN sesiones s ON s.id=i.sesion_id
     WHERE s.actividad_id=ANY(ids) AND i.persona_id=p.id));
 PERFORM id FROM personas WHERE id=ANY(persons) ORDER BY id FOR UPDATE;
 SELECT array_agg(DISTINCT espacio_id) INTO spaces FROM sesiones WHERE actividad_id=ANY(ids);
 INSERT INTO tratamientos_conservacion(actividad_id,fase,vence_en)
 SELECT id,'PROCESANDO',anonimizar_en FROM actividades WHERE id=ANY(ids)
 ON CONFLICT(actividad_id) DO UPDATE SET fase='PROCESANDO';
 PERFORM construir_estadisticas(ids,corte);
 PERFORM set_config('geoasist.retention_cutoff',corte::text,true);
 DELETE FROM revisiones_asistencia WHERE intento_id IN (
   SELECT i.id FROM intentos_asistencia i JOIN sesiones s ON s.id=i.sesion_id WHERE s.actividad_id=ANY(ids));
 DELETE FROM idempotencias WHERE actividad_id=ANY(ids);
 DELETE FROM inicios_invitado WHERE sesion_id IN (SELECT id FROM sesiones WHERE actividad_id=ANY(ids));
 DELETE FROM asistencias WHERE sesion_id IN (SELECT id FROM sesiones WHERE actividad_id=ANY(ids));
 -- FK en cascada retira intentos, tokens, configuraciones, autorizaciones, auditoría y sesiones.
 DELETE FROM actividades WHERE id=ANY(ids);
 GET DIAGNOSTICS removed=ROW_COUNT;
 UPDATE tratamientos_conservacion SET fase='COPIAS_PENDIENTES',procesado_en=corte,vence_en=NULL
 WHERE fase='PROCESANDO' AND actividad_id IS NULL;
 -- No conservar tampoco nombres/centros de espacios que ya carecen de propósito vigente.
 DELETE FROM evidencias_geograficas e WHERE e.espacio_id=ANY(spaces)
   AND NOT EXISTS(SELECT 1 FROM sesiones s WHERE s.espacio_id=e.espacio_id)
   AND NOT EXISTS(SELECT 1 FROM configuracion_geografica c WHERE c.evidencia_id=e.id);
 DELETE FROM espacios e WHERE e.id=ANY(spaces)
   AND NOT EXISTS(SELECT 1 FROM sesiones s WHERE s.espacio_id=e.id)
   AND NOT EXISTS(SELECT 1 FROM evidencias_geograficas g WHERE g.espacio_id=e.id);
 DELETE FROM personas p WHERE p.id=ANY(persons)
   AND NOT EXISTS(SELECT 1 FROM cuentas c WHERE c.persona_id=p.id)
   AND NOT EXISTS(SELECT 1 FROM invitados_evento g WHERE g.persona_id=p.id)
   AND NOT EXISTS(SELECT 1 FROM estudiantes_autorizados e WHERE e.persona_id=p.id)
   AND NOT EXISTS(SELECT 1 FROM intentos_asistencia i WHERE i.persona_id=p.id)
   AND NOT EXISTS(SELECT 1 FROM asistencias a WHERE a.persona_id=p.id)
   AND NOT EXISTS(SELECT 1 FROM inicios_invitado g WHERE g.persona_vinculada_id=p.id OR g.persona_consumida_id=p.id);
 PERFORM set_config('geoasist.retention_cutoff','',true);
 PERFORM completar_tratamientos();
 RETURN removed;
END $$;

COMMENT ON TABLE tratamientos_conservacion IS 'Control restringido sin enlace a estadísticas. Al completar solo UUID aleatorio, versión y estado. DELETE lógico no demuestra borrado físico.';
COMMENT ON TABLE estadisticas_anonimas IS 'Celdas anuales conservadoras sin endpoint público; diez personas es umbral técnico, no garantía institucional. Sin totales ni publicación incremental.';
