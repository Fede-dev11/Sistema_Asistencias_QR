---
description: "Tareas de 003: faltas, estadísticas, asistencia manual, historial, ciclo automático y cuenta"
---

# Tasks: 003 Faltas, estadísticas, asistencia manual, historial, ciclo automático y cuenta

**Input**: [spec.md](spec.md), [plan.md](plan.md) (incluye decisiones, modelo y contrato).
**Tests**: obligatorios (Principio XII). Convenciones `API`, `TEST`, `MIG`, `YAML` como en
[002 tasks](../002-gestion-espacios-eventos/tasks.md). Migraciones desde `V015` (002 usa hasta V014).
Reutilizar `ActivityAuthorization`, `ActivityAuditWriter`, `AttendanceLocks`, `IdempotencyService`,
`MutableClock`, `PostgresFixture`, ámbitos de consulta de `reportes`.

## Phase 1: Setup

- [ ] T001 Crear la rama `feature/003-ausencias-reportes-cuenta` desde `feature/002-gestion-espacios-eventos` y apuntar `.specify/feature.json` a `specs/003-ausencias-reportes-cuenta`
- [X] T002 [P] Crear `API/estadisticas/{presentacion,aplicacion,persistencia}/package-info.java` y registrar en `TEST/soporte/ArchitectureDependencyTest.java` que `estadisticas` solo lee (no depende de `presentacion` de otros módulos)

## Phase 2: Foundational

- [X] T003 `MIG/V015__ausencias_y_justificaciones.sql`: tablas `ausencias` y `justificaciones` exactamente como en plan.md (CHECKs, PK, índice único parcial `WHERE estado='PENDIENTE'`, trigger que impide modificar una justificación con `estado<>'PENDIENTE'`); `asistencias.forma` admite `MANUAL`, columnas `razon_manual varchar(2000)` (CHECK obligatoria si `forma='MANUAL'`) y `registrada_por uuid REFERENCES cuentas(id)`; `intentos_asistencia.canal` admite `MANUAL`
- [X] T004 `MIG/V016__ciclo_y_umbral.sql`: `actividades.ciclo_automatico boolean NOT NULL DEFAULT false`, `actividades.umbral_asistencia numeric(5,2) NOT NULL DEFAULT 70 CHECK(umbral_asistencia BETWEEN 0 AND 100)`, `sesiones.siguiente_intento_en timestamptz`, índices parciales `sesiones(ventana_inicio) WHERE estado='PROGRAMADA'` y `sesiones(ventana_fin) WHERE estado='ACTIVA'`, `auditoria_actividad.actor_id DROP NOT NULL`
- [X] T005 `MIG/V017__cuenta_y_renovacion.sql`: `cuentas.debe_cambiar_password boolean NOT NULL DEFAULT false`, `sesiones_acceso.login_original_en timestamptz` (rellenar con `creada_en`, luego NOT NULL)
- [X] T006 `MIG/V018__conservacion_003.sql`: redefinir la función de anonimización (última versión en `V013__espacios_compartidos.sql`) para borrar `ausencias`/`justificaciones` de la actividad vencida y poner `razon_manual` a NULL; caso nuevo en `TEST/conservacion/RetentionLifecycleTest.java`
- [X] T007 Añadir al YAML los códigos de motivo nuevos de plan.md y esquemas comunes `AttendanceStats`, `AbsenceState`; `npm run validate` en `tools/contracts`

## Phase 3: US1 — Faltas al cerrar y estadísticas (P1) 🎯 MVP

**Independent Test**: 3 estudiantes × 4 sesiones cerradas → ausencias, porcentajes y `bajo_umbral` exactos.

- [X] T008 [P] [US1] `TEST/asistencia/AbsenceRecordingTest.java`: escenarios US1-1, US1-2, US1-3, US1-7; cierre manual, cierre de actividad y cierre concurrente (dos cierres a la vez → ausencias una sola vez, SC-001)
- [X] T009 [P] [US1] `TEST/estadisticas/AttendanceStatsTest.java`: caso 3 × 10 con resultados esperados fijos (SC-002), US1-4, US1-5, "sin datos" sin sesiones cerradas, ámbitos US1-6 (docente propio, director de carrera, institucional; 403 fuera de ámbito, SC-007)
- [X] T010 [US1] `API/asistencia/aplicacion/RecordAbsences.java`: `record(sessionId)` inserta en `ausencias` los `estudiantes_autorizados` vigentes de actividades CLASE sin fila en `asistencias` (`INSERT ... SELECT ... ON CONFLICT DO NOTHING`), dentro de la transacción del llamador
- [X] T011 [US1] Invocar `RecordAbsences` desde `API/sesiones/aplicacion/SessionLifecycle.java` (cierre) y `API/actividades/aplicacion/CloseActivity.java` (cierre de sesiones abiertas); eliminar la ausencia al aprobar en `API/asistencia/aplicacion/ResolveAttendanceReview.java` (US1-2)
- [X] T012 [US1] `API/estadisticas/persistencia/StatsRepository.java` (consultas agregadas por estudiante/actividad, paralelo y carrera con rango de fechas sobre `ventana_inicio`) y `API/estadisticas/aplicacion/AttendanceStats.java` (porcentaje con 2 decimales, `bajo_umbral` con `umbral_asistencia`, "sin datos" = null)
- [X] T013 [US1] `API/estadisticas/presentacion/StatsController.java`: `GET /activities/{id}/stats`, `GET /stats/parallels`, `GET /stats/careers`, `GET /sessions/{id}/absences`, aplicando el ámbito de 001 (reutilizar la autorización de `reportes`); YAML + `npm run generate`

## Phase 4: US2 — Asistencia manual y justificaciones (P1)

**Independent Test**: manual con razón, justificación por responsable, solicitud de estudiante rechazada; auditoría completa.

- [X] T014 [P] [US2] `TEST/asistencia/ManualAttendanceTest.java`: US2-1, US2-2, US2-3 (8 días tras cierre con `MutableClock` → `PLAZO_CORRECCION_VENCIDO`), US2-7, US2-8 (listado y CSV con forma MANUAL), pendiente de revisión resuelto como rechazado "registro manual" (edge case), idempotencia
- [X] T015 [P] [US2] `TEST/asistencia/JustificationTest.java`: US2-4, US2-5 (una pendiente por ausencia → `JUSTIFICACION_PENDIENTE_EXISTE`; sin ausencia → `SIN_AUSENCIA`), US2-6 (decisión inmutable), US2-7, auditoría con razón (SC-004), motivo no presente en CSV
- [X] T016 [US2] `API/asistencia/aplicacion/ManualAttendance.java`: responsable, estudiante autorizado vigente (`NO_AUTORIZADO_EN_SESION`), sesión ACTIVA o CERRADA con `cerrada_en` ≥ ahora − 7 días (propiedad `geoasist.correccion.dias`, defecto 7), bajo `AttendanceLocks.session`; crea intento canal MANUAL + asistencia forma MANUAL con `razon_manual` y `registrada_por`; borra ausencia; resuelve pendiente de revisión; audita
- [X] T017 [US2] `API/asistencia/aplicacion/Justifications.java`: justificar por responsable (crea justificación origen RESPONSABLE APROBADA y pasa ausencia a JUSTIFICADA), solicitud del estudiante (propia ausencia, mismo plazo), decisión (APROBAR → JUSTIFICADA); todo auditado
- [X] T018 [US2] Controladores en `API/asistencia/presentacion/`: rutas de manual, justify, solicitudes y decisión de plan.md; incluir forma/razón manual en `API/reportes/persistencia/AttendanceReadRepository.java` y `ExportAttendancesCsv.java` (sin motivo de justificación); YAML + tipos

## Phase 5: US3 — Historial del participante (P2)

- [ ] T019 [P] [US3] `TEST/estadisticas/MyHistoryTest.java`: US3-1, US3-2 (no ve datos ajenos), US3-3 (actividad anonimizada desaparece), paginación
- [ ] T020 [US3] `GET /me/attendances` (por sesión cerrada: actividad, fecha, resultado ASISTIO/AUSENTE/JUSTIFICADA/PENDIENTE_REVISION, estado de su solicitud) y `GET /me/stats` (por clase) en `API/estadisticas/presentacion/MyHistoryController.java` reutilizando `StatsRepository`; YAML + tipos

## Phase 6: US4 — Ciclo automático (P2)

- [ ] T021 [P] [US4] `TEST/sesiones/SessionCycleTest.java` con `MutableClock`: US4-1 a US4-4 y US4-6; ausencias generadas al cierre automático; `PATCH /activities/{id}` con `ciclo_automatico` y `umbral_asistencia` (validación 0–100)
- [ ] T022 [P] [US4] `TEST/sesiones/SessionCycleConcurrencyTest.java`: dos ejecuciones simultáneas de `tick()` sobre 20 sesiones → cada sesión transiciona una vez (SC-005)
- [ ] T023 [US4] `API/sesiones/aplicacion/SessionCycle.java` (`tick()`: selecciona con `FOR UPDATE SKIP LOCKED` sesiones PROGRAMADA de actividades con ciclo automático y `ventana_inicio <= ahora` y `(siguiente_intento_en IS NULL OR <= ahora)`; ACTIVA con ciclo y `ventana_fin <= ahora`; ACTIVA cualquiera con `ventana_fin + 2 h <= ahora`; cada transición en su transacción reutilizando `SessionLifecycle` en modo sistema; fallo → auditoría `SISTEMA_INICIO_FALLIDO` + `siguiente_intento_en = ahora + 60 s`) y `SessionCycleScheduler.java` (`@Scheduled(fixedDelay = 60000)`, desactivable con propiedad como `QrRotationScheduler`)
- [ ] T024 [US4] Permitir actor sistema en `API/actividades/aplicacion/ActivityAuditWriter.java` (`actor_id` NULL, acción `SISTEMA_*`) y mostrarlo en `AuditQueryController`; ampliar `PATCH /activities/{id}` (de 002 US3) con `ciclo_automatico` y `umbral_asistencia`; YAML + tipos

## Phase 7: US5 — Cuenta propia y sesión prolongada (P2)

- [X] T025 [P] [US5] `TEST/identidades/AccountSelfServiceTest.java`: US5-1 a US5-4 y US5-7 (sin secretos en `GET /me`), contraseña débil → `PASSWORD_DEBIL`, actual incorrecta → `PASSWORD_ACTUAL_INCORRECTA` y cuenta en límite de login
- [X] T026 [P] [US5] `TEST/identidades/SessionRenewalTest.java` con `MutableClock`: US5-5, US5-6, secreto anterior inválido tras renovar, renovación a 29 días OK y a 30 días + 1 min → `RENOVACION_VENCIDA` (SC-006)
- [X] T027 [US5] `API/identidades/aplicacion/AccountSelfService.java`: perfil, cambio de contraseña (BCrypt existente, ≥12, distinta, revoca otras sesiones), restablecimiento por administrador (temporal de 16 caracteres con `SecureRandom`, `debe_cambiar_password=true`, revoca sesiones, auditoría en `auditoria_academica` de 002)
- [X] T028 [US5] Renovación en `API/identidades/aplicacion/AuthenticationService.java` (`renew(token)`: rota secreto, 8 h, `login_original_en` + 30 días máximo; `login` fija `login_original_en`) y `POST /auth/renew` en `AuthController.java`
- [X] T029 [US5] Filtro en `API/identidades/presentacion/ApiSecurityConfig.java`: cuentas con `debe_cambiar_password` solo acceden a `PUT /me/password`, `GET /me`, `/auth/logout`; resto → 403 `CAMBIO_PASSWORD_REQUERIDO`; incluir la marca en la respuesta de login
- [X] T030 [US5] `GET /me`, `PUT /me/password` en `API/identidades/presentacion/AccountController.java` y `POST /admin/accounts/{id}/password-reset` en `API/academico/presentacion/AdminController.java`; YAML + tipos

## Phase 8: Polish

- [ ] T031 [P] Medir SC-003 con 1 000 estudiantes de prueba en `TEST/estadisticas/StatsPerformanceTest.java` (etiqueta `performance`, excluida de `verify` por defecto) y anotar el resultado real en `docs/validation/revision-incremento-003.md`
- [ ] T032 [P] Actualizar `infra/README.md` (propiedades `geoasist.correccion.dias`, programador de ciclo) y `README.md`
- [ ] T033 Ejecutar `./mvnw -B verify`, `npm run validate && npm run check` en `tools/contracts`, y lint/typecheck/test de `apps/web` y `apps/mobile` (los tipos generados cambian); registrar resultados reales en `docs/validation/revision-incremento-003.md`

## Dependencies

- Setup → Foundational → historias. US1 primero (US2 y US3 usan `ausencias`).
- US2 depende de US1 (ausencias). US3 depende de US1 (`StatsRepository`). US4 depende de US1 (`RecordAbsences`) y de 002 US3 (`PATCH /activities`). US5 es independiente.
- Las tareas de YAML tocan el mismo archivo: no en paralelo entre agentes sin mezclar después.

## Parallel Example

```text
Tras US1: línea A = US2 → US3 ; línea B = US4 ; línea C = US5
```

## Requirement Coverage

| FR | Tareas |
|---|---|
| FR-001, FR-002 | T003, T008, T010, T011 |
| FR-003, FR-004 | T009, T012, T013 |
| FR-005–FR-008 | T003, T014–T018 |
| FR-009 | T019, T020 |
| FR-010, FR-011 | T004, T021–T024 |
| FR-012–FR-015 | T005, T025–T030 |
| FR-016 | T006 |
| SC-001 T008 · SC-002 T009 · SC-003 T031 · SC-004 T015 · SC-005 T022 · SC-006 T026 · SC-007 T009, T019 | |
