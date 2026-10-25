---
description: "Tareas de 002: gestión académica simulada, espacios, actividades, eventos abiertos y dispositivo"
---

# Tasks: Gestión académica, espacios, actividades, eventos abiertos y unicidad por dispositivo

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/api.md](contracts/api.md), [quickstart.md](quickstart.md)

**Tests**: obligatorios por la constitución (Principio XII). Cada historia empieza por sus pruebas,
que deben fallar antes de implementar. Pruebas de integración contra PostgreSQL real con
`services/api/src/test/java/ec/edu/unl/geoasist/soporte/PostgresFixture.java`.

**Convenciones**: `API` = `services/api/src/main/java/ec/edu/unl/geoasist`, `TEST` =
`services/api/src/test/java/ec/edu/unl/geoasist`, `MIG` = `services/api/src/main/resources/db/migration`,
`YAML` = `specs/001-asistencia-qr-dinamico/contracts/openapi.yaml` (contrato único). Seguir las capas
`presentacion/aplicacion/dominio/persistencia` y el estilo de los casos de uso de 001 (JdbcTemplate,
`ApiFailure`, `ActivityAuthorization`, `ActivityAuditWriter`, `AttendanceLocks`, `IdempotencyService`).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia de usuario del spec (US1–US6)

---

## Phase 1: Setup

- [ ] T001 Crear la rama `feature/002-gestion-espacios-eventos` desde `develop` (no trabajar en `main`, Principio XIV) y confirmar que `.specify/feature.json` apunta a `specs/002-gestion-espacios-eventos`
- [ ] T002 [P] Crear los paquetes vacíos con `package-info.java` para `API/academico/{presentacion,aplicacion,persistencia}` y `API/espacios/{presentacion,aplicacion,persistencia}` y registrarlos en las reglas de dependencias de `TEST/soporte/ArchitectureDependencyTest.java` (academico y espacios no dependen de asistencia)
- [ ] T003 [P] Redactar `docs/adr/0004-metodos-sin-qr-y-autorregistro.md` (estado: Propuesto) con la desviación del Principio VI de `plan.md` Complexity Tracking, limitaciones y alternativas; sin declarar aprobación
- [ ] T004 [P] Redactar `docs/adr/0005-mapa-web.md` (Propuesto) eligiendo Leaflet + teselas OpenStreetMap para elegir coordenadas en web, con versión fija y atribución de OSM

---

## Phase 2: Foundational (bloquea todas las historias)

- [ ] T005 [P] Prueba unitaria `TEST/compartido/CedulaTest.java`: válidas (incluir provincias 01, 24 y 30), dígito verificador erróneo, longitud ≠ 10, tercer dígito ≥ 6, provincia 00/25, espacios alrededor
- [ ] T006 Implementar `API/compartido/dominio/Cedula.java` (record con `of(String)` que normaliza con `strip()` y valida módulo 10: 10 dígitos, provincia 01–24 o 30, tercer dígito < 6) lanzando `ApiFailure` 400 `CEDULA_INVALIDA` "La cédula no es válida." (R8)
- [ ] T007 Usar `Cedula.of` en `API/actividades/aplicacion/ReplaceGuestList.java` y `API/asistencia/presentacion/AttendanceInputs.java` (`GuestSubmit`) en lugar del regex suelto, para que exista una sola regla (Principio III)
- [ ] T008 Reemplazar las cédulas ficticias inválidas por cédulas válidas generadas en `API/identidades/aplicacion/DevelopmentSeed.java`, `TEST/soporte/ScenarioFixtures.java` y demás pruebas que las usen (buscar `cedula` en `TEST`), y ejecutar `./mvnw verify` para confirmar que 001 sigue verde
- [ ] T009 Crear `MIG/V009__academico_y_espacios.sql`: añadir `ADMINISTRADOR` al CHECK de `roles_cuenta.rol`; tablas `matriculas(persona_id, paralelo_id, vigente boolean NOT NULL DEFAULT true, PK(persona_id,paralelo_id))` y `docentes_paralelo(cuenta_id, paralelo_id, vigente boolean NOT NULL DEFAULT true, PK(cuenta_id,paralelo_id))`; en `espacios` añadir `codigo varchar(40)`, `codigo_normalizado varchar(40) UNIQUE`, `latitud`/`longitud double precision` con CHECK "ambos o ninguno; ±90 / ±180", `radio_defecto_m double precision NOT NULL DEFAULT 20 CHECK(>0)`, `precision_defecto_m double precision NOT NULL DEFAULT 30 CHECK(>0)`, `creado_por uuid REFERENCES cuentas(id)`; CHECK de `tipo IN ('AULA','LABORATORIO','AUDITORIO','EXTERIOR','OTRO')`; rellenar `codigo`/`codigo_normalizado` de filas existentes con `nombre` normalizado
- [ ] T010 Crear `MIG/V010__actividades_horarios_metodo.sql`: `actividades.acceso varchar(10)` con CHECK `(modalidad='EVENTO' AND acceso IN ('CERRADO','ABIERTO')) OR (modalidad='CLASE' AND acceso IS NULL)` (rellenar eventos existentes con `CERRADO`), `actividades.metodo_verificacion varchar(10) NOT NULL DEFAULT 'SOLO_QR'` CHECK `IN ('QR_Y_GPS','SOLO_QR','SOLO_GPS','NINGUNO')`, `actividades.espacio_id uuid REFERENCES espacios(id)`, `estado` admite `CANCELADA` (mismo requisito de `cerrada_en`/`anonimizar_en` que `CERRADA` y mismo trigger de inmutabilidad); tabla `horarios_clase(id, actividad_id, dias smallint[] NOT NULL, hora_inicio time, hora_fin time CHECK(hora_fin>hora_inicio), fecha_desde date, fecha_hasta date CHECK(fecha_hasta>=fecha_desde), espacio_id, zona varchar(40) NOT NULL DEFAULT 'America/Guayaquil')`; `sesiones.estado` admite `CANCELADA`, `sesiones.metodo_verificacion` (mismo CHECK, default `'SOLO_QR'`), `sesiones.horario_id uuid REFERENCES horarios_clase(id) ON DELETE SET NULL`; índices `matriculas(paralelo_id) WHERE vigente`, `docentes_paralelo(paralelo_id)`, `sesiones(horario_id)`
- [ ] T011 Crear `MIG/V011__dispositivo_y_autorregistro.sql`: `intentos_asistencia.dispositivo_hash char(64)` y `asistencias.dispositivo_hash char(64)`; `CREATE UNIQUE INDEX asistencias_sesion_dispositivo_uk ON asistencias(sesion_id, dispositivo_hash) WHERE dispositivo_hash IS NOT NULL`; `asistencias.forma` y `intentos_asistencia.canal` admiten `AUTORREGISTRO`
- [ ] T012 Crear `services/api/src/main/resources/db/geo-migration/V002__centro_generado.sql`: eliminar `espacios.centro` y `radio_recomendado_m`, volver a crear `centro geography(Point,4326) GENERATED ALWAYS AS (CASE WHEN latitud IS NULL THEN NULL ELSE ST_SetSRID(ST_MakePoint(longitud,latitud),4326)::geography END) STORED`; ajustar el INSERT de `TEST/sesiones/OptionalPostgisTest.java` para usar `latitud`/`longitud`
- [ ] T013 Ampliar la autorización: rol `ADMINISTRADOR` en `API/identidades/presentacion/ApiSecurityConfig.java` para `/api/v1/admin/**` (resto de roles → 403) y método `requireAnyRole(...)` reutilizable en `API/identidades/dominio/UserSession.java` si no existe
- [ ] T014 Añadir la cuenta `admin@dev.geoasist.local` con rol ADMINISTRADOR, un espacio con coordenadas (UNL, -4.0328, -79.2026), matrículas y asignaciones docente-paralelo de los datos existentes en `API/identidades/aplicacion/DevelopmentSeed.java`; documentar los IDs en `infra/README.md`
- [ ] T015 Añadir al YAML los esquemas comunes nuevos (`Space`, `ImportReport`, `VerificationMethod` enum, `DeviceId` = string uuid) y los códigos de motivo de `contracts/api.md`; ejecutar `npm run validate` en `tools/contracts`

**Checkpoint**: migraciones aplican sobre una base con datos de 001, 001 sigue en verde, cédulas validadas en un solo lugar.

---

## Phase 3: User Story 1 — Administrador gestiona la sección académica simulada (P1) 🎯 MVP

**Goal**: CRUD de personas, cuentas, carreras, paralelos, matrículas y docentes, e importación CSV.

**Independent Test**: base sin datos académicos → importar CSV de estudiantes, docentes y matrículas → los estudiantes inician sesión y aparecen en sus paralelos; reimportar no duplica.

### Tests for User Story 1

- [ ] T016 [P] [US1] Prueba de contrato `TEST/academico/AdminContractTest.java`: cada ruta `/admin/**` de `contracts/api.md` responde con el esquema del YAML; 403 para DOCENTE/ESTUDIANTE/DIRECTOR_*; 401 sin sesión
- [ ] T017 [P] [US1] Prueba de integración `TEST/academico/AdminPeopleAccountsTest.java`: escenarios US1-1, US1-2 (409 cédula existe), US1-3 (`CEDULA_INVALIDA`), US1-7 (cuenta desactivada no inicia sesión y conserva asistencias); acciones auditadas
- [ ] T018 [P] [US1] Prueba de integración `TEST/academico/CsvImportTest.java`: US1-4 (100 válidas + 3 inválidas → informe con número de fila y motivo; reimportación → `creadas: 0`), encabezado inválido → 400 sin cambios, separador `;` aceptado, >5 000 filas o >2 MB → 413, fila con correo duplicado rechazada
- [ ] T019 [P] [US1] Prueba de integración `TEST/academico/EnrollmentTest.java`: reemplazo de matriculados y docentes de un paralelo; matricular a una persona sin rol ESTUDIANTE se rechaza; `GET /me/parallels` del docente muestra sus paralelos con número de matriculados (US1-5)

### Implementation for User Story 1

- [ ] T020 [P] [US1] `API/academico/persistencia/AcademicRepository.java`: consultas JDBC de personas (búsqueda paginada por cédula/nombre), cuentas, roles, carreras, paralelos, matrículas y docentes_paralelo
- [ ] T021 [US1] `API/academico/aplicacion/ManagePeopleAndAccounts.java`: crear/editar persona (`Cedula.of`, nombres 1–200), crear cuenta (correo único, contraseña con el `PasswordEncoder` existente, roles válidos, una cuenta por persona), cambiar roles, activar/desactivar; auditar con quién/qué/cuándo (Principio V)
- [ ] T022 [US1] `API/academico/aplicacion/ManageCareersAndParallels.java`: CRUD de carreras y paralelos (`codigo` único por carrera), reemplazo de matrículas (solo personas con rol ESTUDIANTE) y de docentes (solo cuentas con rol DOCENTE/ENCARGADO), devolviendo filas rechazadas
- [ ] T023 [US1] `API/academico/aplicacion/CsvImport.java`: lector CSV de la librería estándar (UTF-8, comillas, separador `,`/`;` por encabezado, máx. 5 000 filas y 2 MB), tres tipos (`students` `cedula,nombres,correo`; `teachers` `cedula,nombres,correo,rol`; `enrollments` `cedula,carrera,paralelo`), una transacción, idempotente por cédula/correo/par, contraseña inicial desde configuración `geoasist.dev.import-password` (nunca del CSV), devuelve `ImportReport`
- [ ] T024 [US1] `API/academico/presentacion/AdminController.java` y `AdminImportController.java` con las rutas `/admin/**` de `contracts/api.md`, DTOs propios (no entidades), paginación ≤100, `Idempotency-Key` en POST de alta
- [ ] T025 [US1] `GET /me/parallels` en `API/academico/presentacion/TeacherCatalogController.java` (solo DOCENTE/ENCARGADO, sus asignaciones vigentes)
- [ ] T026 [US1] Añadir al YAML las rutas `/admin/**` y `/me/parallels`; `npm run validate && npm run generate` en `tools/contracts` para regenerar `apps/web/src/generated` y `apps/mobile/src/generated`
- [ ] T027 [P] [US1] Web: páginas `apps/web/src/app/admin/{personas,carreras,importar}/page.tsx` con componentes en `apps/web/src/features/admin/` (tablas paginadas, formularios con estados de carga/éxito/error, subida de CSV con informe de filas rechazadas; visible solo con rol ADMINISTRADOR)
- [ ] T028 [P] [US1] Fixtures `docs/validation/fixtures/{estudiantes,docentes,matriculas}.csv` con cédulas válidas ficticias y 3 filas inválidas para el quickstart

**Checkpoint**: US1 operable de forma independiente.

---

## Phase 4: User Story 2 — Docente o encargado crea o elige el espacio (P1)

**Goal**: espacios compartidos con código normalizado, coordenadas y tolerancia por defecto.

**Independent Test**: crear "A334" con coordenadas, buscarlo desde otro docente, rechazo de "a-334", rechazo de edición ajena.

### Tests for User Story 2

- [ ] T029 [P] [US2] Prueba `TEST/espacios/SpaceTest.java`: US2-1 a US2-4 (alta, `ESPACIO_CODIGO_EXISTE` con `espacio_id` del existente para `a-334`/`A 334`, coordenadas fuera de rango y radio ≤0 → 400, edición por no creador → 403, administrador puede editar); búsqueda paginada por código y nombre; ESTUDIANTE no puede crear
- [ ] T030 [P] [US2] Prueba `TEST/espacios/SpaceSnapshotTest.java`: US2-6 — editar `radio_defecto_m` no cambia `configuracion_geografica` de sesiones ACTIVA o CERRADA

### Implementation for User Story 2

- [ ] T031 [P] [US2] `API/espacios/dominio/SpaceCode.java`: normalización (mayúsculas, sin espacios ni guiones, 1–40 caracteres) con prueba unitaria en `TEST/espacios/SpaceCodeTest.java`
- [ ] T032 [US2] `API/espacios/persistencia/SpaceRepository.java` y `API/espacios/aplicacion/ManageSpaces.java`: crear (DOCENTE/ENCARGADO/ADMINISTRADOR, `creado_por`), editar (creador o ADMINISTRADOR), buscar; conflicto de unicidad → 409 con el id existente; auditar ediciones
- [ ] T033 [US2] `API/espacios/presentacion/SpaceController.java` con `GET/POST /spaces` y `PATCH /spaces/{spaceId}`; añadir al YAML y regenerar tipos
- [ ] T034 [US2] Cambiar `API/asistencia/persistencia/PostgisProximityAdapter.java` y `API/sesiones/dominio/GeographicCapability.java` para que "configurado" signifique espacio con `latitud`/`longitud` no nulos (US2-5)
- [ ] T035 [P] [US2] Web: `apps/web/src/features/spaces/SpacePicker.tsx` (búsqueda con debounce + "crear nuevo") y `SpaceForm.tsx` con mapa Leaflet (ADR 0005) para fijar latitud/longitud, radio y precisión por defecto; texto alternativo y entrada manual de coordenadas para accesibilidad

**Checkpoint**: US2 operable; las sesiones pueden usar espacios con coordenadas.

---

## Phase 5: User Story 3 — Docente crea una clase con horario recurrente (P1)

**Goal**: crear clases con paralelos, lista autorizada derivada, horario que genera sesiones, sesiones sueltas, edición y cancelación.

**Independent Test**: clase lunes y miércoles 08:00–10:00 por 4 semanas → 8 sesiones; solo matriculados registran.

### Tests for User Story 3

- [ ] T036 [P] [US3] Prueba unitaria `TEST/actividades/ScheduleExpansionTest.java`: días ISO, cruce de mes, `hasta` inclusivo, conversión `America/Guayaquil`→UTC, fin ≤ inicio → error, >200 sesiones → `HORARIO_DEMASIADO_LARGO` (SC-006)
- [ ] T037 [P] [US3] Prueba de integración `TEST/actividades/CreateClassTest.java`: US3-1, US3-2 (`PARALELO_NO_ASIGNADO`), US3-3, US3-6, US3-8; la lista autorizada son los matriculados vigentes; la clase queda con `actividad_carreras`/`actividad_paralelos` para el ámbito del director de carrera de 001
- [ ] T038 [P] [US3] Prueba de integración `TEST/actividades/AuthorizedListTest.java`: US3-4 (quitar/añadir auditados, asistencias existentes intactas), US3-5 (sincronizar no reincorpora quitados), estudiante quitado ya no puede registrar
- [ ] T039 [P] [US3] Prueba de integración `TEST/sesiones/SessionEditingTest.java`: US3-7 (editar/cancelar PROGRAMADA; ACTIVA o CERRADA → `SESION_NO_EDITABLE`), reemplazar horario regenera solo programadas futuras sin intentos ni tokens, cancelar actividad solo sin sesiones iniciadas (estado `CANCELADA` con `anonimizar_en`)

### Implementation for User Story 3

- [ ] T040 [P] [US3] `API/actividades/dominio/WeeklySchedule.java`: record (días 1–7 sin repetidos, horas, rango, zona) con `expand()` → lista de ventanas UTC, máx. 200
- [ ] T041 [US3] `API/actividades/aplicacion/CreateActivity.java` (rama CLASE): validar rol DOCENTE, paralelos asignados vigentes, espacio existente; insertar actividad (`responsable_id` = cuenta), `actividad_carreras`/`actividad_paralelos`, `estudiantes_autorizados` desde `matriculas` vigentes, `horarios_clase` y sesiones generadas con `metodo_verificacion` de la actividad; todo en una transacción; auditar
- [ ] T042 [US3] `API/actividades/aplicacion/ManageAuthorizedStudents.java`: listar paginado, añadir por cédula (debe tener rol ESTUDIANTE), quitar (`vigente=false`), sincronizar (`INSERT ... ON CONFLICT DO NOTHING` desde matrículas vigentes, R6); auditar; usar `locks.activity`
- [ ] T043 [US3] `API/actividades/aplicacion/EditActivity.java` y `ReplaceSchedule.java`: editar nombre/espacio/método por defecto; reemplazar horario borrando solo sesiones `PROGRAMADA` con `ventana_inicio` futura sin filas en `intentos_asistencia` ni `tokens_qr` y regenerando; cancelar actividad (`CANCELADA`, `cerrada_en`, `anonimizar_en` con `RetentionPolicy` de 001)
- [ ] T044 [US3] `API/sesiones/aplicacion/ManageScheduledSessions.java`: sesión suelta, editar hora/espacio, cancelar; solo `PROGRAMADA`; auditar
- [ ] T045 [US3] Endpoints en `API/actividades/presentacion/ActivityCommandController.java` (`POST /activities`, `PATCH`, `/cancel`, `/schedule`, `/students/**`, `/sessions`) y `API/sesiones/presentacion/SessionController.java` (`PATCH /sessions/{id}`, `/cancel`); `GET /activities/{id}/sessions` en `ActivityQueryController.java`; YAML + tipos regenerados
- [ ] T046 [US3] Hacer que `API/sesiones/aplicacion/SessionLifecycle.java` rechace iniciar sesiones `CANCELADA` y que `API/actividades/aplicacion/CloseActivity.java` trate `CANCELADA` como no reabrible
- [ ] T047 [P] [US3] Web: `apps/web/src/features/activities/CreateClassForm.tsx` (paralelos de `/me/parallels`, `SpacePicker`, editor de horario con vista previa de fechas), `AuthorizedStudents.tsx` (quitar/añadir/sincronizar) y lista de sesiones con editar/cancelar en `apps/web/src/app/actividades/`

**Checkpoint**: un docente opera su clase de punta a punta sin datos semilla de actividades.

---

## Phase 6: User Story 4 — Encargado crea un evento cerrado o abierto (P1)

**Goal**: crear eventos con acceso CERRADO/ABIERTO; autorregistro con cédula y nombres en abiertos.

**Independent Test**: evento abierto → asistente con cuenta + externo autorregistrado; cédula con cuenta → `USAR_CREDENCIALES`.

### Tests for User Story 4

- [ ] T048 [P] [US4] Prueba `TEST/actividades/CreateEventTest.java`: US4-1 (evento cerrado con sesión programada y configuración de invitados con `tolerancia_invitado_segundos` por defecto 120), US4-8 (editar/cancelar antes de iniciar; `acceso` inmutable tras iniciar la primera sesión)
- [ ] T049 [P] [US4] Prueba `TEST/asistencia/OpenEventTest.java`: US4-2 (cuenta sin lista → ACEPTADO), US4-3 (autorregistro → forma `AUTORREGISTRO`, persona `EXTERNA` creada sin cuenta), US4-4 (`USAR_CREDENCIALES`), US4-5 (cédula inválida / nombres vacíos), US4-6 (persona externa existente: `nombres_registro` = escrito, `personas.nombres` sin cambio), US4-7 (evento cerrado sigue exigiendo listado y rechaza `nombres` en el envío)

### Implementation for User Story 4

- [ ] T050 [US4] Rama EVENTO de `API/actividades/aplicacion/CreateActivity.java`: rol DOCENTE o ENCARGADO, `acceso`, espacio, `inicio`/`fin` → una sesión; crear `configuracion_invitados` (`habilitado=true`, tolerancia enviada o 120, >40); carreras opcionales
- [ ] T051 [US4] Validar en `API/actividades/aplicacion/EditActivity.java` que `acceso` no cambie si alguna sesión salió de `PROGRAMADA`; en `ReplaceGuestList.java` rechazar listado para eventos `ABIERTO` (409)
- [ ] T052 [US4] `API/asistencia/aplicacion/AttendanceEligibility.java`: en `account()` permitir cualquier cuenta activa si `acceso='ABIERTO'` (comportamiento actual de EVENTO), y añadir `selfRegistered(activity, cedula, nombres)` que: exige `acceso='ABIERTO'`; si la cédula tiene cuenta → `USAR_CREDENCIALES`; si no existe persona la crea `tipo='EXTERNA'`; devuelve `Person` con los nombres escritos
- [ ] T053 [US4] `API/asistencia/aplicacion/SubmitGuestAttendance.java` y `API/asistencia/presentacion/AttendanceInputs.java` (`GuestSubmit.nombres` opcional 1–200): evento ABIERTO → `selfRegistered` y forma/canal `AUTORREGISTRO`; evento CERRADO → flujo de 001 y `nombres` presente → 400
- [ ] T054 [US4] `GET /events/open/{sessionId}` (datos públicos mínimos solo si la sesión es de un evento ABIERTO y ACTIVA) en `API/asistencia/presentacion/GuestRegistrationController.java`; YAML (`GuestSubmitRequest.nombres`) + tipos
- [ ] T055 [US4] Incluir personas `EXTERNA` autorregistradas en la anonimización de `API/conservacion/aplicacion/AnonymizeExpiredActivity.java` igual que invitados de listado, con caso en `TEST/conservacion/RetentionLifecycleTest.java`
- [ ] T056 [P] [US4] Web: `apps/web/src/features/activities/CreateEventForm.tsx` (tipo de acceso, `SpacePicker`, fecha/hora, tolerancia; listado de invitados existente solo si CERRADO)
- [ ] T057 [P] [US4] Móvil: en `apps/mobile/src/features/attendance/AttendanceScreen.tsx` mostrar el campo nombres en el formulario de invitado cuando el evento es ABIERTO y enviar `nombres`; prueba en `AttendanceScreen.test.tsx`

**Checkpoint**: eventos cerrados y abiertos creados y operados sin semilla.

---

## Phase 7: User Story 6 — Un dispositivo registra como máximo una asistencia por sesión (P1)

**Goal**: identificador de instalación obligatorio, guardado como HMAC y único por sesión.

**Independent Test**: mismo `dispositivo_id`, persona A acepta, persona B en la misma sesión → `DISPOSITIVO_YA_REGISTRADO`; en otra sesión → acepta.

### Tests for User Story 6

- [ ] T058 [P] [US6] Prueba `TEST/asistencia/DeviceUniquenessTest.java`: US6-1 a US6-4 y US6-7 en los tres canales (credenciales, invitado de listado, autorregistro); sin `dispositivo_id` o no-UUID → `DISPOSITIVO_REQUERIDO`; ni el listado, ni el CSV, ni la auditoría contienen el valor en claro (SC-008)
- [ ] T059 [P] [US6] Prueba de concurrencia `TEST/asistencia/DeviceConcurrencyTest.java`: 50 intentos simultáneos de personas distintas con el mismo dispositivo y sesión → exactamente 1 asistencia (SC-003); repetir 20 ejecuciones
- [ ] T060 [P] [US6] Caso US6-6 en `TEST/asistencia/ReviewConcurrencyTest.java`: aprobar el pendiente de B con el mismo dispositivo que la asistencia ya aprobada de A → 409 `DISPOSITIVO_YA_REGISTRADO`, sin asistencia creada

### Implementation for User Story 6

- [ ] T061 [US6] `AttendanceInputs.AccountAttempt` y `GuestSubmit`: campo `dispositivo_id` (`@NotNull` UUID); falta o formato inválido → 400 `DISPOSITIVO_REQUERIDO` "Registra tu asistencia desde la app GeoAsist." vía `API/identidades/presentacion/ApiProblemHandler.java`; excluir el valor de `toString()`
- [ ] T062 [US6] `API/asistencia/aplicacion/AttendanceRecording.java`: calcular `OpaqueSecrets.hmac("device:" + dispositivo_id)`, guardarlo en `intentos_asistencia.dispositivo_hash`; dentro del bloqueo de sesión, si otra persona ya tiene asistencia con ese hash → intento `RECHAZADO` `DISPOSITIVO_YA_REGISTRADO`; si es la misma persona → respuesta "ya registrado" de 001; insertar `asistencias.dispositivo_hash`; traducir la violación del índice `asistencias_sesion_dispositivo_uk` al mismo motivo (R2)
- [ ] T063 [US6] `API/asistencia/aplicacion/ResolveAttendanceReview.java`: al aprobar, copiar `dispositivo_hash` del intento y aplicar la misma comprobación (US6-6)
- [ ] T064 [US6] Confirmar que la huella de `API/compartido/aplicacion/IdempotencyService.java` incluye `dispositivo_id` (mismo `Idempotency-Key` con otro dispositivo → 409 de idempotencia de 001)
- [ ] T065 [US6] Anonimización: poner a NULL `dispositivo_hash` en intentos y asistencias en `API/conservacion/aplicacion/AnonymizeExpiredActivity.java`; verificar que `API/reportes/persistencia/AttendanceReadRepository.java` y `ExportAttendancesCsv.java` no seleccionan la columna
- [ ] T066 [US6] YAML: `dispositivo_id` obligatorio en `AttendanceAttemptRequest` y `GuestSubmitRequest`, códigos nuevos; regenerar tipos
- [ ] T067 [P] [US6] Móvil: `apps/mobile/src/features/device/installationId.ts` (UUID v4 con `expo-crypto`, guardado y leído de `expo-secure-store`, creado una sola vez) con prueba `installationId.test.ts`; enviarlo en `AttendanceScreen.tsx` y en la reanudación de `guestResume.ts`, reutilizando el mismo valor en reintentos
- [ ] T068 [P] [US6] Web: retirar `apps/web/src/app/asistencia/page.tsx` y `apps/web/src/features/attendance/` (R10, FR-026), quitar enlaces y pruebas asociadas, y mostrar en el inicio del participante el aviso "Registra tu asistencia desde la app GeoAsist"

**Checkpoint**: la regla de dispositivo se aplica en servidor en todos los canales.

---

## Phase 8: User Story 5 — Método de verificación y tolerancia geográfica (P2)

**Goal**: `QR_Y_GPS`, `SOLO_QR`, `SOLO_GPS`, `NINGUNO` por sesión; tolerancia heredada del espacio; listado de sesiones activas.

**Independent Test**: espacio con radio 30 m → sesión `SOLO_GPS` hereda 30 m, se ajusta a 15 m y registra sin QR dentro/fuera.

### Tests for User Story 5

- [ ] T069 [P] [US5] Prueba `TEST/sesiones/VerificationMethodTest.java`: US5-1, US5-2 (herencia y ajuste), US5-6 (`ESPACIO_SIN_UBICACION`), US5-8 (método/tolerancia fijos tras iniciar), iniciar con `*_GPS` sin configuración habilitada → 409
- [ ] T070 [P] [US5] Prueba `TEST/asistencia/MethodAttendanceTest.java`: US5-3 (`SOLO_GPS` sin QR, dentro/fuera/imprecisa con repetir y revisión de 001), US5-4 (`QR_REQUERIDO`), US5-5 (`NINGUNO` acepta solo con sesión ACTIVA; CERRADA → motivo de 001), `qr_token` en método sin QR → `METODO_NO_ADMITE_QR`; inicio de invitado por `sesion_id` solo en métodos sin QR
- [ ] T071 [P] [US5] Prueba `TEST/asistencia/ActiveSessionsTest.java`: US5-7 — `GET /me/active-sessions` devuelve clases donde está autorizado (vigente), eventos abiertos y eventos cerrados (cuenta), con `metodo_verificacion` y `requiere_ubicacion`; nunca sesiones PROGRAMADA/CERRADA/CANCELADA

### Implementation for User Story 5

- [ ] T072 [US5] `PATCH /sessions/{id}` acepta `metodo_verificacion` (T044); validar espacio con coordenadas para `*_GPS`; al pasar a método sin GPS deshabilitar `configuracion_geografica`
- [ ] T073 [US5] `API/sesiones/aplicacion/ConfigureSessionGeolocation.java`: `radius`/`maximumPrecision` opcionales → heredar `radio_defecto_m`/`precision_defecto_m` del espacio; `maximumAgeSeconds` por defecto 60; mantener el cálculo de modo `REVIEW_ONLY`/`AUTO` por evidencia de 001
- [ ] T074 [US5] `API/sesiones/aplicacion/SessionLifecycle.java`: al iniciar, comprobar coherencia método ↔ configuración geográfica ↔ coordenadas y no emitir QR (`QrRotationScheduler`/`IssueSessionQr`) para `SOLO_GPS`/`NINGUNO`; `GET /sessions/{id}/qr` → 409 en esos métodos
- [ ] T075 [US5] `API/asistencia/aplicacion/RecordAuthenticatedAttendance.java` y `StartGuestRegistration.java`: resolver la sesión por `qr_token` o por `sesion_id` según el método (exactamente uno), aplicar QR solo si el método lo incluye y GPS solo si lo incluye; `AttendanceInputs` con `qr_token` opcional y `sesion_id` opcional
- [ ] T076 [US5] `GET /me/active-sessions` en `API/asistencia/presentacion/ActiveSessionsController.java` con consulta paginada en `API/asistencia/persistencia/`
- [ ] T077 [US5] YAML (`qr_token` opcional, `sesion_id`, `metodo_verificacion` en sesión/actividad, `/me/active-sessions`, `GeolocationRequest` con campos opcionales) + tipos regenerados
- [ ] T078 [P] [US5] Web: selector de método en la lista de sesiones y en los formularios de actividad, con advertencia visible para `NINGUNO` ("no comprueba presencia") y valores heredados del espacio editables
- [ ] T079 [P] [US5] Móvil: `apps/mobile/src/features/sessions/ActiveSessionsScreen.tsx` que lista `/me/active-sessions`; según método abre cámara (QR), pide ubicación en el momento de uso (GPS) o envía directo (`NINGUNO`); prueba de pantalla

**Checkpoint**: todas las historias completas.

---

## Phase 9: Polish & Cross-Cutting

- [ ] T080 [P] Actualizar `specs/001-asistencia-qr-dinamico/spec.md` Assumptions: la gestión de datos semilla pasa a 002, y referencia a FR-023..FR-026 de 002 para dispositivo y canal móvil (Principio XIII: coherencia)
- [ ] T081 [P] Actualizar `README.md` e `infra/README.md`: cuenta admin dev, importación CSV, configuración `geoasist.dev.import-password`
- [ ] T082 Añadir a `.github/workflows/ci.yml` las nuevas suites y la comprobación `npm run check` del contrato
- [ ] T083 Ejecutar `quickstart.md` de punta a punta y registrar resultados reales (sin inventar) en `docs/validation/revision-incremento-002.md`, incluido SC-001, SC-002 y SC-005 medidos
- [ ] T084 Ejecutar `/speckit-analyze` y resolver inconsistencias entre spec, plan y tasks; abrir PR hacia `develop` declarando el uso de IA y las desviaciones del ADR 0004 pendientes de revisión de otro integrante

---

## Dependencies & Execution Order

- **Setup (T001–T004)** → **Foundational (T005–T015)** → historias.
- **US1** (académico) y **US2** (espacios) son independientes entre sí.
- **US3** (clase) necesita US1 (matrículas y docentes) y US2 (espacios).
- **US4** (evento) necesita US2; no necesita US1.
- **US6** (dispositivo) solo necesita Foundational (T011): puede hacerse en paralelo con US1–US4. Si se hace antes de US4, T049/T053 deben incluir `dispositivo_id`.
- **US5** (método) necesita US2 (coordenadas); se beneficia de US3/US4 para crear sesiones sin semilla.
- Cada tarea de YAML (T015, T026, T033, T045, T054, T066, T077) toca el mismo archivo: no ejecutarlas en paralelo.
- Dentro de cada historia: pruebas → dominio → aplicación → controladores → YAML/tipos → clientes.

## Parallel Examples

```text
# Tras Foundational, tres líneas a la vez:
Línea A: T016–T028 (US1)
Línea B: T029–T035 (US2) → T048–T057 (US4)
Línea C: T058–T068 (US6)

# Dentro de US1, pruebas en paralelo:
T016, T017, T018, T019
# Dentro de US6, cliente y servidor en paralelo:
T067 (móvil) y T068 (web) mientras se hacen T061–T066
```

## Implementation Strategy

1. **MVP**: Setup + Foundational + US1 + US2 + US3 → un docente crea su clase y registra asistencias sin semilla.
2. **Incremento 2**: US6 (pedido explícito del usuario; puede adelantarse al MVP si la app móvil se prueba pronto).
3. **Incremento 3**: US4 (eventos cerrados/abiertos).
4. **Incremento 4**: US5 (métodos sin QR y listado de sesiones activas).
5. Polish y PR. Cada checkpoint se valida con la prueba independiente de su historia.

## Requirement Coverage

| Requisito | Tareas |
|---|---|
| FR-001, FR-004, FR-005 | T013, T014, T016–T027 |
| FR-002 | T005–T008, T021, T023, T052 |
| FR-003 | T018, T023 |
| FR-006–FR-008 | T009, T012, T029–T035 |
| FR-009–FR-011 | T010, T036–T043, T047 |
| FR-012, FR-013 | T039, T043, T044, T048, T050, T051, T056 |
| FR-014 | T017, T021, T032, T038, T042–T044 |
| FR-015–FR-017 | T049, T052–T054, T057 |
| FR-018–FR-022 | T010, T069–T079 |
| FR-023–FR-026 | T011, T058–T068 |
| Privacidad / conservación | T055, T065 |
| SC-001 | T047, T083 · SC-002 T018, T083 · SC-003 T059 · SC-004 T058 · SC-005 T057, T083 · SC-006 T036 · SC-007 T016, T029, T037 · SC-008 T058 |
