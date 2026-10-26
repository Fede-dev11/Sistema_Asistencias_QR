# Contrato API: cambios de 002

**Fuente de verdad única**: [`specs/001-asistencia-qr-dinamico/contracts/openapi.yaml`](../../001-asistencia-qr-dinamico/contracts/openapi.yaml),
la que leen `tools/contracts` y los tipos generados de web y móvil (Principio IV: un contrato
para ambos clientes). 002 añade operaciones a ese archivo; este documento las resume.
Prefijo `/api/v1`, errores Problem Details, colecciones paginadas (`page`, `size` ≤ 100) e
`Idempotency-Key` en POST que crean registros, como en 001.

## Administración (rol ADMINISTRADOR)

| Método y ruta | Uso | Códigos |
|---|---|---|
| `GET/POST /admin/people` | Listar (filtro `q` cédula/nombre) y crear persona | 200, 201, 400, 409 cédula existe |
| `GET/PATCH /admin/people/{personId}` | Ver y editar nombres | 200, 404 |
| `POST /admin/accounts` | Crear cuenta (persona, correo, contraseña, roles) | 201, 409 correo o persona con cuenta |
| `PATCH /admin/accounts/{accountId}` | Roles, activar/desactivar, cambiar contraseña | 200 |
| `GET/POST /admin/careers` y `PATCH /admin/careers/{id}` | Carreras | |
| `GET/POST /admin/careers/{careerId}/parallels` | Paralelos de una carrera | |
| `PUT /admin/parallels/{parallelId}/enrollments` | Reemplazar matriculados (lista de cédulas) | 200 con filas rechazadas |
| `PUT /admin/parallels/{parallelId}/teachers` | Reemplazar docentes asignados | 200 |
| `POST /admin/imports/{kind}` | `kind` = `students`, `teachers`, `enrollments`; `multipart/form-data` campo `file` | 200 `ImportReport`, 400 formato, 413 tamaño |

`ImportReport`: `{ procesadas, creadas, actualizadas, sin_cambios, rechazadas: [{ fila, motivo }] }`.

Columnas CSV: students `cedula,nombres,correo`; teachers `cedula,nombres,correo,rol` (`DOCENTE`/`ENCARGADO`);
enrollments `cedula,carrera,paralelo`. La contraseña inicial de cuentas importadas viene de la
configuración de desarrollo, nunca del CSV.

## Catálogo del responsable

| Método y ruta | Uso |
|---|---|
| `GET /me/parallels` | Paralelos asignados al docente con número de matriculados |
| `GET /spaces?q=` | Buscar espacios por código o nombre (paginado; cualquier cuenta de responsable) |
| `POST /spaces` | Crear espacio (DOCENTE/ENCARGADO/ADMINISTRADOR) → 201, 409 `ESPACIO_CODIGO_EXISTE` con `espacio_id` existente |
| `PATCH /spaces/{spaceId}` | Editar (creador o ADMINISTRADOR) → 403 en otro caso |

`Space`: `{ id, codigo, nombre, tipo, latitud?, longitud?, radio_defecto_m, precision_defecto_m, editable }`.

## Actividades y sesiones

| Método y ruta | Uso |
|---|---|
| `POST /activities` | Crear clase o evento (ver cuerpo) → 201 con sesiones generadas |
| `PATCH /activities/{activityId}` | Nombre, espacio por defecto, método por defecto; `acceso` solo si ninguna sesión se inició |
| `POST /activities/{activityId}/cancel` | Cancelar (sin sesiones iniciadas) |
| `PUT /activities/{activityId}/schedule` | Reemplazar horario y regenerar sesiones programadas futuras sin intentos |
| `GET /activities/{activityId}/students` | Lista autorizada (paginada) |
| `POST /activities/{activityId}/students` | Añadir estudiante por cédula |
| `DELETE /activities/{activityId}/students/{personId}` | Quitar (vigente=false) |
| `POST /activities/{activityId}/students/sync` | Sincronizar con paralelos |
| `GET /activities/{activityId}/sessions` | Sesiones de la actividad (paginado) |
| `POST /activities/{activityId}/sessions` | Sesión suelta `{ inicio, fin, espacio_id? }` |
| `PATCH /sessions/{sessionId}` | Hora, espacio, `metodo_verificacion` (solo PROGRAMADA) |
| `POST /sessions/{sessionId}/cancel` | Cancelar (solo PROGRAMADA) |
| `PUT /sessions/{sessionId}/geolocation` | Existente; `radio_m`/`precision_max_m` pasan a opcionales y heredan del espacio |

`CreateActivityRequest`:
```
{ modalidad: CLASE|EVENTO, nombre, espacio_id, metodo_verificacion?,
  // CLASE
  paralelo_ids: [uuid]  (≥1, asignados al docente),
  horario?: { dias:[1..7], hora_inicio:"08:00", hora_fin:"10:00", desde:"2026-11-02", hasta:"2026-11-27" },
  // EVENTO
  acceso: CERRADO|ABIERTO, inicio, fin, tolerancia_invitado_segundos? (>40, defecto 120) }
```

## Participante

| Método y ruta | Uso |
|---|---|
| `GET /me/active-sessions` | Sesiones activas donde puede registrar: `{ sesion_id, actividad, espacio, metodo_verificacion, requiere_ubicacion }` |
| `POST /attendance/attempts` | Existente. Cambios: `dispositivo_id` (UUID, obligatorio); `qr_token` opcional; `sesion_id` obligatorio si el método no usa QR |
| `GET /events/open/{sessionId}` | Datos públicos mínimos de una sesión de evento abierto activa (nombre, espacio, método) |
| `POST /guest-registrations` | Existente. Acepta `qr_token` **o** `sesion_id` (solo métodos sin QR) |
| `POST /guest-registrations/submit` | Existente. Añade `dispositivo_id` (obligatorio) y `nombres` (obligatorio en evento abierto, prohibido en cerrado) |

## Códigos de motivo nuevos

| code | HTTP | Mensaje al usuario |
|---|---|---|
| `DISPOSITIVO_REQUERIDO` | 400 | Registra tu asistencia desde la app GeoAsist. |
| `DISPOSITIVO_YA_REGISTRADO` | 409 (y resultado `RECHAZADO`) | Este dispositivo ya registró una asistencia en esta sesión. |
| `CEDULA_INVALIDA` | 400 | La cédula no es válida. |
| `ESPACIO_CODIGO_EXISTE` | 409 | Ya existe un espacio con ese código. |
| `ESPACIO_SIN_UBICACION` | 409 | El espacio no tiene ubicación; elige un método sin GPS. |
| `METODO_NO_ADMITE_QR` / `QR_REQUERIDO` | 400 | — |
| `PARALELO_NO_ASIGNADO` | 403 | No tienes asignado ese paralelo. |
| `SESION_NO_EDITABLE` | 409 | La sesión ya inició o fue cancelada. |
| `HORARIO_DEMASIADO_LARGO` | 400 | El horario genera más de 200 sesiones. |
