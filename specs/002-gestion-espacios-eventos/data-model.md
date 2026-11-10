# Modelo de datos: 002

Cambios sobre el esquema de 001 (`services/api/src/main/resources/db/migration`). Nuevas
migraciones: `V009__academico_y_espacios.sql`, `V010__actividades_horarios_metodo.sql`,
`V011__dispositivo_y_autorregistro.sql`; geográfica opcional `geo-migration/V002__centro_generado.sql`.

## Tablas existentes que cambian

### roles_cuenta
- `rol` admite además `ADMINISTRADOR`.

### espacios (V009)
| Campo | Tipo | Regla |
|---|---|---|
| codigo | varchar(40) | lo que escribió el usuario, no vacío |
| codigo_normalizado | varchar(40) UNIQUE | mayúsculas, sin espacios ni guiones |
| nombre | existente | no vacío |
| tipo | existente | `AULA`,`LABORATORIO`,`AUDITORIO`,`EXTERIOR`,`OTRO` |
| latitud / longitud | double precision | ambos o ninguno; ±90 / ±180 |
| radio_defecto_m | double precision | >0, por defecto 20 |
| precision_defecto_m | double precision | >0, por defecto 30 |
| creado_por | uuid → cuentas | obligatorio en filas nuevas |

Geo V002: `centro` pasa a `GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitud,latitud),4326)::geography) STORED`; se elimina `radio_recomendado_m`.

### actividades (V010)
| Campo | Regla |
|---|---|
| acceso | `CERRADO`/`ABIERTO`; obligatorio si `modalidad='EVENTO'`, NULL si `CLASE`; inmutable tras iniciar la primera sesión |
| metodo_verificacion | por defecto `SOLO_QR` |
| espacio_id | espacio por defecto de la actividad (nullable) |
| estado | añade `CANCELADA` (con `cerrada_en` y `anonimizar_en` como `CERRADA`, para conservar la retención de 001) |

### sesiones (V010)
| Campo | Regla |
|---|---|
| estado | añade `CANCELADA` |
| metodo_verificacion | copiado de la actividad al crear; editable solo en `PROGRAMADA` |
| horario_id | uuid → horarios_clase, NULL para sesiones sueltas |

Regla al iniciar: método `*_GPS` ⇒ `configuracion_geografica.habilitada` y espacio con coordenadas;
método sin GPS ⇒ configuración deshabilitada.

### configuracion_geografica
- Sin cambios de columnas. Al activar GPS, `radio_m` y `precision_max_m` se rellenan con los
  defectos del espacio si el responsable no envía valores; `antiguedad_max_s` por defecto 60 (001).

### intentos_asistencia (V011)
- `dispositivo_hash char(64)` (HMAC hex), NULL solo en filas anteriores a 002.
- `canal` admite `AUTORREGISTRO`.

### asistencias (V011)
- `dispositivo_hash char(64)`; `UNIQUE (sesion_id, dispositivo_hash) WHERE dispositivo_hash IS NOT NULL`.
- `forma` admite `AUTORREGISTRO`.

### inicios_invitado
- Sin cambios; en eventos abiertos el envío puede crear la persona externa antes de consumir.

## Tablas nuevas (V009, V010)

### matriculas
`persona_id → personas`, `paralelo_id → paralelos`, `vigente boolean`, `PK(persona_id, paralelo_id)`.
La persona debe tener cuenta con rol ESTUDIANTE (comprobado en aplicación).

### docentes_paralelo
`cuenta_id → cuentas`, `paralelo_id → paralelos`, `vigente boolean`, `PK(cuenta_id, paralelo_id)`.
La cuenta debe tener rol DOCENTE.

### horarios_clase
| Campo | Regla |
|---|---|
| id | uuid |
| actividad_id | → actividades, modalidad CLASE |
| dias | smallint[] con valores 1–7, sin repetidos, no vacío |
| hora_inicio / hora_fin | time; fin > inicio |
| fecha_desde / fecha_hasta | date; hasta ≥ desde |
| espacio_id | → espacios |
| zona | varchar, `America/Guayaquil` |

## Estados

```
Actividad: ABIERTA → CERRADA (001)    ABIERTA → CANCELADA (solo sin sesiones iniciadas)
Sesión:    PROGRAMADA → ACTIVA → CERRADA (001)    PROGRAMADA → CANCELADA
```

## Conservación (001 FR-022/FR-025)
La anonimización pone a NULL `dispositivo_hash` de intentos y asistencias y trata a las personas
`EXTERNA` autorregistradas igual que a los invitados de listado. `CANCELADA` cuenta como cierre
definitivo para el cálculo de los cinco años.
