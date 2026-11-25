-- Los espacios son catálogo compartido (sesiones, actividades y horarios): la retención ya no los borra.
CREATE OR REPLACE FUNCTION anonimizar_vencidas(corte timestamptz, maximo integer DEFAULT 100)
RETURNS integer LANGUAGE plpgsql AS $$
DECLARE ids uuid[]; persons uuid[]; removed integer;
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

CREATE TABLE auditoria_espacios (
 id uuid PRIMARY KEY, espacio_id uuid NOT NULL REFERENCES espacios(id) ON DELETE CASCADE,
 actor_id uuid NOT NULL REFERENCES cuentas(id), accion varchar(100) NOT NULL,
 instante timestamptz NOT NULL, correlacion uuid NOT NULL,
 anterior jsonb NOT NULL DEFAULT '{}', nuevo jsonb NOT NULL DEFAULT '{}');
CREATE INDEX auditoria_espacios_idx ON auditoria_espacios(espacio_id,instante);
