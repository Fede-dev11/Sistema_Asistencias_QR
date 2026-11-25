CREATE TABLE auditoria_academica (
 id uuid PRIMARY KEY, actor_id uuid NOT NULL REFERENCES cuentas(id), accion varchar(100) NOT NULL,
 entidad varchar(40) NOT NULL, entidad_id uuid, instante timestamptz NOT NULL, correlacion uuid NOT NULL,
 anterior jsonb NOT NULL DEFAULT '{}', nuevo jsonb NOT NULL DEFAULT '{}'
);
CREATE INDEX auditoria_academica_instante_idx ON auditoria_academica(instante,id);
