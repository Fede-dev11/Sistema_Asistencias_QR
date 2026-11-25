# Implementation Plan: Faltas, estadísticas, asistencia manual, historial, ciclo automático y cuenta

**Branch**: `feature/003-ausencias-reportes-cuenta` (desde `feature/002-gestion-espacios-eventos` integrada) | **Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)

## Summary

Solo backend (más contrato y tipos generados). Se añaden ausencias capturadas en el cierre de
sesión, estadísticas por consulta SQL agregada, asistencia manual y justificaciones auditadas,
historial propio, un programador de ciclo de sesiones y la gestión de la cuenta propia con
renovación de sesión. Mismo stack, módulos y convenciones de 001/002; sin dependencias nuevas.

## Technical Context

**Language/Version**: Java 21 · **Dependencies**: Spring Boot 4.1.1 (MVC, Security, JDBC, `@Scheduled` ya usado por `QrRotationScheduler`) · **Storage**: PostgreSQL 17.7 · **Testing**: JUnit 6 + Testcontainers, `MutableClock` de pruebas · **Project Type**: API web · **Performance**: SC-003 < 3 s para estadísticas de carrera (índices justificados abajo) · **Constraints**: contrato único `specs/001-asistencia-qr-dinamico/contracts/openapi.yaml`.

## Constitution Check

| Principio | Tratamiento | Estado |
|---|---|---|
| I SDD | Spec con alcance del usuario; FR → tareas | Cumple |
| II Modularidad | Nuevo módulo `estadisticas` (lectura); el resto en módulos existentes | Cumple |
| III SOLID | Regla de ausencias en un solo caso de uso, invocado por todos los cierres | Cumple |
| IV API | Paginación, Problem Details, idempotencia en altas manuales | Cumple |
| V Identidad | Contraseñas BCrypt existente; secretos opacos rotados; auditoría admin | Cumple |
| VI Registro | Manual no evita reglas: autorizado, ventana de 7 días, auditado | Cumple |
| VIII Integridad | Ausencias en la transacción del cierre; `UNIQUE(sesion_id, persona_id)`; bloqueo de sesión existente; programador con `FOR UPDATE SKIP LOCKED` | Cumple |
| XI Privacidad | Motivos no exportados; anonimización a 5 años | Cumple; LOPDP pendiente |
| XII Pruebas | Unit (porcentajes), integración, concurrencia de cierre y de programador, autorización | Cumple |

Sin desviaciones.

## Decisiones (research)

- **R1 Ausencias como instantánea** en `ausencias` al cerrar (no derivadas): la lista autorizada cambia
  después y el historial no debe cambiar. Un único caso de uso `RecordAbsences` lo llaman
  `SessionLifecycle.close`, `CloseActivity` y el programador.
- **R2 Estadísticas por SQL agregado** sobre `asistencias` y `ausencias` (computables = asistencias +
  ausencias de sesiones CERRADAS); sin tablas materializadas hasta medir SC-003.
- **R3 Programador**: `@Scheduled(fixedDelay = 60 s)` con `SELECT ... FOR UPDATE SKIP LOCKED` sobre
  sesiones a transicionar, transición por sesión en su propia transacción; fallo → auditoría y
  `siguiente_intento_en` = ahora + 60 s. Auditoría con actor sistema (`actor_id` NULL y `accion`
  `SISTEMA_*`): requiere permitir NULL en `auditoria_actividad.actor_id`.
- **R4 Renovación de sesión**: `POST /auth/renew` con el secreto vigente → nuevo secreto 8 h, revoca el
  anterior, conserva `login_original_en`; rechaza si `ahora > login_original_en + 30 días`.
- **R5 Contraseña temporal**: 16 caracteres de `SecureRandom`, devuelta una vez; `cuentas.debe_cambiar_password`.
  Con esa marca, el filtro de seguridad solo permite `PUT /me/password`, `GET /me` y `/auth/logout`.

## Modelo de datos (migraciones V015–V018, numeración tras 002)

- `ausencias(sesion_id → sesiones ON DELETE CASCADE, persona_id → personas, estado varchar(12) CHECK IN ('AUSENTE','JUSTIFICADA'), registrada_en timestamptz NOT NULL, PK(sesion_id, persona_id))`; índice `(persona_id)`.
- `justificaciones(id, sesion_id, persona_id, FK (sesion_id,persona_id) → ausencias ON DELETE CASCADE, origen CHECK IN ('ESTUDIANTE','RESPONSABLE'), motivo varchar(2000) NOT NULL, estado CHECK IN ('PENDIENTE','APROBADA','RECHAZADA'), decidida_por → cuentas, razon_decision varchar(2000), solicitada_en, decidida_en)`; índice único parcial `(sesion_id, persona_id) WHERE estado='PENDIENTE'`; trigger de inmutabilidad tras decidir.
- `asistencias.forma` admite `MANUAL`; `asistencias.razon_manual varchar(2000)` obligatoria si MANUAL; `registrada_por uuid → cuentas`. La asistencia manual crea también un intento con canal `MANUAL` para mantener `intento_id NOT NULL`.
- `actividades.ciclo_automatico boolean NOT NULL DEFAULT false`, `actividades.umbral_asistencia numeric(5,2) NOT NULL DEFAULT 70 CHECK 0–100`.
- `sesiones.siguiente_intento_en timestamptz`; índice parcial `(ventana_inicio) WHERE estado='PROGRAMADA'` y `(ventana_fin) WHERE estado='ACTIVA'`.
- `auditoria_actividad.actor_id` pasa a NULL permitido (acciones `SISTEMA_*`).
- `cuentas.debe_cambiar_password boolean NOT NULL DEFAULT false`; `sesiones_acceso.login_original_en timestamptz` (relleno = `creada_en`).
- Conservación: `anonimizar_vencidas` borra ausencias y justificaciones de la actividad vencida y nulifica `razon_manual`.

## Contrato (añadir al YAML único)

| Ruta | Uso |
|---|---|
| `GET /activities/{id}/stats` | Por estudiante (paginado) + resumen de la actividad |
| `GET /stats/parallels?careerId&desde&hasta` y `GET /stats/careers?desde&hasta` | Directores y responsables según ámbito |
| `GET /sessions/{id}/absences` | Ausencias y su estado (responsable/directores) |
| `POST /sessions/{id}/manual-attendances` | `{persona_id, razon}` + `Idempotency-Key` |
| `POST /sessions/{id}/absences/{personId}/justify` | `{razon}` (responsable) |
| `POST /me/absences/{sessionId}/justification-requests` | `{motivo}` (estudiante) |
| `GET /activities/{id}/justification-requests?estado=` | Bandeja del responsable |
| `POST /justification-requests/{id}/decision` | `{decision: APROBAR|RECHAZAR, razon}` |
| `GET /me`, `GET /me/attendances`, `GET /me/stats` | Perfil, historial paginado, porcentajes propios |
| `PATCH /activities/{id}` | Añade `ciclo_automatico`, `umbral_asistencia` |
| `PUT /me/password` | `{actual, nueva}` |
| `POST /admin/accounts/{id}/password-reset` | → `{password_temporal}` una vez |
| `POST /auth/renew` | Rota el secreto |

Códigos nuevos: `PLAZO_CORRECCION_VENCIDO`, `NO_AUTORIZADO_EN_SESION`, `JUSTIFICACION_PENDIENTE_EXISTE`, `SIN_AUSENCIA`, `PASSWORD_ACTUAL_INCORRECTA`, `PASSWORD_DEBIL`, `CAMBIO_PASSWORD_REQUERIDO`, `RENOVACION_VENCIDA`.

## Project Structure

```text
services/api/src/main/java/ec/edu/unl/geoasist/
├── asistencia/      # RecordAbsences, ManualAttendance, Justifications
├── estadisticas/    # NUEVO: AttendanceStats (lectura), controladores de stats e historial propio
├── sesiones/        # SessionCycleScheduler; cierre invoca RecordAbsences
├── identidades/     # AccountSelfService (perfil, contraseña), renovación, filtro cambio obligatorio
└── conservacion/    # extensión de anonimización
```
