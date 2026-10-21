ALTER TABLE configuracion_invitados ADD COLUMN acceso_generacion bigint NOT NULL DEFAULT 0;
CREATE TABLE inicios_invitado (
 id uuid PRIMARY KEY, sesion_id uuid NOT NULL REFERENCES sesiones(id) ON DELETE CASCADE,
 secreto_hash char(64) NOT NULL UNIQUE, inicio_en timestamptz NOT NULL, vence_en timestamptz NOT NULL,
 tolerancia_segundos integer NOT NULL CHECK(tolerancia_segundos>40), acceso_generacion bigint NOT NULL,
 estado varchar(15) NOT NULL DEFAULT 'ABIERTO' CHECK(estado IN ('ABIERTO','CONSUMIDO')),
 persona_vinculada_id uuid REFERENCES personas(id), persona_consumida_id uuid REFERENCES personas(id),
 recibo_asistencia_id uuid, consumido_en timestamptz,
 CHECK(vence_en=inicio_en+tolerancia_segundos*INTERVAL '1 second'),
 CHECK(estado<>'CONSUMIDO' OR (persona_vinculada_id IS NOT NULL AND persona_consumida_id=persona_vinculada_id AND recibo_asistencia_id IS NOT NULL AND consumido_en IS NOT NULL))
);
CREATE TABLE intentos_asistencia (
 id uuid PRIMARY KEY, secuencia bigint GENERATED ALWAYS AS IDENTITY UNIQUE,
 sesion_id uuid NOT NULL REFERENCES sesiones(id) ON DELETE CASCADE, persona_id uuid REFERENCES personas(id),
 nombres_referencia varchar(200), resultado varchar(25) NOT NULL CHECK(resultado IN ('ACEPTADO','RECHAZADO','REPETIR','PENDIENTE_REVISION')),
 code varchar(80) NOT NULL, mensaje text NOT NULL, canal varchar(30) NOT NULL,
 recibido_en timestamptz NOT NULL, distancia_m double precision CHECK(distancia_m>=0), precision_m double precision CHECK(precision_m>=0),
 inicio_invitado_id uuid REFERENCES inicios_invitado(id) ON DELETE SET NULL,
 CHECK(persona_id IS NOT NULL OR nombres_referencia IS NULL)
);
CREATE TABLE asistencias (
 id uuid PRIMARY KEY, sesion_id uuid NOT NULL REFERENCES sesiones(id) ON DELETE CASCADE,
 persona_id uuid NOT NULL REFERENCES personas(id), intento_id uuid NOT NULL UNIQUE REFERENCES intentos_asistencia(id),
 registrada_en timestamptz NOT NULL, forma varchar(30) NOT NULL CHECK(forma IN ('CREDENCIALES','INVITADO_LISTADO','REVISION')),
 nombres_registro varchar(200) NOT NULL CHECK(length(btrim(nombres_registro))>0), UNIQUE(sesion_id,persona_id)
);
ALTER TABLE inicios_invitado ADD FOREIGN KEY(recibo_asistencia_id) REFERENCES asistencias(id) ON DELETE CASCADE;
ALTER TABLE idempotencias ADD COLUMN actor_cuenta_id uuid REFERENCES cuentas(id) ON DELETE CASCADE;
ALTER TABLE idempotencias ADD COLUMN inicio_id uuid REFERENCES inicios_invitado(id) ON DELETE CASCADE;
UPDATE idempotencias SET actor_cuenta_id=actor::uuid WHERE actor ~ '^[0-9a-fA-F-]{36}$';
ALTER TABLE idempotencias ADD CHECK(num_nonnulls(actor_cuenta_id,inicio_id)=1);
CREATE INDEX intentos_sesion_persona_orden_idx ON intentos_asistencia(sesion_id,persona_id,secuencia DESC);
CREATE INDEX inicios_vence_idx ON inicios_invitado(vence_en);
