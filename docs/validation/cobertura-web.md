# Cobertura web de operaciones de la API (006 · T022)

Generado con `npm run coverage:web` en `tools/contracts` desde `apps/web/src/generated/api-docs.json`. No editar a mano.

**Cubiertas: 53 de 73 operaciones.**

Criterio: el archivo de `apps/web/src` (sin `generated/` ni `*.test.*`) menciona los segmentos literales de la ruta en orden, con hasta 40 caracteres entre ellos.

## Autenticación y cuenta (`auth`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/auth/login` | pub | Sí | `apps/web/src/features/auth/LoginForm.tsx` |
| POST | `/auth/cas` | pub | Sí | `apps/web/src/features/auth/CasCallback.tsx` |
| GET | `/auth/sso-config` | pub | Sí | `apps/web/src/features/auth/CasCallback.tsx` |
| POST | `/auth/logout` | cta | Sí | `apps/web/src/features/auth/SessionProvider.tsx` |
| POST | `/auth/renew` | cta | Sí | `apps/web/src/features/auth/SessionProvider.tsx` |
| GET | `/me` | cta | Sí | `apps/web/src/features/activities/CreateClassForm.tsx` |

## Administración académica (`admin`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| GET | `/admin/people` | adm | Sí | `apps/web/src/features/admin/PeoplePanel.tsx` |
| POST | `/admin/people` | adm | Sí | `apps/web/src/features/admin/PeoplePanel.tsx` |
| GET | `/admin/people/{personId}` | adm | Sí | `apps/web/src/features/admin/PeoplePanel.tsx` |
| PATCH | `/admin/people/{personId}` | adm | Sí | `apps/web/src/features/admin/PeoplePanel.tsx` |
| POST | `/admin/accounts` | adm | Sí | `apps/web/src/features/admin/PeoplePanel.tsx` |
| PATCH | `/admin/accounts/{accountId}` | adm | Sí | `apps/web/src/features/admin/PeoplePanel.tsx` |
| GET | `/admin/careers` | adm | Sí | `apps/web/src/features/admin/CareersPanel.tsx` |
| POST | `/admin/careers` | adm | Sí | `apps/web/src/features/admin/CareersPanel.tsx` |
| PATCH | `/admin/careers/{careerId}` | adm | Sí | `apps/web/src/features/admin/CareersPanel.tsx` |
| GET | `/admin/careers/{careerId}/parallels` | adm | Sí | `apps/web/src/features/admin/CareersPanel.tsx` |
| POST | `/admin/careers/{careerId}/parallels` | adm | Sí | `apps/web/src/features/admin/CareersPanel.tsx` |
| GET | `/admin/parallels/{parallelId}/enrollments` | adm | No | — |
| PUT | `/admin/parallels/{parallelId}/enrollments` | adm | No | — |
| GET | `/admin/parallels/{parallelId}/teachers` | adm | No | — |
| PUT | `/admin/parallels/{parallelId}/teachers` | adm | No | — |
| POST | `/admin/imports/{kind}` | adm | Sí | `apps/web/src/features/admin/ImportPanel.tsx` |

## Espacios (`spaces`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| GET | `/spaces` | doc, enc, adm | Sí | `apps/web/src/features/activities/CreateClassForm.tsx` |
| POST | `/spaces` | doc, enc, adm | Sí | `apps/web/src/features/activities/CreateClassForm.tsx` |
| PATCH | `/spaces/{spaceId}` | doc, enc, adm | Sí | `apps/web/src/features/activities/CreateClassForm.tsx` |
| GET | `/spaces/{spaceId}/geo-evidence` | doc, enc, adm | No | — |
| POST | `/spaces/{spaceId}/geo-evidence` | adm | No | — |

## Clases y eventos (`activities`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| GET | `/activities` | doc, enc, dir | Sí | `apps/web/src/app/actividades/[id]/page.tsx` |
| POST | `/activities` | doc, enc | Sí | `apps/web/src/app/actividades/[id]/page.tsx` |
| GET | `/activities/{activityId}` | doc, enc, dir | Sí | `apps/web/src/app/actividades/[id]/page.tsx` |
| PATCH | `/activities/{activityId}` | doc, enc | Sí | `apps/web/src/app/actividades/[id]/page.tsx` |
| POST | `/activities/{activityId}/close` | doc, enc | Sí | `apps/web/src/features/sessions/ResponsibleSessionPanel.tsx` |
| GET | `/activities/{activityId}/guest-settings` | doc, enc, dir | Sí | `apps/web/src/features/activities/GuestListView.tsx` |
| PUT | `/activities/{activityId}/guest-settings` | doc, enc | Sí | `apps/web/src/features/activities/GuestListView.tsx` |
| GET | `/activities/{activityId}/guest-list` | doc, enc, dir | Sí | `apps/web/src/features/activities/GuestListView.tsx` |
| PUT | `/activities/{activityId}/guest-list` | doc, enc | Sí | `apps/web/src/features/activities/GuestListView.tsx` |
| GET | `/activities/{activityId}/audit` | doc, enc, dir | Sí | `apps/web/src/features/reports/ActivityAuditPanel.tsx` |
| GET | `/me/parallels` | doc, enc | Sí | `apps/web/src/features/activities/CreateClassForm.tsx` |
| POST | `/activities/{activityId}/cancel` | doc, enc | Sí | `apps/web/src/features/activities/ClassesPanel.tsx` |
| GET | `/activities/{activityId}/schedule` | doc, enc, dir | Sí | `apps/web/src/features/activities/SchedulePanel.tsx` |
| PUT | `/activities/{activityId}/schedule` | doc | Sí | `apps/web/src/features/activities/SchedulePanel.tsx` |
| GET | `/activities/{activityId}/students` | doc | Sí | `apps/web/src/features/activities/AuthorizedStudents.tsx` |
| POST | `/activities/{activityId}/students` | doc | Sí | `apps/web/src/features/activities/AuthorizedStudents.tsx` |
| DELETE | `/activities/{activityId}/students/{personId}` | doc | Sí | `apps/web/src/features/activities/AuthorizedStudents.tsx` |
| POST | `/activities/{activityId}/students/sync` | doc | No | — |

## Sesiones y QR (`sessions`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/sessions/{sessionId}/start` | doc, enc | No | — |
| POST | `/sessions/{sessionId}/close` | doc, enc | No | — |
| GET | `/sessions/{sessionId}/qr` | doc, enc | Sí | `apps/web/src/features/sessions/ResponsibleSessionPanel.tsx` |
| GET | `/sessions/{sessionId}/geolocation` | doc, enc, dir | Sí | `apps/web/src/features/sessions/GeolocationSettingsPanel.tsx` |
| PUT | `/sessions/{sessionId}/geolocation` | doc, enc | Sí | `apps/web/src/features/sessions/GeolocationSettingsPanel.tsx` |
| GET | `/sessions/{sessionId}/attendances` | doc, enc, dir | Sí | `apps/web/src/features/corrections/ManualAttendanceForm.tsx` |
| GET | `/sessions/{sessionId}/attendances/export` | doc, enc | Sí | `apps/web/src/features/reports/AttendanceReportPage.tsx` |
| GET | `/activities/{activityId}/sessions` | doc, enc | Sí | `apps/web/src/features/activities/ClassSessions.tsx` |
| POST | `/activities/{activityId}/sessions` | doc, enc | Sí | `apps/web/src/features/activities/ClassSessions.tsx` |
| PATCH | `/sessions/{sessionId}` | doc, enc | Sí | `apps/web/src/app/sesiones/page.tsx` |
| POST | `/sessions/{sessionId}/cancel` | doc, enc | Sí | `apps/web/src/features/activities/ClassSessions.tsx` |

## Registro de asistencia (`attendance`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/attendance/attempts` | est | No | — |
| GET | `/me/active-sessions` | est | No | — |

## Invitados y eventos abiertos (`guest-registrations`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/guest-registrations` | inv | No | — |
| POST | `/guest-registrations/submit` | inv | No | — |
| GET | `/events/open/{sessionId}` | inv | No | — |

## Revisión de intentos (`attempts`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/attempts/{attemptId}/review` | doc, enc | Sí | `apps/web/src/features/reviews/ReviewPanel.tsx` |

## Correcciones y justificaciones (`corrections`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/sessions/{sessionId}/manual-attendances` | doc, enc | Sí | `apps/web/src/features/corrections/ManualAttendanceForm.tsx` |
| POST | `/sessions/{sessionId}/absences/{personId}/justify` | doc, enc | Sí | `apps/web/src/features/corrections/AbsencesPanel.tsx` |
| POST | `/me/absences/{sessionId}/justification-requests` | est | No | — |
| GET | `/activities/{activityId}/justification-requests` | doc, enc | Sí | `apps/web/src/features/corrections/JustificationInbox.tsx` |
| POST | `/justification-requests/{requestId}/decision` | doc, enc | Sí | `apps/web/src/features/corrections/JustificationInbox.tsx` |

## Estadísticas e historial (`stats`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| GET | `/activities/{activityId}/stats` | doc, enc, dir | No | — |
| GET | `/stats/parallels` | doc, enc, dir | No | — |
| GET | `/stats/careers` | doc, enc, dir | No | — |
| GET | `/sessions/{sessionId}/absences` | doc, enc, dir | Sí | `apps/web/src/features/corrections/AbsencesPanel.tsx` |
| GET | `/me/attendances` | est | No | — |
| GET | `/me/stats` | est | No | — |
