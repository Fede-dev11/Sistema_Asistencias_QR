---
description: "Tareas de 005: consultas faltantes, contrato completo y evidencia de GPS"
---

# Tasks: Consultas faltantes, contrato completo y evidencia de GPS

**Input**: [spec.md](spec.md), [plan.md](plan.md). **Tests**: obligatorios (Principio XII).
Convenciones `API`, `APIT`, `YAML` como en [004 tasks](../004-autenticacion-sso/tasks.md).

## Phase 1: Setup

- [X] T001 Confirmar rama `feature/005-consultas-faltantes` (desde 004) y `.specify/feature.json` → `specs/005-consultas-faltantes`

## Phase 2: US1 — Ver el estado actual antes de editar (P1) 🎯 MVP

- [ ] T002 [P] [US1] `APIT/actividades/GuestReadTest.java`: escenarios US1-1, US1-2, US1-6 (no responsable → 403; director fuera de ámbito → 403; evento abierto → página vacía; paginación ≤ 100)
- [ ] T003 [P] [US1] `APIT/actividades/ScheduleReadTest.java`: US1-3 (con horario → días/horas/fechas/espacio; sin horario → 404 `SIN_HORARIO`; no responsable → 403)
- [ ] T004 [P] [US1] `APIT/sesiones/GeolocationReadTest.java`: US1-4 (OFF por defecto; habilitada con valores heredados; modo AUTO/REVIEW_ONLY y `evidencia_vigente`; legible en ACTIVA y CERRADA; no responsable → 403)
- [ ] T005 [P] [US1] `APIT/academico/ParallelReadTest.java`: US1-5 (matriculados y docentes paginados con `vigente`; no ADMINISTRADOR → 403; paralelo inexistente → 404)
- [ ] T006 [US1] `GET /activities/{id}/guest-list` y `GET /activities/{id}/guest-settings` en `API/actividades/presentacion/GuestManagementController.java` con consulta en `API/actividades/aplicacion/` reutilizando la autorización de `ReplaceGuestList`/`ConfigureGuests`
- [ ] T007 [US1] `GET /activities/{id}/schedule` (reutilizar autorización de `ReplaceSchedule`; 404 `SIN_HORARIO`)
- [ ] T008 [US1] `GET /sessions/{id}/geolocation` en `API/sesiones/presentacion/GeolocationController.java` (incluye `modo`, `evidencia_id`, `evidencia_vigente`)
- [ ] T009 [US1] `GET /admin/parallels/{id}/enrollments` y `/teachers` en `API/academico/presentacion/AdminController.java` con consultas paginadas en `API/academico/persistencia/AcademicRepository.java`
- [ ] T010 [US1] YAML: seis operaciones y esquemas (`GuestPage`, `GuestSettings`, `Schedule` reutilizado, `GeolocationConfig`, `EnrollmentPage`, `TeacherPage`), código `SIN_HORARIO`; roles en `tools/contracts/docs.mjs`; `npm run validate && npm run generate`

## Phase 3: US2 — Contrato que coincide con la API (P1)

- [ ] T011 [P] [US2] `APIT/actividades/ActivityContractTest.java`: `GET /activities/{id}` de una CLASE y de un EVENTO; el conjunto de claves de la respuesta es exactamente el declarado en `Activity` del YAML (leer el YAML en la prueba) y los tipos/nulabilidad coinciden
- [ ] T012 [US2] YAML `Activity`: declarar `acceso` (CERRADO|ABIERTO, nullable), `espacio_id` (uuid, nullable), `metodo_verificacion` (enum) y cualquier otra clave real que T011 detecte; `npm run validate && npm run generate`; `npm run typecheck` en `apps/web` y `apps/mobile`

## Phase 4: US3 — Evidencia de GPS en campo (P2)

- [ ] T013 [P] [US3] `APIT/espacios/GeoEvidenceTest.java`: US3-1 a US3-5 (alta con versión siguiente y auditoría; tasas fuera de 0–1, referencia vacía, fecha futura → 400; no ADMINISTRADOR → 403; espacio sin coordenadas → 409 `ESPACIO_SIN_UBICACION`; sin endpoint de edición ni borrado)
- [ ] T014 [P] [US3] Caso en `APIT/sesiones/VerificationMethodTest.java` o `GeolocationReadTest.java`: sin evidencia → REVIEW_ONLY; tras registrar evidencia, nueva configuración → AUTO (SC-003)
- [ ] T015 [US3] `API/espacios/aplicacion/GeoEvidence.java` (alta en transacción con `version = max+1`, `responsable_id` = actor, `verificada_en` = ahora, auditoría en `auditoria_espacios`) y lectura por espacio
- [ ] T016 [US3] `POST/GET /spaces/{id}/geo-evidence` en `API/espacios/presentacion/SpaceController.java`; YAML (`GeoEvidence`, `GeoEvidenceCreate`), roles en `docs.mjs` (POST: Administrador; GET: Docente, Encargado, Administrador); `npm run validate && npm run generate`

## Phase 5: Polish y estabilidad

- [ ] T017 [P] Identificar las pruebas web (1) y móviles (2) que fallan bajo carga (correr `npm test` 5 veces con `mvnw verify` en paralelo, anotar nombres) y corregir su causa (esperas por tiempo fijo, timers no controlados); no subir solo los timeouts
- [ ] T018 [P] Actualizar `infra/README.md` (cómo registrar evidencia de GPS) y regenerar `/documentacion`
- [ ] T019 Verificación completa: `services/api` y `services/auth` `spotless:check verify`, contrato `validate && check`, web y móvil lint/typecheck/test, Playwright; comprobar SC-001 listando en `api-docs.json` las rutas con PUT/PATCH sin GET (debe dar cero); resultados reales en `docs/validation/revision-incremento-005.md`

## Dependencies

- US1, US2 y US3 son independientes entre sí; las tareas de YAML (T010, T012, T016) tocan el mismo archivo: no en paralelo entre agentes sin mezclar después.
- T017 es independiente de todo.

## Parallel Example

```text
Línea A: T002–T010 (US1)   Línea B: T011–T012 (US2)   Línea C: T013–T016 (US3)   Línea D: T017
```

## Requirement Coverage

| FR | Tareas |
|---|---|
| FR-001 | T002, T006, T010 |
| FR-002 | T003, T007, T010 |
| FR-003 | T004, T008, T010 |
| FR-004 | T005, T009, T010 |
| FR-005 | T002–T005 |
| FR-006 | T011, T012 |
| FR-007, FR-009 | T013, T015, T016 |
| FR-008 | T014 |
| SC-001 T019 · SC-002 T011 · SC-003 T014 | |
