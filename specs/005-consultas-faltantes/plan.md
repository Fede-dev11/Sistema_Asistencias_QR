# Implementation Plan: Consultas faltantes, contrato completo y evidencia de GPS

**Branch**: `feature/005-consultas-faltantes` | **Date**: 2026-10-28 | **Spec**: [spec.md](spec.md)

## Summary

Solo backend y contrato. Seis lecturas nuevas que devuelven lo que ya guardan las escrituras
existentes, el esquema `Activity` completado y validado contra respuestas reales, y el alta y
consulta de evidencias de GPS sobre la tabla `evidencias_geograficas` que ya existe (V006).
`ConfigureSessionGeolocation` ya elige AUTO cuando hay evidencia: no cambia. Sin migraciones
nuevas salvo que la auditoría de evidencias lo requiera.

## Technical Context

Igual que 001–004: Java 21, Spring Boot 4.1.1, JDBC, PostgreSQL 17.7, JUnit 6 + Testcontainers,
contrato único `specs/001-asistencia-qr-dinamico/contracts/openapi.yaml`, tipos y `/documentacion`
generados con `tools/contracts`. Sin dependencias nuevas.

## Constitution Check

| Principio | Tratamiento | Estado |
|---|---|---|
| IV API | GET paginados, Problem Details, contrato actualizado y validado | Cumple |
| V Identidad | Lecturas con el mismo `ActivityAuthorization`/ámbito; evidencias solo ADMINISTRADOR; auditoría | Cumple |
| VII GPS | AUTO solo con evidencia real registrada; repetición y revisión intactas | Cumple (es el mecanismo que pedía T071 de 001) |
| VIII Integridad | Evidencias inmutables y versionadas (`UNIQUE(espacio_id, version)` existente) | Cumple |
| XII Pruebas | Integración, autorización y contrato por endpoint | Cumple |

Sin desviaciones.

## Diseño

| Lectura | Ruta | Reutiliza |
|---|---|---|
| Listado de invitados | `GET /activities/{id}/guest-list?page&size` | tabla `invitados_evento`; autorización de `ReplaceGuestList` |
| Configuración de invitados | `GET /activities/{id}/guest-settings` | `configuracion_invitados`; `ConfigureGuests` |
| Horario | `GET /activities/{id}/schedule` (404 `SIN_HORARIO` si no tiene) | `horarios_clase`; `ReplaceSchedule` |
| Configuración GPS | `GET /sessions/{id}/geolocation` | `configuracion_geografica` + `espacios` + última evidencia |
| Matriculados | `GET /admin/parallels/{id}/enrollments?page&size` | `matriculas`; `AcademicRepository` |
| Docentes | `GET /admin/parallels/{id}/teachers?page&size` | `docentes_paralelo`; `AcademicRepository` |
| Evidencias GPS | `POST /spaces/{id}/geo-evidence`, `GET /spaces/{id}/geo-evidence` | `evidencias_geograficas`; auditoría de espacios (`auditoria_espacios`, V013) |

`Activity`: declarar `acceso`, `espacio_id`, `metodo_verificacion` (y cualquier otro campo que la
respuesta real incluya) con nulabilidad correcta; prueba `ActivityContractTest` que valida la
respuesta real contra el esquema con el validador que ya usa `tools/contracts`
(o una aserción de claves exactas si el backend no tiene validador de esquema).

## Project Structure

```text
services/api/src/main/java/ec/edu/unl/geoasist/
├── actividades/presentacion/  GuestManagementController (+GET), ActivityCommandController o ActivityQueryController (+schedule)
├── sesiones/presentacion/      GeolocationController (+GET)
├── academico/presentacion/     AdminController (+GET enrollments/teachers)
└── espacios/                   GeoEvidence (aplicacion), SpaceController (+POST/GET geo-evidence)
specs/001-asistencia-qr-dinamico/contracts/openapi.yaml, tools/contracts/docs.mjs (roles)
```

## Complexity Tracking

Sin entradas.
