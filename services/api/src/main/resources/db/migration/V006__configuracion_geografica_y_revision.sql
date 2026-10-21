CREATE TABLE evidencias_geograficas (
 id uuid PRIMARY KEY,
 espacio_id uuid NOT NULL REFERENCES espacios(id),
 version integer NOT NULL CHECK(version>0),
 referencia varchar(500) NOT NULL CHECK(length(btrim(referencia))>0),
 realizada_en timestamptz NOT NULL,
 responsable_id uuid NOT NULL REFERENCES cuentas(id),
 falsos_positivos numeric(7,6) NOT NULL CHECK(falsos_positivos BETWEEN 0 AND 1),
 falsos_negativos numeric(7,6) NOT NULL CHECK(falsos_negativos BETWEEN 0 AND 1),
 verificada_en timestamptz NOT NULL,
 UNIQUE(espacio_id,version)
);

CREATE TABLE configuracion_geografica (
 sesion_id uuid PRIMARY KEY REFERENCES sesiones(id) ON DELETE CASCADE,
 habilitada boolean NOT NULL DEFAULT false,
 radio_m double precision,
 precision_max_m double precision,
 antiguedad_max_s integer,
 modo varchar(20) NOT NULL DEFAULT 'OFF' CHECK(modo IN ('OFF','REVIEW_ONLY','AUTO')),
 evidencia_id uuid REFERENCES evidencias_geograficas(id),
 configurada_por uuid NOT NULL REFERENCES cuentas(id),
 configurada_en timestamptz NOT NULL,
 CHECK(
   (NOT habilitada AND radio_m IS NULL AND precision_max_m IS NULL AND antiguedad_max_s IS NULL AND modo='OFF' AND evidencia_id IS NULL)
   OR
   (habilitada AND radio_m>0 AND precision_max_m>0 AND antiguedad_max_s>=1 AND modo IN ('REVIEW_ONLY','AUTO') AND (modo<>'AUTO' OR evidencia_id IS NOT NULL))
 )
);

CREATE TABLE revisiones_asistencia (
 intento_id uuid PRIMARY KEY REFERENCES intentos_asistencia(id) ON DELETE CASCADE,
 revisor_id uuid NOT NULL REFERENCES cuentas(id),
 decision varchar(10) NOT NULL CHECK(decision IN ('APROBAR','RECHAZAR')),
 razon varchar(2000) NOT NULL CHECK(length(btrim(razon)) BETWEEN 1 AND 2000),
 revisada_en timestamptz NOT NULL,
 asistencia_id uuid REFERENCES asistencias(id),
 CHECK((decision='APROBAR' AND asistencia_id IS NOT NULL) OR (decision='RECHAZAR' AND asistencia_id IS NULL))
);

CREATE FUNCTION proteger_revision_asistencia() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'Decisión de revisión inmutable';
END $$;
CREATE TRIGGER revision_inmutable BEFORE UPDATE OR DELETE ON revisiones_asistencia FOR EACH ROW EXECUTE FUNCTION proteger_revision_asistencia();

CREATE INDEX configuracion_geografica_evidencia_idx ON configuracion_geografica(evidencia_id) WHERE evidencia_id IS NOT NULL;
CREATE INDEX revisiones_revisor_fecha_idx ON revisiones_asistencia(revisor_id,revisada_en,intento_id);
