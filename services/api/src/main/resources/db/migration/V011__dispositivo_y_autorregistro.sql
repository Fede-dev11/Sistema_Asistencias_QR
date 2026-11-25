ALTER TABLE intentos_asistencia ADD COLUMN dispositivo_hash char(64);
ALTER TABLE asistencias ADD COLUMN dispositivo_hash char(64);

CREATE UNIQUE INDEX asistencias_sesion_dispositivo_uk
  ON asistencias(sesion_id, dispositivo_hash)
  WHERE dispositivo_hash IS NOT NULL;

ALTER TABLE asistencias DROP CONSTRAINT asistencias_forma_check;
ALTER TABLE asistencias ADD CONSTRAINT asistencias_forma_check
  CHECK (forma IN ('CREDENCIALES','INVITADO_LISTADO','REVISION','AUTORREGISTRO'));

ALTER TABLE intentos_asistencia ADD CONSTRAINT intentos_asistencia_canal_check
  CHECK (canal IN ('CREDENCIALES','INVITADO_LISTADO','AUTORREGISTRO'));
