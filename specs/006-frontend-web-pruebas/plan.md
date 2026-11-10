# Implementation Plan: Vistas web básicas para probar todas las funcionalidades

**Branch**: `feature/006-frontend-web-pruebas` | **Date**: 2026-11-03 | **Spec**: [spec.md](spec.md)

## Summary

Solo `apps/web` (Next.js 16, React 19, TypeScript estricto). Navegación por rol en el layout,
renovación de sesión en `SessionProvider`, selector de sesiones en lugar del código pegado, paneles
nuevos para correcciones, horario, estadísticas, paralelos y evidencias, y dos simuladores
(`/pruebas/participante`, `/pruebas/invitado`) que solo existen con `NEXT_PUBLIC_HERRAMIENTAS_PRUEBA=true`.
Se reutilizan `lib/api/client.ts` (`request`, `ApiError`), los tipos generados (`src/generated/api.ts`)
y los estilos de `globals.css`. Sin dependencias nuevas.

## Technical Context

**Stack**: Next.js 16.4.2 (App Router; leer `apps/web/AGENTS.md` y `node_modules/next/dist/docs/`), React 19.2,
TypeScript estricto, Vitest + Testing Library, Playwright. **Contrato**: tipos generados desde
`specs/001-asistencia-qr-dinamico/contracts/openapi.yaml`. **Restricciones**: token solo en memoria;
accesibilidad básica; paginación ≤ 100.

## Constitution Check

| Principio | Tratamiento | Estado |
|---|---|---|
| II | Reglas siguen en el backend; la web solo muestra lo que responde | Cumple |
| IV | Un contrato para web y móvil; sin endpoints exclusivos | Cumple |
| IX | Componentes separados del acceso a datos (`features/*`), estados de carga/éxito/error, token en memoria, accesibilidad | Cumple |
| X / 002 FR-026 | Registro de participante solo en la app en producción; simulador tras bandera de pruebas | Cumple (bandera desactivada por defecto) |
| XII | Prueba de componente por panel nuevo y E2E por rol | Cumple |

## Diseño

| Pantalla / ruta | Contenido | Endpoints |
|---|---|---|
| Layout (todas) | Menú por rol, nombre, salir, aviso "sin acceso" | `GET /me`, `POST /auth/renew`, `POST /auth/logout` |
| `/sesiones` (rehecha) | Actividad → lista de sesiones → panel de sesión: iniciar/QR/cerrar, GPS (estado actual), asistencias en vivo, manual, ausencias, justificar | `GET /activities/{id}/sessions`, start/close/qr, `GET/PUT …/geolocation`, `GET /sessions/{id}/attendances`, manual-attendances, absences, justify |
| `/actividades/[id]` | Ficha: estudiantes + sincronizar, horario (ver/reemplazar), invitados y configuración (ver/editar), bandeja de justificaciones, auditoría | schedule, students/sync, guest-list, guest-settings, justification-requests, decision, audit |
| `/estadisticas` | Por clase (docente) y por paralelo/carrera con periodo (directores) | `GET /activities/{id}/stats`, `/stats/parallels`, `/stats/careers` |
| `/admin/carreras` (+ paralelo) | Matriculados y docentes ver/reemplazar | `GET/PUT /admin/parallels/{id}/enrollments`, `/teachers` |
| `/admin/espacios` | Lista de espacios, evidencias GPS (ver/registrar) | `GET /spaces`, `GET/POST /spaces/{id}/geo-evidence` |
| `/pruebas/participante` (bandera) | Sesiones activas, registrar (QR pegado o sesión, ubicación manual, dispositivo de prueba), historial, porcentajes, solicitar justificación | `/me/active-sessions`, `/attendance/attempts`, `/me/attendances`, `/me/stats`, `/me/absences/{id}/justification-requests` |
| `/pruebas/invitado` (bandera, pública) | Datos del evento abierto, abrir formulario, enviar cédula (+ nombres) | `/events/open/{id}`, `/guest-registrations`, `/guest-registrations/submit` |

Componente común nuevo: `features/common/{Paginated,ProblemMessage,RoleGuard}.tsx` (paginación,
mensaje de Problem Details, bloqueo por rol). La bandera se lee en el servidor de cada ruta de
`/pruebas/*` y responde `notFound()` si está desactivada.

## Project Structure

```text
apps/web/src/
├── app/{sesiones,actividades/[id],estadisticas,admin/espacios,pruebas/participante,pruebas/invitado}/page.tsx
└── features/{common,sessions,activities,corrections,stats,admin,spaces,testing}/*.tsx (+ *.test.tsx)
apps/web/e2e/{docente,director,admin,simulador}.spec.ts
```

## Complexity Tracking

Sin entradas.
