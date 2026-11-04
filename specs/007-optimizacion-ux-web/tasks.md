---
description: "Tareas de 007: optimización de la experiencia web"
---

# Tasks: Optimización de la experiencia web

**Input**: [spec.md](spec.md), [plan.md](plan.md). **Tests**: obligatorios (Principio XII): prueba de
componente por pieza nueva y E2E que mide clics/altura. `WEB` = `apps/web/src`. Leer `apps/web/AGENTS.md`.

## Phase 1: Setup

- [X] T001 Confirmar rama `feature/007-optimizacion-ux-web` y `.specify/feature.json`; guardar la auditoría base (métricas de la spec) en `docs/validation/ux-auditoria-007.md`

## Phase 2: Foundational — sistema visual (bloquea todo)

- [ ] T002 Tokens del plan en `WEB/app/globals.css` (variables CSS, escala, espaciado, radios, foco visible, `prefers-reduced-motion`) y fuente Atkinson Hyperlegible con `next/font/google` en `WEB/app/layout.tsx`
- [ ] T003 [P] `WEB/features/ui/Button.tsx` (variantes primary/secondary/text/danger, tamaño mínimo 44 px, estado de carga) con prueba
- [ ] T004 [P] `WEB/features/ui/{StatusTag,EmptyState,ConfirmInline,Breadcrumbs}.tsx` con pruebas (texto + color; confirmación en página; migas con `aria-label="Migas de pan"`)
- [ ] T005 [P] `WEB/features/ui/{Tabs,Drawer,Stepper}.tsx` accesibles por teclado (roles tablist/tab/tabpanel, diálogo con foco atrapado y Escape, pasos con `aria-current="step"`), estado de pestaña en `searchParams`, con pruebas

## Phase 3: US1 — Llegar directo a mi trabajo (P1) 🎯 MVP

- [ ] T006 [P] [US1] Pruebas: redirección post-ingreso por rol; re-autenticación silenciosa (con marca → redirige a SSO con `destino`; callback vuelve a `destino`; destino externo rechazado; fallo → sin bucle)
- [ ] T007 [US1] `SessionProvider`/`CasCallback`: marca `sessionStorage['geoasist-sso']` (solo indicador), re-autenticación silenciosa al cargar sin sesión, `destino` validado como ruta interna; eliminar la página intermedia "Sesión iniciada" y redirigir al inicio del rol
- [ ] T008 [US1] `WEB/features/home/TeacherToday.tsx` en `WEB/app/hoy/page.tsx`: sesiones de hoy de todas las actividades del docente (activas primero) con una acción principal por estado; estado vacío con la próxima sesión
- [ ] T009 [P] [US1] `WEB/features/home/{DirectorSummary,AdminHome}.tsx` en `WEB/app/inicio/page.tsx` (resumen del ámbito con paralelos bajo umbral; tareas del administrador con contadores)
- [ ] T010 [US1] Navegación (`RoleNav` → barra lateral en escritorio, menú compacto en teléfono; ≤ 5 entradas por rol según el plan; sección "Pruebas" separada con la bandera; `aria-current`)

## Phase 4: US2 — Sesión en un clic y proyección (P1)

- [ ] T011 [P] [US2] Pruebas de `ProjectionView` (QR grande, cuenta regresiva desde `vigencia_segundos`, contador en vivo, pantalla completa con fallback si se rechaza, cierre con `ConfirmInline`) y `SessionSummary` (asistencias, ausencias, pendientes con acciones directas)
- [ ] T012 [US2] `WEB/app/sesiones/[id]/proyectar/page.tsx` + `features/sessions/ProjectionView.tsx` (tipografía 64 px, sin chrome de la app, alto contraste, `prefers-reduced-motion`); "Iniciar y proyectar QR" en "Hoy" inicia la sesión y navega aquí
- [ ] T013 [US2] `WEB/app/sesiones/[id]/page.tsx` + `features/sessions/SessionSummary.tsx`: en vivo mientras ACTIVA (reutilizar `LiveAttendances`), resumen al cerrar con `ManualAttendanceForm`, `AbsencesPanel` y revisiones en pestañas; la página `/sesiones` actual se reduce a la lista de sesiones con enlace a cada una

## Phase 5: US3 — Actividades sin sobrecarga (P1)

- [ ] T014 [P] [US3] Pruebas de `ActivityList` (búsqueda, filtros en URL, estado vacío) y `ActivityWizard` (3 pasos, validación por paso con error junto al campo, valores por defecto, resumen, creación de clase y de evento)
- [ ] T015 [US3] `features/activities/ActivityList.tsx` en `WEB/app/actividades/page.tsx`: solo la lista (nombre, tipo, `StatusTag`, próxima sesión) + "Nueva actividad"; quitar los formularios de esta página
- [ ] T016 [US3] `WEB/app/actividades/nueva/page.tsx` + `features/activities/ActivityWizard.tsx` reutilizando la lógica de `CreateClassForm`/`CreateEventForm`, `SpacePicker` y `MethodSelect` (último espacio usado como valor por defecto en `localStorage`, sin datos sensibles)
- [ ] T017 [US3] Ficha `WEB/app/actividades/[id]/page.tsx` con `Tabs` (Sesiones, Participantes, Horario o Invitados, Justificaciones, Auditoría) y `Breadcrumbs`; "Cancelar actividad" en menú secundario con `ConfirmInline`

## Phase 6: US4 — Jerarquía visual coherente (P2)

- [ ] T018 [US4] Reemplazar botones y estados en todas las pantallas por `Button` (una sola primaria por pantalla) y `StatusTag`; quitar `window.confirm` restantes (usar `ConfirmInline`); estados vacíos con `EmptyState`
- [ ] T019 [P] [US4] Revisión de accesibilidad: contraste de tokens, foco visible, objetivos ≥ 44 px, sin desplazamiento horizontal a 375 px; documentar en `docs/validation/ux-auditoria-007.md`

## Phase 7: US5 — Administración con panel lateral (P2)

- [ ] T020 [P] [US5] Pruebas de tablas con búsqueda/paginación y `Drawer` de alta/edición en Personas, Carreras y paralelos, Espacios
- [ ] T021 [US5] Rehacer `admin/personas`, `admin/carreras` (paralelo con pestañas Matriculados/Docentes en el panel) y `admin/espacios` (evidencias en el panel) con tabla + `Drawer`

## Phase 8: Polish y medición

- [ ] T022 E2E `apps/web/e2e/ux-clics.spec.ts`: mide SC-001..SC-006 (clics contados y altura de "Mis actividades" con 10 actividades) y falla si se supera la meta; ajustar E2E existentes a la nueva navegación
- [ ] T023 [P] Actualizar `README.md` (navegación por rol) y capturas antes/después en `docs/validation/ux-auditoria-007.md`
- [ ] T024 Verificación: lint, typecheck, test, build, Playwright completo, cobertura web 73/73 (`npm run coverage:web`); resultados reales en `docs/validation/revision-incremento-007.md`

## Dependencies

- Foundational (T002–T005) bloquea todo. US1 antes que US2 (la vista "Hoy" lanza la proyección).
- US3 y US5 son independientes entre sí y de US2. US4 (T018) al final, cuando las pantallas nuevas existen.

## Parallel Example

```text
Tras Foundational:  A = US1 → US2   B = US3   C = US5
Después:            D = US4 (T018–T019)   E = Polish
```

## Requirement Coverage

| FR | Tareas |
|---|---|
| FR-001, FR-002 | T006, T007 |
| FR-003, FR-004 | T008, T011–T013 |
| FR-005, FR-006 | T014–T017 |
| FR-007, FR-010 | T003, T004, T018 |
| FR-008, FR-009 | T004, T010, T017 |
| FR-011 | T020, T021 |
| FR-012 | T002, T005, T019 |
| SC-001..SC-007 | T022, T019 |
