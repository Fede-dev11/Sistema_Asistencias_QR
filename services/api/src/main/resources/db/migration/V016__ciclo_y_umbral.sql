ALTER TABLE actividades ADD COLUMN ciclo_automatico boolean NOT NULL DEFAULT false;
ALTER TABLE actividades ADD COLUMN umbral_asistencia numeric(5,2) NOT NULL DEFAULT 70
 CHECK(umbral_asistencia BETWEEN 0 AND 100);
ALTER TABLE sesiones ADD COLUMN siguiente_intento_en timestamptz;
CREATE INDEX sesiones_programadas_idx ON sesiones(ventana_inicio) WHERE estado='PROGRAMADA';
CREATE INDEX sesiones_activas_idx ON sesiones(ventana_fin) WHERE estado='ACTIVA';
ALTER TABLE auditoria_actividad ALTER COLUMN actor_id DROP NOT NULL;
