CREATE TABLE horarios_clase (
 id uuid PRIMARY KEY, actividad_id uuid NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
 dias smallint[] NOT NULL CHECK(cardinality(dias)>0 AND dias <@ ARRAY[1,2,3,4,5,6,7]::smallint[]),
 hora_inicio time NOT NULL, hora_fin time NOT NULL, fecha_desde date NOT NULL, fecha_hasta date NOT NULL,
 espacio_id uuid REFERENCES espacios(id), zona varchar(40) NOT NULL DEFAULT 'America/Guayaquil',
 CHECK(hora_fin>hora_inicio), CHECK(fecha_hasta>=fecha_desde)
);

ALTER TABLE actividades
 ADD COLUMN acceso varchar(10),
 ADD COLUMN metodo_verificacion varchar(10) NOT NULL DEFAULT 'SOLO_QR'
  CHECK(metodo_verificacion IN ('QR_Y_GPS','SOLO_QR','SOLO_GPS','NINGUNO')),
 ADD COLUMN espacio_id uuid REFERENCES espacios(id);
UPDATE actividades SET acceso='CERRADO' WHERE modalidad='EVENTO';
ALTER TABLE actividades ADD CONSTRAINT actividades_acceso_ck
 CHECK((modalidad='EVENTO' AND acceso IN ('CERRADO','ABIERTO')) OR (modalidad='CLASE' AND acceso IS NULL));

ALTER TABLE actividades DROP CONSTRAINT actividades_estado_check;
ALTER TABLE actividades DROP CONSTRAINT actividades_check;
ALTER TABLE actividades ADD CONSTRAINT actividades_estado_check CHECK(estado IN ('ABIERTA','CERRADA','CANCELADA'));
ALTER TABLE actividades ADD CONSTRAINT actividades_cierre_ck CHECK(
 (estado='ABIERTA' AND cerrada_en IS NULL AND anonimizar_en IS NULL)
 OR (estado IN ('CERRADA','CANCELADA') AND cerrada_en IS NOT NULL AND anonimizar_en IS NOT NULL));

CREATE OR REPLACE FUNCTION proteger_cierre_actividad() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.estado IN ('CERRADA','CANCELADA') AND (NEW.estado<>OLD.estado OR NEW.cerrada_en IS DISTINCT FROM OLD.cerrada_en OR NEW.anonimizar_en IS DISTINCT FROM OLD.anonimizar_en) THEN
  RAISE EXCEPTION 'Cierre definitivo inmutable';
 END IF;
 RETURN NEW;
END $$;

ALTER TABLE sesiones DROP CONSTRAINT sesiones_estado_check;
ALTER TABLE sesiones ADD CONSTRAINT sesiones_estado_check CHECK(estado IN ('PROGRAMADA','ACTIVA','CERRADA','CANCELADA'));
ALTER TABLE sesiones
 ADD COLUMN metodo_verificacion varchar(10) NOT NULL DEFAULT 'SOLO_QR'
  CHECK(metodo_verificacion IN ('QR_Y_GPS','SOLO_QR','SOLO_GPS','NINGUNO')),
 ADD COLUMN horario_id uuid REFERENCES horarios_clase(id) ON DELETE SET NULL;

CREATE INDEX matriculas_paralelo_idx ON matriculas(paralelo_id) WHERE vigente;
CREATE INDEX docentes_paralelo_paralelo_idx ON docentes_paralelo(paralelo_id);
CREATE INDEX sesiones_horario_idx ON sesiones(horario_id);
