# Implementation Plan: Gestión académica, espacios, actividades, eventos abiertos y dispositivo

**Branch**: `feature/002-gestion-espacios-eventos` | **Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-gestion-espacios-eventos/spec.md`

## Summary

Extender el monolito de 001 sin cambiar su arquitectura: un módulo nuevo `academico`
(sección simulada de personas, cuentas, carreras, paralelos, matrículas e importación CSV),
creación de espacios con coordenadas y tolerancia por defecto, creación/edición/cancelación
de clases (con horario recurrente y lista desde paralelos) y eventos cerrados/abiertos, método
de verificación por sesión (`QR_Y_GPS`, `SOLO_QR`, `SOLO_GPS`, `NINGUNO`), autorregistro en
eventos abiertos y unicidad de asistencia por dispositivo (identificador de instalación con
HMAC e índice único por sesión). Las reglas de 001 (token, plazos, revisión, ámbito,
auditoría, conservación) se reutilizan, no se duplican. Decisiones en [research.md](research.md).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript estricto (web y móvil) — igual que 001

**Primary Dependencies**: Spring Boot 4.1.1 (MVC, Security, JDBC/JPA), Flyway; Next.js 16 y Expo SDK 57.
Sin dependencias nuevas en backend. Web: un componente de mapa (Leaflet + OpenStreetMap, ver tarea de
ADR) solo para elegir coordenadas. Móvil: `expo-crypto` (UUID) y `expo-secure-store` ya previstos.

**Storage**: PostgreSQL 17.7; PostGIS opcional como en 001

**Testing**: JUnit 6 + Testcontainers PostgreSQL; contrato con `tools/contracts`; tests de clientes existentes

**Target Platform**: servidor Linux en contenedor; Android/iOS (app) y navegador (responsables/admin)

**Project Type**: API web + cliente web + app móvil (estructura de 001)

**Performance Goals**: SC-002 importación de 1 000 estudiantes < 2 min; registro de asistencia mantiene
los objetivos de 001 (p95 < 2 s); la comprobación de dispositivo usa el bloqueo y el índice existentes.

**Constraints**: un único contrato OpenAPI; CSV ≤ 2 MB y 5 000 filas; ≤ 200 sesiones por horario;
zona horaria `America/Guayaquil`.

**Scale/Scope**: una institución; miles de estudiantes, cientos de actividades por periodo.

## Constitution Check

| Principio | Tratamiento | Estado |
|---|---|---|
| I. SDD | Spec aclarado (8 decisiones), FR/SC → tareas | Cumple |
| II. Modularidad | Módulo `academico` nuevo; reglas en servidor; la regla de dispositivo no depende del cliente | Cumple |
| III. SOLID | Validación de cédula en un único lugar; sin puertos nuevos sin segundo uso | Cumple |
| IV. API | Mismo YAML, Problem Details, paginación, idempotencia en altas | Cumple |
| V. Identidad | Rol ADMINISTRADOR, mínimo privilegio, auditoría de acciones administrativas; CAS sigue detrás de `IdentityProvider` | Cumple |
| VI. Registro | `SOLO_GPS` y `NINGUNO` no usan QR temporal; autorregistro sin prueba de titularidad | **Desviación pendiente de revisión** (Complexity Tracking, ADR 0004) |
| VII. GPS | Tolerancia por espacio solo como valor por defecto; repetición/revisión de 001 sin cambios; AUTO sigue exigiendo evidencia | Cumple |
| VIII. Integridad | Índice único sesión+dispositivo, bloqueo por sesión, migraciones versionadas, transacciones en importación | Cumple |
| IX. Web | Pantallas de admin, espacios con mapa, actividades; estados de carga/error; accesible | Cumple |
| X. Móvil | Identificador en almacenamiento seguro; reintentos idempotentes con el mismo identificador | Cumple |
| XI. Privacidad | Identificador como dato personal (HMAC, no visible, anonimizado); externos autorregistrados con mínimo de datos | Cumple; LOPDP pendiente institucional |
| XII. Pruebas | Unit (cédula, horario), integración, contrato, concurrencia de dispositivo, autorización por rol | Cumple |
| XIII. Docs | research, modelo, contrato, quickstart, ADR 0004 y ADR de mapa | Cumple |
| XIV. Colaboración | Rama `feature/002-...` hacia `develop` por PR; no commits a `main` | Condición de integración |

**Re-evaluación tras el diseño**: sin cambios; la única desviación es la del Principio VI.

## Project Structure

### Documentation (this feature)

```text
specs/002-gestion-espacios-eventos/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api.md      # resumen; el YAML vive en specs/001-.../contracts/openapi.yaml
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
services/api/src/main/java/ec/edu/unl/geoasist/
├── academico/            # NUEVO: presentacion/aplicacion/persistencia (personas, cuentas, carreras,
│                         #        paralelos, matrículas, docentes, importación CSV)
├── espacios/             # NUEVO: presentacion/aplicacion/persistencia
├── actividades/          # + crear/editar/cancelar, lista desde paralelos, horario
├── sesiones/             # + sesiones sueltas, edición, cancelación, método de verificación
├── asistencia/           # + dispositivo, métodos sin QR, autorregistro
├── compartido/dominio/   # + Cedula
└── conservacion/         # + anonimización de dispositivo y autorregistro
services/api/src/main/resources/db/migration/V009..V011
services/api/src/main/resources/db/geo-migration/V002
apps/web/src/app/{admin,espacios,actividades}/      # NUEVAS páginas; se retira asistencia/
apps/mobile/src/features/{device,sessions,attendance}/
```

**Structure Decision**: la de 001 (monolito modular + dos clientes). `espacios` se separa de
`sesiones` porque tiene ciclo de vida propio y lo usan actividades y sesiones.

## Complexity Tracking

| Desviación | Motivo | Alternativa descartada | Revisión |
|---|---|---|---|
| Principio VI: `SOLO_GPS` y `NINGUNO` sin QR temporal | Usuario pidió método configurable por sesión, incluso sin QR | QR siempre obligatorio (rechazado por el usuario 2026-10-25) | ADR 0004 propuesto; revisión de otro integrante antes de integrar |
| Principios V/VI: autorregistro por cédula en evento abierto | Eventos abiertos sin listado (decisión del usuario) | Solo cuentas UNL (descartado por el usuario) | ADR 0004; misma condición que ADR 0002 de 001 |
