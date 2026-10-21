CREATE TABLE actividades (
 id uuid PRIMARY KEY, nombre varchar(200) NOT NULL, modalidad varchar(10) NOT NULL CHECK(modalidad IN ('CLASE','EVENTO')),
 responsable_id uuid NOT NULL REFERENCES cuentas(id), estado varchar(10) NOT NULL DEFAULT 'ABIERTA' CHECK(estado IN ('ABIERTA','CERRADA')),
 cerrada_en timestamptz, anonimizar_en timestamptz,
 CHECK ((estado='ABIERTA' AND cerrada_en IS NULL AND anonimizar_en IS NULL) OR (estado='CERRADA' AND cerrada_en IS NOT NULL AND anonimizar_en IS NOT NULL))
);
CREATE FUNCTION proteger_cierre_actividad() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.estado='CERRADA' AND (NEW.estado<>OLD.estado OR NEW.cerrada_en IS DISTINCT FROM OLD.cerrada_en OR NEW.anonimizar_en IS DISTINCT FROM OLD.anonimizar_en) THEN
  RAISE EXCEPTION 'Cierre definitivo inmutable';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER cierre_inmutable BEFORE UPDATE ON actividades FOR EACH ROW EXECUTE FUNCTION proteger_cierre_actividad();
CREATE TABLE actividad_carreras (actividad_id uuid NOT NULL REFERENCES actividades(id) ON DELETE CASCADE, carrera_id uuid NOT NULL REFERENCES carreras(id), PRIMARY KEY(actividad_id,carrera_id));
CREATE TABLE actividad_paralelos (actividad_id uuid NOT NULL, carrera_id uuid NOT NULL, paralelo_id uuid NOT NULL, PRIMARY KEY(actividad_id,paralelo_id), FOREIGN KEY(actividad_id,carrera_id) REFERENCES actividad_carreras(actividad_id,carrera_id) ON DELETE CASCADE, FOREIGN KEY(paralelo_id,carrera_id) REFERENCES paralelos(id,carrera_id));
CREATE TABLE estudiantes_autorizados (actividad_id uuid NOT NULL REFERENCES actividades(id) ON DELETE CASCADE, persona_id uuid NOT NULL REFERENCES personas(id), vigente boolean NOT NULL DEFAULT true, PRIMARY KEY(actividad_id,persona_id));
CREATE TABLE espacios (id uuid PRIMARY KEY, nombre varchar(200) NOT NULL, tipo varchar(40) NOT NULL);
CREATE TABLE sesiones (
 id uuid PRIMARY KEY, actividad_id uuid NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
 espacio_id uuid REFERENCES espacios(id), estado varchar(15) NOT NULL DEFAULT 'PROGRAMADA' CHECK(estado IN ('PROGRAMADA','ACTIVA','CERRADA')),
 ventana_inicio timestamptz NOT NULL, ventana_fin timestamptz NOT NULL, iniciada_en timestamptz, cerrada_en timestamptz,
 CHECK(ventana_fin>ventana_inicio), CHECK(estado<>'ACTIVA' OR iniciada_en IS NOT NULL), CHECK(estado<>'CERRADA' OR cerrada_en IS NOT NULL)
);
CREATE INDEX actividades_responsable_idx ON actividades(responsable_id);
CREATE INDEX sesiones_actividad_idx ON sesiones(actividad_id);
CREATE INDEX actividad_carreras_carrera_idx ON actividad_carreras(carrera_id,actividad_id);
