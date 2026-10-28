ALTER TABLE cuentas ADD COLUMN sincronizada_en timestamptz;
ALTER TABLE cuentas ADD CONSTRAINT cuentas_cas_sin_password_ck
  CHECK (proveedor <> 'CAS' OR password_hash IS NULL);
