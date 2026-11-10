CREATE TABLE usuarios (
  id uuid PRIMARY KEY,
  correo varchar(254) NOT NULL UNIQUE CHECK (correo = lower(correo) AND correo ~ '^[^@\s]+@[^@\s]+$'),
  cedula char(10) NOT NULL UNIQUE,
  nombres varchar(200) NOT NULL CHECK (char_length(nombres) BETWEEN 1 AND 200),
  password_hash text NOT NULL,
  activo boolean NOT NULL DEFAULT true,
  debe_cambiar_password boolean NOT NULL DEFAULT true,
  creado_en timestamptz NOT NULL,
  actualizado_en timestamptz NOT NULL
);
CREATE TABLE usuario_roles (
  usuario_id uuid NOT NULL REFERENCES usuarios ON DELETE CASCADE,
  rol varchar(30) NOT NULL CHECK (rol IN ('ESTUDIANTE','DOCENTE','ENCARGADO','DIRECTOR_INSTITUCIONAL','DIRECTOR_CARRERA','ADMINISTRADOR')),
  PRIMARY KEY (usuario_id, rol)
);
CREATE TABLE usuario_carreras (
  usuario_id uuid NOT NULL REFERENCES usuarios ON DELETE CASCADE,
  carrera_codigo varchar(40) NOT NULL,
  PRIMARY KEY (usuario_id, carrera_codigo)
);
CREATE TABLE servicios (
  id uuid PRIMARY KEY,
  nombre varchar(100) NOT NULL UNIQUE,
  retornos text[] NOT NULL,
  clave_hash char(64) NOT NULL,
  activo boolean NOT NULL DEFAULT true
);
CREATE TABLE sesiones_sso (
  id uuid PRIMARY KEY,
  usuario_id uuid NOT NULL REFERENCES usuarios ON DELETE CASCADE,
  secreto_hash char(64) NOT NULL UNIQUE,
  creada_en timestamptz NOT NULL,
  ultimo_uso_en timestamptz NOT NULL,
  vence_absoluto_en timestamptz NOT NULL,
  cerrada_en timestamptz
);
CREATE TABLE tickets (
  id uuid PRIMARY KEY,
  hash char(64) NOT NULL UNIQUE,
  sesion_id uuid NOT NULL REFERENCES sesiones_sso ON DELETE CASCADE,
  servicio_id uuid NOT NULL REFERENCES servicios,
  service text NOT NULL,
  emitido_en timestamptz NOT NULL,
  vence_en timestamptz NOT NULL,
  usado_en timestamptz
);
CREATE INDEX tickets_pendientes ON tickets (vence_en) WHERE usado_en IS NULL;
CREATE TABLE registro_ingresos (
  id uuid PRIMARY KEY,
  correo varchar(254) NOT NULL,
  resultado varchar(20) NOT NULL CHECK (resultado IN ('OK','CREDENCIALES','INACTIVO','BLOQUEADO')),
  origen varchar(64) NOT NULL,
  servicio_id uuid REFERENCES servicios,
  en timestamptz NOT NULL
);
CREATE TABLE auditoria_admin (
  id uuid PRIMARY KEY,
  actor_id uuid NOT NULL,
  accion varchar(60) NOT NULL,
  objetivo_id uuid,
  anterior jsonb,
  nuevo jsonb,
  en timestamptz NOT NULL
);
CREATE TABLE contadores_limite (
  ambito char(64) NOT NULL,
  ventana timestamptz NOT NULL,
  conteo integer NOT NULL,
  PRIMARY KEY (ambito, ventana)
);
