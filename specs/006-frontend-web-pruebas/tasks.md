---
description: "Tareas de 006: vistas web básicas para probar todas las funcionalidades"
---

# Tasks: Vistas web básicas para probar todas las funcionalidades

**Input**: [spec.md](spec.md), [plan.md](plan.md). **Tests**: obligatorios (Principio XII): una prueba
de componente por panel nuevo (Vitest + Testing Library, API simulada) y E2E por rol.
`WEB` = `apps/web/src`. Leer `apps/web/AGENTS.md` antes de escribir código Next.js.

## Phase 1: Setup

- [X] T001 Confirmar rama `feature/006-frontend-web-pruebas` y `.specify/feature.json` → `specs/006-frontend-web-pruebas`; `.env.example` con `NEXT_PUBLIC_HERRAMIENTAS_PRUEBA=false`

## Phase 2: Foundational

- [X] T002 [P] `WEB/features/common/ProblemMessage.tsx`: muestra `code` y mensaje de `ApiError` (rol `alert`), con prueba
- [X] T003 [P] `WEB/features/common/Paginated.tsx`: lista paginada genérica (Anterior/Siguiente, total, carga y vacío), con prueba
- [X] T004 [P] `WEB/features/common/RoleGuard.tsx`: muestra hijos si la sesión tiene alguno de los roles; si no, "No tienes acceso a esta sección", con prueba

## Phase 3: US1 — Navegación por rol y sesión (P1) 🎯 MVP

- [X] T005 [P] [US1] Pruebas `WEB/features/auth/RoleNav.test.tsx` y renovación en `SessionProvider.test.tsx` (renueva antes de vencer; 401/403/502 → cierra sesión con mensaje)
- [X] T006 [US1] `WEB/features/auth/RoleNav.tsx` en `WEB/app/layout.tsx`: nombre, roles y enlaces por rol (docente/encargado: Mis actividades, Sesiones, Estadísticas; director: Estadísticas, Reportes; administrador: Personas, Carreras y paralelos, Espacios, Importar; con bandera: Simulador); salir con opción de cerrar la sesión universitaria
- [X] T007 [US1] `SessionProvider`: cargar `GET /me` tras ingresar, renovar con `POST /auth/renew` 10 min antes de `expira_en`, manejar rechazo; envolver páginas existentes con `RoleGuard`

## Phase 4: US2 — Docente opera una sesión de principio a fin (P1)

- [ ] T008 [P] [US2] Pruebas de componente: `SessionPicker`, `SessionPanel` (iniciar/QR/cerrar, asistencias en vivo cada 5 s), `ManualAttendanceForm`, `AbsencesPanel` (justificar), `JustificationInbox` (aprobar/rechazar), `SchedulePanel` (ver/reemplazar con vista previa), `GuestListView`
- [ ] T009 [US2] `WEB/features/sessions/SessionPicker.tsx` (actividad → `GET /activities/{id}/sessions` paginado con fecha, estado, método) y rehacer `WEB/app/sesiones/page.tsx` sobre él, reutilizando `ResponsibleSessionPanel`, `GeolocationSettingsPanel` (cargar `GET /sessions/{id}/geolocation` antes de editar) y `ReviewPanel`; quitar el campo "Código de sesión"
- [ ] T010 [US2] `WEB/features/sessions/LiveAttendances.tsx` (`GET /sessions/{id}/attendances`, refresco cada 5 s mientras ACTIVA, filtro por estado, enlace al CSV)
- [ ] T011 [US2] `WEB/features/corrections/{ManualAttendanceForm,AbsencesPanel,JustificationInbox}.tsx` (`POST /sessions/{id}/manual-attendances` con `Idempotency-Key`; `GET /sessions/{id}/absences` + justify; `GET /activities/{id}/justification-requests?estado=` + decision)
- [ ] T012 [US2] `WEB/app/actividades/[id]/page.tsx` (ficha): estudiantes + sincronizar (`POST …/students/sync`), `SchedulePanel` (`GET/PUT …/schedule`, reutilizar `schedulePreview.ts`), `GuestListView` (`GET …/guest-list` y `…/guest-settings` antes de editar, reutilizar `GuestSettingsPanel`), bandeja de justificaciones y `ActivityAuditPanel`; enlazar la ficha desde `ClassesPanel`

## Phase 5: US3 — Estadísticas (P2)

- [ ] T013 [P] [US3] Prueba `WEB/features/stats/StatsPage.test.tsx` (tabla por estudiante con "bajo umbral" resaltado; "sin datos"; filtros de periodo/carrera)
- [ ] T014 [US3] `WEB/app/estadisticas/page.tsx` + `WEB/features/stats/{ActivityStats,ParallelStats,CareerStats}.tsx` con `RoleGuard` (docente/encargado/directores); periodo con dos fechas y carrera desde `GET /admin/careers` si es administrador o desde `GET /me` (carreras del director)

## Phase 6: US4 — Paralelos y evidencias de GPS (P2)

- [ ] T015 [P] [US4] Pruebas `WEB/features/admin/ParallelMembers.test.tsx` y `WEB/features/spaces/GeoEvidencePanel.test.tsx`
- [ ] T016 [US4] `WEB/features/admin/ParallelMembers.tsx` en la pantalla de carreras: ver (`GET …/enrollments`, `…/teachers`) y reemplazar (`PUT`, lista de cédulas o correos) con informe de rechazados
- [ ] T017 [US4] `WEB/app/admin/espacios/page.tsx` + `WEB/features/spaces/GeoEvidencePanel.tsx`: lista de espacios (`GET /spaces`), historial y alta de evidencia (`GET/POST /spaces/{id}/geo-evidence`); mostrar el historial (solo lectura) en `SpacePicker` para docentes

## Phase 7: US5 — Simulador de participante e invitado (P1)

- [ ] T018 [P] [US5] Pruebas `WEB/features/testing/{ParticipantSimulator,GuestSimulator}.test.tsx` (bandera desactivada → no se renderiza; dispositivo de prueba editable y reutilizable; envío de QR pegado o `sesion_id` según método; ubicación manual; respuesta y motivo visibles; invitado cerrado y abierto)
- [ ] T019 [US5] `WEB/app/pruebas/participante/page.tsx` (`notFound()` sin bandera) + `WEB/features/testing/ParticipantSimulator.tsx`: sesiones activas, formulario de intento (`dispositivo_id` UUID en `sessionStorage`, botón "Nuevo dispositivo"), historial (`/me/attendances`), porcentajes (`/me/stats`), solicitar justificación; aviso visible "Herramienta de pruebas: en producción el registro es desde la app"
- [ ] T020 [US5] `WEB/app/pruebas/invitado/page.tsx` (pública, `notFound()` sin bandera) + `WEB/features/testing/GuestSimulator.tsx`: `GET /events/open/{sessionId}`, `POST /guest-registrations` (QR o `sesion_id`), `POST /guest-registrations/submit` (cédula, `nombres` si es abierto, `dispositivo_id`)

## Phase 8: Polish

- [ ] T021 [P] E2E `apps/web/e2e/{docente,director,admin,simulador}.spec.ts` con el SSO (reutilizar `e2e/sso.ts`); en el job `browser` del CI activar `NEXT_PUBLIC_HERRAMIENTAS_PRUEBA=true`
- [ ] T022 [P] Lista de verificación `docs/validation/cobertura-web.md`: las 73 operaciones con la pantalla que la usa (SC-001), generada con un script que lee `src/generated/api-docs.json` y busca su uso en `apps/web/src`
- [ ] T023 [P] `README.md`: cómo activar el simulador y qué rol usar para cada prueba
- [ ] T024 Verificación: `npm run lint && npm run typecheck && npm test && npm run build` en `apps/web`, Playwright; resultados reales en `docs/validation/revision-incremento-006.md`

## Dependencies

- Foundational → todas las historias. US1 primero (navegación y `RoleGuard`). US2–US5 independientes entre sí.
- US2 toca `WEB/app/sesiones` y `features/sessions`; US5 solo `pruebas/*` y `features/testing`; US3 `estadisticas`; US4 `admin` y `spaces` → paralelizables.

## Parallel Example

```text
Tras US1:  A = US2 (T008–T012)   B = US5 (T018–T020)   C = US3 + US4 (T013–T017)   D = T022, T023
```

## Requirement Coverage

| FR | Tareas |
|---|---|
| FR-001 | T004–T007 |
| FR-002 | T005, T007 |
| FR-003 | T008–T010 |
| FR-004 | T011 |
| FR-005, FR-006 | T009, T012 |
| FR-007 | T013, T014 |
| FR-008 | T015–T017 |
| FR-009, FR-011 | T018–T020 |
| FR-010 | T002–T004 y pruebas de cada panel |
| SC-001 T022 · SC-002 T021 · SC-003 T009, T012 | |
