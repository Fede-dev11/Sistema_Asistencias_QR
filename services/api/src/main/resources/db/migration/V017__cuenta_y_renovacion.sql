ALTER TABLE cuentas ADD COLUMN debe_cambiar_password boolean NOT NULL DEFAULT false;
ALTER TABLE sesiones_acceso ADD COLUMN login_original_en timestamptz;
UPDATE sesiones_acceso SET login_original_en = creada_en;
ALTER TABLE sesiones_acceso ALTER COLUMN login_original_en SET NOT NULL;
