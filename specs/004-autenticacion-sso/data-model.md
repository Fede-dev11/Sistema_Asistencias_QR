# Modelo de datos: 004

## Servicio de autenticación (`services/auth`, base `geoasist_auth`, migraciones Flyway propias)

### usuarios
| Campo | Tipo | Regla |
|---|---|---|
| id | uuid PK | |
| correo | varchar(254) | único, minúsculas, formato correo |
| cedula | char(10) | única, módulo 10 (misma regla que `Cedula` de GeoAsist) |
| nombres | varchar(200) | 1–200 |
| password_hash | text | BCrypt; contraseña ≥ 12 y ≤ 128 caracteres |
| activo | boolean | por defecto true |
| debe_cambiar_password | boolean | por defecto true en altas y restablecimientos |
| creado_en / actualizado_en | timestamptz | |

### usuario_roles
`usuario_id → usuarios ON DELETE CASCADE`, `rol` CHECK IN (`ESTUDIANTE`,`DOCENTE`,`ENCARGADO`,`DIRECTOR_INSTITUCIONAL`,`DIRECTOR_CARRERA`,`ADMINISTRADOR`), PK (usuario_id, rol).

### usuario_carreras
`usuario_id`, `carrera_codigo varchar(40)`, PK (usuario_id, carrera_codigo). El código coincide con `carreras.codigo` de GeoAsist.

### servicios
| Campo | Regla |
|---|---|
| id uuid, nombre (único) | |
| retornos text[] | prefijos de URL de retorno permitidos; esquema `https`, `http` solo para `localhost`, o esquema propio de app (`geoasist://`) |
| clave_hash char(64) | hash de la clave para `X-Service-Key` (consulta de estado) |
| activo boolean | |

### sesiones_sso (TGT)
`id uuid`, `usuario_id`, `secreto_hash char(64) UNIQUE`, `creada_en`, `ultimo_uso_en`, `vence_absoluto_en` (+24 h), `cerrada_en`. Vigente si no cerrada, `ultimo_uso_en + 8 h > now()` y `vence_absoluto_en > now()`.

### tickets (ST)
`id uuid`, `hash char(64) UNIQUE`, `sesion_id → sesiones_sso`, `servicio_id → servicios`, `service text` (cadena exacta), `emitido_en`, `vence_en` (+10 s), `usado_en`. Índice parcial por `vence_en WHERE usado_en IS NULL` para limpieza.

### registro_ingresos
`id`, `correo` (tal como se escribió, truncado a 254), `resultado` CHECK IN (`OK`,`CREDENCIALES`,`INACTIVO`,`BLOQUEADO`), `origen` (IP), `servicio_id` nullable, `en`. Conservación 90 días (limpieza diaria).

### auditoria_admin
`id`, `actor_id`, `accion`, `objetivo_id`, `anterior jsonb`, `nuevo jsonb` (sin contraseñas), `en`.

### contadores_limite
Igual que en GeoAsist: `ambito char(64)`, `ventana`, `conteo`. Login: 5/min por correo, 50/min por IP.

### Estados
```
Usuario:   activo ⇄ inactivo        debe_cambiar_password: true → false (al cambiarla)
Sesión:    vigente → cerrada (logout, cambio/restablecimiento de contraseña, desactivación)
Ticket:    emitido → usado | vencido
```

## GeoAsist (`services/api`, migración `V019__cuentas_sso.sql`)

- `cuentas.proveedor` admite `CAS` (las cuentas `DEV` siguen para pruebas con inicio local).
- `cuentas.password_hash` ya es nullable; cuentas `CAS` siempre NULL (CHECK `proveedor<>'CAS' OR password_hash IS NULL`).
- `cuentas.sincronizada_en timestamptz` — último canje que actualizó roles.
- Sin tablas nuevas: roles en `roles_cuenta`, carreras de director en `direcciones_carrera`.
