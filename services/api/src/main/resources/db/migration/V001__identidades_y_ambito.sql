CREATE TABLE personas (
 id uuid PRIMARY KEY, cedula_normalizada varchar(10) NOT NULL UNIQUE CHECK (cedula_normalizada ~ '^[0-9]{10}$'),
 nombres varchar(200) NOT NULL CHECK (length(btrim(nombres)) > 0), tipo varchar(20) NOT NULL CHECK(tipo IN ('INSTITUCIONAL','EXTERNA'))
);
CREATE TABLE cuentas (
 id uuid PRIMARY KEY, persona_id uuid NOT NULL UNIQUE REFERENCES personas(id), proveedor varchar(50) NOT NULL,
 identificador varchar(254) NOT NULL UNIQUE, password_hash text, activa boolean NOT NULL DEFAULT true
);
CREATE TABLE roles_cuenta (
 cuenta_id uuid NOT NULL REFERENCES cuentas(id), rol varchar(30) NOT NULL CHECK(rol IN ('DOCENTE','ENCARGADO','ESTUDIANTE','DIRECTOR_INSTITUCIONAL','DIRECTOR_CARRERA')),
 PRIMARY KEY(cuenta_id,rol)
);
CREATE TABLE carreras (id uuid PRIMARY KEY, codigo varchar(40) NOT NULL UNIQUE, nombre varchar(200) NOT NULL);
CREATE TABLE paralelos (id uuid PRIMARY KEY, carrera_id uuid NOT NULL REFERENCES carreras(id), codigo varchar(40) NOT NULL, UNIQUE(id,carrera_id), UNIQUE(carrera_id,codigo));
CREATE TABLE direcciones_carrera (cuenta_id uuid NOT NULL REFERENCES cuentas(id), carrera_id uuid NOT NULL REFERENCES carreras(id), vigente boolean NOT NULL DEFAULT true, PRIMARY KEY(cuenta_id,carrera_id));
