ALTER TABLE roles_cuenta DROP CONSTRAINT roles_cuenta_rol_check;
ALTER TABLE roles_cuenta ADD CONSTRAINT roles_cuenta_rol_check
 CHECK(rol IN ('DOCENTE','ENCARGADO','ESTUDIANTE','DIRECTOR_INSTITUCIONAL','DIRECTOR_CARRERA','ADMINISTRADOR'));

CREATE TABLE matriculas (
 persona_id uuid NOT NULL REFERENCES personas(id), paralelo_id uuid NOT NULL REFERENCES paralelos(id),
 vigente boolean NOT NULL DEFAULT true, PRIMARY KEY(persona_id,paralelo_id)
);
CREATE TABLE docentes_paralelo (
 cuenta_id uuid NOT NULL REFERENCES cuentas(id), paralelo_id uuid NOT NULL REFERENCES paralelos(id),
 vigente boolean NOT NULL DEFAULT true, PRIMARY KEY(cuenta_id,paralelo_id)
);

ALTER TABLE espacios
 ADD COLUMN codigo varchar(40) CHECK(codigo IS NULL OR length(btrim(codigo))>0),
 ADD COLUMN codigo_normalizado varchar(40) UNIQUE,
 ADD COLUMN latitud double precision CHECK(latitud BETWEEN -90 AND 90),
 ADD COLUMN longitud double precision CHECK(longitud BETWEEN -180 AND 180),
 ADD COLUMN radio_defecto_m double precision NOT NULL DEFAULT 20 CHECK(radio_defecto_m>0),
 ADD COLUMN precision_defecto_m double precision NOT NULL DEFAULT 30 CHECK(precision_defecto_m>0),
 ADD COLUMN creado_por uuid REFERENCES cuentas(id),
 ADD CONSTRAINT espacios_coordenadas_ck CHECK((latitud IS NULL) = (longitud IS NULL));

-- Filas de 001: el código sale del nombre; los repetidos reciben un sufijo numérico para respetar UNIQUE.
UPDATE espacios SET tipo='OTRO' WHERE tipo NOT IN ('AULA','LABORATORIO','AUDITORIO','EXTERIOR','OTRO');
WITH n AS (
 SELECT id, left(btrim(nombre),40) AS cod,
        upper(regexp_replace(nombre,'[[:space:]-]','','g')) AS norm,
        row_number() OVER (PARTITION BY upper(regexp_replace(nombre,'[[:space:]-]','','g')) ORDER BY id) AS rn
   FROM espacios)
UPDATE espacios e SET codigo=n.cod,
       codigo_normalizado=CASE WHEN n.rn=1 THEN left(n.norm,40) ELSE left(n.norm,34)||'-'||n.rn END
  FROM n WHERE n.id=e.id AND n.norm<>'';
ALTER TABLE espacios ADD CONSTRAINT espacios_tipo_ck CHECK(tipo IN ('AULA','LABORATORIO','AUDITORIO','EXTERIOR','OTRO'));
