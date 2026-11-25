-- 003: ausencias capturadas al cerrar la sesion, justificaciones y asistencia manual.
CREATE TABLE ausencias (
 sesion_id uuid NOT NULL REFERENCES sesiones(id) ON DELETE CASCADE,
 persona_id uuid NOT NULL REFERENCES personas(id),
 estado varchar(12) NOT NULL DEFAULT 'AUSENTE' CHECK(estado IN ('AUSENTE','JUSTIFICADA')),
 registrada_en timestamptz NOT NULL,
 PRIMARY KEY(sesion_id,persona_id));
CREATE INDEX ausencias_persona_idx ON ausencias(persona_id);

CREATE TABLE justificaciones (
 id uuid PRIMARY KEY, sesion_id uuid NOT NULL, persona_id uuid NOT NULL,
 origen varchar(12) NOT NULL CHECK(origen IN ('ESTUDIANTE','RESPONSABLE')),
 motivo varchar(2000) NOT NULL CHECK(length(btrim(motivo))>0),
 estado varchar(10) NOT NULL CHECK(estado IN ('PENDIENTE','APROBADA','RECHAZADA')),
 decidida_por uuid REFERENCES cuentas(id), razon_decision varchar(2000),
 solicitada_en timestamptz NOT NULL, decidida_en timestamptz,
 FOREIGN KEY(sesion_id,persona_id) REFERENCES ausencias(sesion_id,persona_id) ON DELETE CASCADE,
 CHECK(estado='PENDIENTE' OR (decidida_por IS NOT NULL AND decidida_en IS NOT NULL)));
CREATE UNIQUE INDEX justificaciones_pendiente_uk ON justificaciones(sesion_id,persona_id) WHERE estado='PENDIENTE';

CREATE FUNCTION justificaciones_inmutables() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.estado<>'PENDIENTE' THEN RAISE EXCEPTION 'Justificacion decidida inmutable'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER justificaciones_inmutables_tg BEFORE UPDATE ON justificaciones
 FOR EACH ROW EXECUTE FUNCTION justificaciones_inmutables();

ALTER TABLE asistencias DROP CONSTRAINT asistencias_forma_check;
ALTER TABLE asistencias ADD CONSTRAINT asistencias_forma_check
 CHECK (forma IN ('CREDENCIALES','INVITADO_LISTADO','REVISION','AUTORREGISTRO','MANUAL'));
ALTER TABLE asistencias ADD COLUMN razon_manual varchar(2000);
ALTER TABLE asistencias ADD COLUMN registrada_por uuid REFERENCES cuentas(id);
ALTER TABLE asistencias ADD CONSTRAINT asistencias_manual_ck
 CHECK (forma<>'MANUAL' OR (length(btrim(razon_manual))>0 AND registrada_por IS NOT NULL));
ALTER TABLE intentos_asistencia DROP CONSTRAINT intentos_asistencia_canal_check;
ALTER TABLE intentos_asistencia ADD CONSTRAINT intentos_asistencia_canal_check
 CHECK (canal IN ('CREDENCIALES','INVITADO_LISTADO','AUTORREGISTRO','MANUAL'));
