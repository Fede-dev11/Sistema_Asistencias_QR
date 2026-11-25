# Cobertura web de operaciones de la API (006 · T022)

Generado con `npm run coverage:web` en `tools/contracts` desde `apps/web/src/generated/api-docs.json`. No editar a mano.

**Cubiertas: 73 de 73 operaciones.**

Criterio: el archivo de `apps/web/src` (sin `generated/` ni `*.test.*`) menciona los segmentos literales de la ruta en orden, con hasta 40 caracteres entre ellos; o el prefijo literal más el último segmento como cadena o `/segmento` (tramo final variable).

## Autenticación y cuenta (`auth`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/auth/login` | pub | Sí | `apps/web/src/app/page.tsx` |
| POST | `/auth/cas` | pub | Sí | `apps/web/src/features/auth/CasCallback.tsx` |
| GET | `/auth/sso-config` | pub | Sí | `apps/web/src/features/auth/CasCallback.tsx` |
| POST | `/auth/logout` | cta | Sí | `apps/web/src/features/auth/SessionProvider.tsx` |
| POST | `/auth/renew` | cta | Sí | `apps/web/src/features/auth/SessionProvider.tsx` |
| GET | `/me` | cta | Sí | `apps/web/src/features/activities/ActivityWizard.tsx` |

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
| GET | `/admin/parallels/{parallelId}/enrollments` | adm | Sí | `apps/web/src/features/admin/CareersPanel.tsx` |
| PUT | `/admin/parallels/{parallelId}/enrollments` | adm | Sí | `apps/web/src/features/admin/CareersPanel.tsx` |
| GET | `/admin/parallels/{parallelId}/teachers` | adm | Sí | `apps/web/src/features/admin/CareersPanel.tsx` |
| PUT | `/admin/parallels/{parallelId}/teachers` | adm | Sí | `apps/web/src/features/admin/CareersPanel.tsx` |
| POST | `/admin/imports/{kind}` | adm | Sí | `apps/web/src/features/admin/ImportPanel.tsx` |

## Espacios (`spaces`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| GET | `/spaces` | doc, enc, adm | Sí | `apps/web/src/app/admin/espacios/page.tsx` |
| POST | `/spaces` | doc, enc, adm | Sí | `apps/web/src/app/admin/espacios/page.tsx` |
| PATCH | `/spaces/{spaceId}` | doc, enc, adm | Sí | `apps/web/src/app/admin/espacios/page.tsx` |
| GET | `/spaces/{spaceId}/geo-evidence` | doc, enc, adm | Sí | `apps/web/src/features/spaces/GeoEvidencePanel.tsx` |
| POST | `/spaces/{spaceId}/geo-evidence` | adm | Sí | `apps/web/src/features/spaces/GeoEvidencePanel.tsx` |

## Clases y eventos (`activities`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| GET | `/activities` | doc, enc, dir | Sí | `apps/web/src/app/actividades/[id]/page.tsx` |
| POST | `/activities` | doc, enc | Sí | `apps/web/src/app/actividades/[id]/page.tsx` |
| GET | `/activities/{activityId}` | doc, enc, dir | Sí | `apps/web/src/app/actividades/[id]/page.tsx` |
| PATCH | `/activities/{activityId}` | doc, enc | Sí | `apps/web/src/app/actividades/[id]/page.tsx` |
| POST | `/activities/{activityId}/close` | doc, enc | Sí | `apps/web/src/features/sessions/SessionSummary.tsx` |
| GET | `/activities/{activityId}/guest-settings` | doc, enc, dir | Sí | `apps/web/src/features/activities/GuestListView.tsx` |
| PUT | `/activities/{activityId}/guest-settings` | doc, enc | Sí | `apps/web/src/features/activities/GuestListView.tsx` |
| GET | `/activities/{activityId}/guest-list` | doc, enc, dir | Sí | `apps/web/src/features/activities/GuestListView.tsx` |
| PUT | `/activities/{activityId}/guest-list` | doc, enc | Sí | `apps/web/src/features/activities/GuestListView.tsx` |
| GET | `/activities/{activityId}/audit` | doc, enc, dir | Sí | `apps/web/src/features/reports/ActivityAuditPanel.tsx` |
| GET | `/me/parallels` | doc, enc | Sí | `apps/web/src/features/activities/ActivityWizard.tsx` |
| POST | `/activities/{activityId}/cancel` | doc, enc | Sí | `apps/web/src/features/activities/ActivityDetail.tsx` |
| GET | `/activities/{activityId}/schedule` | doc, enc, dir | Sí | `apps/web/src/features/activities/SchedulePanel.tsx` |
| PUT | `/activities/{activityId}/schedule` | doc | Sí | `apps/web/src/features/activities/SchedulePanel.tsx` |
| GET | `/activities/{activityId}/students` | doc | Sí | `apps/web/src/features/activities/AuthorizedStudents.tsx` |
| POST | `/activities/{activityId}/students` | doc | Sí | `apps/web/src/features/activities/AuthorizedStudents.tsx` |
| DELETE | `/activities/{activityId}/students/{personId}` | doc | Sí | `apps/web/src/features/activities/AuthorizedStudents.tsx` |
| POST | `/activities/{activityId}/students/sync` | doc | Sí | `apps/web/src/features/activities/AuthorizedStudents.tsx` |

## Sesiones y QR (`sessions`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/sessions/{sessionId}/start` | doc, enc | Sí | `apps/web/src/features/home/TeacherToday.tsx` |
| POST | `/sessions/{sessionId}/close` | doc, enc | Sí | `apps/web/src/features/sessions/ProjectionView.tsx` |
| GET | `/sessions/{sessionId}/qr` | doc, enc | Sí | `apps/web/src/features/sessions/ProjectionView.tsx` |
| GET | `/sessions/{sessionId}/geolocation` | doc, enc, dir | Sí | `apps/web/src/features/sessions/GeolocationSettingsPanel.tsx` |
| PUT | `/sessions/{sessionId}/geolocation` | doc, enc | Sí | `apps/web/src/features/sessions/GeolocationSettingsPanel.tsx` |
| GET | `/sessions/{sessionId}/attendances` | doc, enc, dir | Sí | `apps/web/src/features/corrections/ManualAttendanceForm.tsx` |
| GET | `/sessions/{sessionId}/attendances/export` | doc, enc | Sí | `apps/web/src/features/reports/AttendanceReportPage.tsx` |
| GET | `/activities/{activityId}/sessions` | doc, enc | Sí | `apps/web/src/features/activities/ActivityList.tsx` |
| POST | `/activities/{activityId}/sessions` | doc, enc | Sí | `apps/web/src/features/activities/ActivityList.tsx` |
| PATCH | `/sessions/{sessionId}` | doc, enc | Sí | `apps/web/src/app/sesiones/[id]/page.tsx` |
| POST | `/sessions/{sessionId}/cancel` | doc, enc | Sí | `apps/web/src/features/activities/ClassSessions.tsx` |

## Registro de asistencia (`attendance`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/attendance/attempts` | est | Sí | `apps/web/src/features/testing/ParticipantSimulator.tsx` |
| GET | `/me/active-sessions` | est | Sí | `apps/web/src/features/testing/ParticipantSimulator.tsx` |

## Invitados y eventos abiertos (`guest-registrations`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/guest-registrations` | inv | Sí | `apps/web/src/features/testing/GuestSimulator.tsx` |
| POST | `/guest-registrations/submit` | inv | Sí | `apps/web/src/features/testing/GuestSimulator.tsx` |
| GET | `/events/open/{sessionId}` | inv | Sí | `apps/web/src/features/testing/GuestSimulator.tsx` |

## Revisión de intentos (`attempts`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/attempts/{attemptId}/review` | doc, enc | Sí | `apps/web/src/features/reviews/ReviewPanel.tsx` |

## Correcciones y justificaciones (`corrections`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| POST | `/sessions/{sessionId}/manual-attendances` | doc, enc | Sí | `apps/web/src/features/corrections/ManualAttendanceForm.tsx` |
| POST | `/sessions/{sessionId}/absences/{personId}/justify` | doc, enc | Sí | `apps/web/src/features/corrections/AbsencesPanel.tsx` |
| POST | `/me/absences/{sessionId}/justification-requests` | est | Sí | `apps/web/src/features/testing/ParticipantSimulator.tsx` |
| GET | `/activities/{activityId}/justification-requests` | doc, enc | Sí | `apps/web/src/features/corrections/JustificationInbox.tsx` |
| POST | `/justification-requests/{requestId}/decision` | doc, enc | Sí | `apps/web/src/features/corrections/JustificationInbox.tsx` |

## Estadísticas e historial (`stats`)

| Método | Ruta | Roles | Cubierta | Primer archivo |
|---|---|---|---|---|
| GET | `/activities/{activityId}/stats` | doc, enc, dir | Sí | `apps/web/src/features/stats/ActivityStats.tsx` |
| GET | `/stats/parallels` | doc, enc, dir | Sí | `apps/web/src/features/home/DirectorSummary.tsx` |
| GET | `/stats/careers` | doc, enc, dir | Sí | `apps/web/src/features/stats/CareerStats.tsx` |
| GET | `/sessions/{sessionId}/absences` | doc, enc, dir | Sí | `apps/web/src/features/corrections/AbsencesPanel.tsx` |
| GET | `/me/attendances` | est | Sí | `apps/web/src/features/testing/ParticipantSimulator.tsx` |
| GET | `/me/stats` | est | Sí | `apps/web/src/features/testing/ParticipantSimulator.tsx` |
