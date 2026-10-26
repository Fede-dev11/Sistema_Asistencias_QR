---
description: "Tareas de 004: servicio de autenticación universitaria simulado (SSO, CAS 3.0)"
---

# Tasks: Servicio de autenticación universitaria simulado (SSO)

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/api.md](contracts/api.md), [quickstart.md](quickstart.md)

**Tests**: obligatorios (Principio XII); cada historia empieza por sus pruebas.

**Convenciones**: `AUTH` = `services/auth/src/main/java/ec/edu/unl/sso`, `AUTHT` = `services/auth/src/test/java/ec/edu/unl/sso`,
`AUTHR` = `services/auth/src/main/resources`, `API` = `services/api/src/main/java/ec/edu/unl/geoasist`,
`APIT` = `services/api/src/test/java/ec/edu/unl/geoasist`, `YAML` = `specs/001-asistencia-qr-dinamico/contracts/openapi.yaml`.

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup

- [ ] T001 Confirmar rama `feature/004-autenticacion-sso` (creada desde 003) y `.specify/feature.json` → `specs/004-autenticacion-sso`
- [X] T002 Crear `services/auth` copiando la base de `services/api`: `pom.xml` (mismo parent Spring Boot 4.1.1, Java 21, spotless + google-java-format 1.28.0; dependencias web, jdbc, flyway, postgresql, security (cadena de filtros para CSRF en formularios, cabeceras y `PasswordEncoder`; sin login de Spring), thymeleaf, validation; test: spring-boot-starter-test, testcontainers postgresql), `mvnw`/`.mvn`, `Dockerfile` igual al de la API (puerto 8081), `AUTH/SsoApplication.java`, `AUTHR/application.yml` (`server.port=8081`, datasource `${AUTH_DATABASE_URL}`, `sso.ticket-ttl=10s`, `sso.session-idle=8h`, `sso.session-max=24h`, `sso.cookie-secure`, `sso.initial-password` desde `${DEV_SEED_PASSWORD}`)
- [ ] T003 [P] `infra/db-init/01-auth-db.sql` (`CREATE DATABASE geoasist_auth`) montado en `db` de `infra/docker-compose.yml`; servicio `auth` (perfil `api`, `127.0.0.1:${AUTH_PORT:-8081}:8081`, depende de `db` sano); `api` recibe `SSO_INTERNAL_URL=http://auth:8081`, `SSO_PUBLIC_URL`, `SSO_SERVICE_KEY`; `.env.example` con `AUTH_PORT`, `SSO_PUBLIC_URL=http://localhost:8081`, `SSO_SERVICE_KEY=`, `NEXT_PUBLIC_SSO_URL`
- [ ] T004 [P] `docs/adr/0006-servicio-autenticacion-simulado.md` (Propuesto): justificación Principio II y protocolo asumido Principio V (research R1, R2), alternativas (módulo en monolito, Apereo CAS, OIDC), condición de revisión del equipo
- [ ] T005 [P] Job `auth` en `.github/workflows/ci.yml`: `./mvnw -B spotless:check verify` en `services/auth`

## Phase 2: Foundational

- [X] T006 `AUTHR/db/migration/V001__usuarios_y_servicios.sql`: tablas `usuarios`, `usuario_roles`, `usuario_carreras`, `servicios`, `sesiones_sso`, `tickets`, `registro_ingresos`, `auditoria_admin`, `contadores_limite` exactamente como en data-model.md (CHECK de roles, `correo` único en minúsculas, `cedula char(10)` única, `clave_hash char(64)`, `hash`/`secreto_hash char(64) UNIQUE`, índice parcial de tickets por `vence_en WHERE usado_en IS NULL`)
- [X] T007 [P] `AUTH/compartido/Cedula.java` (misma regla módulo 10 que `API/compartido/dominio/Cedula.java`: 10 dígitos, provincia 01–24 o 30, tercer dígito < 6) con `AUTHT/compartido/CedulaTest.java`
- [X] T008 [P] `AUTH/compartido/Secrets.java`: aleatorio de 32 bytes `SecureRandom` en base64url, `sha256Hex`; `AUTH/compartido/RateLimiter.java` (contadores en BD por ventana de 1 min: login 5/correo y 50/IP) con prueba
- [X] T009 `AUTH/servicios/persistencia/ServiceRegistry.java` + `AUTH/servicios/aplicacion/ServiceMatcher.java`: `match(service)` devuelve el servicio activo cuyo prefijo de retorno coincide (comparación de esquema+host+puerto exacta y ruta por prefijo; sin comodines; `http` solo para `localhost`); prueba `AUTHT/servicios/ServiceMatcherTest.java` con casos de redirección abierta (`https://localhost.evil`, `//evil`, `http://localhost@evil`, mayúsculas, puertos)
- [X] T010 `AUTH/compartido/DevelopmentSeed.java` (perfil `dev`): usuarios con los mismos correos y cédulas que `API/identidades/aplicacion/DevelopmentSeed.java` y roles equivalentes, contraseña `sso.initial-password`, `debe_cambiar_password=false` en semillas; servicios `geoasist-web` (`http://localhost:3000/auth/callback`), `geoasist-movil` (`geoasist://auth/callback`), `ejemplo` (`http://localhost:8082/`) y clave de `geoasist-web`/`geoasist-movil` = `SSO_SERVICE_KEY`
- [X] T011 [P] `AUTHT/soporte/AuthFixture.java`: Testcontainers PostgreSQL, MockMvc, reloj ajustable (mismo patrón que `APIT/soporte/PostgresFixture.java` y `MutableClock`)

## Phase 3: US1 — Ingresar a GeoAsist web con la cuenta universitaria (P1) 🎯 MVP

**Independent Test**: docente ficticio ingresa por el servicio y ve sus actividades en GeoAsist sin escribir la contraseña en GeoAsist.

### Tests

- [X] T012 [P] [US1] `AUTHT/cas/LoginTest.java`: login correcto → TGC + 302 a `service?ticket=ST-…`; credenciales erróneas → mismo mensaje genérico, sin redirección; usuario inactivo → mensaje; 6.º intento fallido en 1 min → 429 con `Retry-After`; `debe_cambiar_password` → 302 a `/cas/password` y luego al servicio; registro en `registro_ingresos` sin contraseña
- [X] T013 [P] [US1] `AUTHT/cas/ServiceValidateTest.java`: JSON y XML con atributos `correo, cedula, nombres, roles, carreras, activo`; `INVALID_TICKET` reutilizado/vencido (reloj +11 s); `INVALID_SERVICE` con otro `service`; `INVALID_REQUEST` sin parámetros; 20 validaciones concurrentes del mismo ticket → exactamente 1 éxito
- [ ] T014 [P] [US1] `APIT/identidades/CasLoginTest.java` (GeoAsist, con servidor HTTP falso que responde JSON CAS grabado): canje válido → `LoginResult`; provisión crea persona `INSTITUCIONAL` y cuenta `CAS` sin contraseña; persona existente por cédula se enlaza (matrículas intactas); `EXTERNA` pasa a `INSTITUCIONAL`; roles reemplazados en el segundo ingreso; rol desconocido ignorado; cédula inválida → 422 `IDENTIDAD_INVALIDA`; `authenticationFailure` → 401 `TICKET_INVALIDO`; servicio caído → 502 `SSO_NO_DISPONIBLE`; `DIRECTOR_CARRERA` sincroniza `direcciones_carrera` por código de carrera; correo distinto para la misma cédula actualiza `cuentas.identificador`; `GET /auth/sso-config` devuelve URLs de configuración (prueba de integración y de contrato)
- [ ] T015 [P] [US1] `apps/web/src/features/auth/CasCallback.test.tsx`: callback con `ticket` llama a `POST /auth/cas` con el mismo `service` y guarda la sesión en memoria; sin `ticket` muestra error y botón para reintentar

### Implementation

- [X] T016 [US1] `AUTH/usuarios/persistencia/UserRepository.java` (búsqueda por correo en minúsculas, roles, carreras)
- [X] T017 [US1] `AUTH/cas/aplicacion/SsoSessions.java` (crear TGT, validar vigencia por inactividad 8 h y absoluta 24 h, actualizar `ultimo_uso_en`, cerrar) y `AUTH/cas/aplicacion/Tickets.java` (emitir ST de 10 s ligado a sesión, servicio y `service` exacto; consumo atómico con `UPDATE … WHERE usado_en IS NULL AND vence_en>now() RETURNING`)
- [X] T018 [US1] `AUTH/cas/presentacion/LoginController.java` + `AUTHR/templates/login.html`, `error.html`, `password.html`: `GET/POST /cas/login`, `GET/POST /cas/password`; cookie `CASTGC` (`HttpOnly`, `SameSite=Lax`, `Path=/cas`, `Secure` si `sso.cookie-secure`); CSRF en formularios; cabeceras `Cache-Control: no-store`, `X-Frame-Options: DENY`
- [X] T019 [US1] `AUTH/cas/presentacion/ValidateController.java`: `GET /cas/p3/serviceValidate` XML (por defecto) y JSON (`format=JSON`), códigos CAS de contracts/api.md; `AUTH/compartido/LogRedaction` para que `ticket` y `password` nunca se registren
- [X] T020 [US1] Contrato propio `specs/004-autenticacion-sso/contracts/auth-openapi.yaml` (login, logout, serviceValidate, users/status) y prueba de contrato en `AUTHT/cas/ContractTest.java`
- [ ] T021 [US1] GeoAsist: cambiar `API/identidades/aplicacion/IdentityProvider.java` a `VerifiedIdentity verifyTicket(String ticket, String service)` + `boolean isActive(String correo)`; `API/identidades/aplicacion/CasIdentityProvider.java` con `RestClient` (timeouts 2 s, URL interna `geoasist.sso.internal-url`); mantener la autenticación por contraseña como `LocalCredentials` activa solo si `geoasist.auth.local-login=true`
- [ ] T022 [US1] `API/identidades/aplicacion/AccountProvisioning.java` según research R5 (transacción, `Cedula.of`, roles conocidos, `direcciones_carrera`, auditoría `SISTEMA_PROVISION_CUENTA`); migración `services/api/src/main/resources/db/migration/V019__cuentas_sso.sql` (`proveedor` admite `CAS`, CHECK `proveedor<>'CAS' OR password_hash IS NULL`, `sincronizada_en`)
- [ ] T023 [US1] `API/identidades/presentacion/AuthController.java`: `POST /auth/cas`, `GET /auth/sso-config`; `/auth/login` → 404 si `geoasist.auth.local-login=false`; `application.yml` con `geoasist.sso.*` y `local-login: ${GEOASIST_LOCAL_LOGIN:false}`; perfil de pruebas con `local-login: true` en `APIT/soporte/PostgresFixture.java`
- [ ] T024 [US1] YAML: `POST /auth/cas`, `GET /auth/sso-config`, nota en `/auth/login`, códigos `TICKET_INVALIDO`, `CUENTA_INACTIVA`, `IDENTIDAD_INVALIDA`, `SSO_NO_DISPONIBLE`; `npm run validate && npm run generate` en `tools/contracts` (regenera también la documentación `/documentacion`; actualizar roles en `tools/contracts/docs.mjs` para `/auth/cas` y `/auth/sso-config` = Público)
- [ ] T025 [US1] Web: botón "Ingresar con cuenta universitaria" en `apps/web/src/features/auth/LoginForm.tsx` (lee `/auth/sso-config`), página `apps/web/src/app/auth/callback/page.tsx` con `features/auth/CasCallback.tsx`; el formulario de contraseña solo aparece si `/auth/login` responde (modo local)

## Phase 4: US2 — Una sola sesión para varios servicios (P1)

**Independent Test**: tras ingresar en GeoAsist, el servicio de ejemplo entra sin pedir contraseña; un ticket de GeoAsist no sirve al ejemplo.

- [ ] T026 [P] [US2] `AUTHT/cas/SingleSignOnTest.java`: con TGC vigente `GET /cas/login?service=` → 302 inmediato con ticket nuevo; ticket de `geoasist-web` validado con `service` de `ejemplo` → `INVALID_SERVICE`; `service` no registrado → 400 sin `Location`; logout cierra TGT y el siguiente login pide credenciales; inactividad de 8 h y máximo 24 h (reloj)
- [ ] T027 [US2] `AUTH/cas/presentacion/LogoutController.java` (`GET /cas/logout?service=`, solo redirige a servicios registrados) + plantilla `logout.html`
- [ ] T028 [P] [US2] Script de demostración `infra/tests/sso_demo.py` (solo biblioteca estándar): con una cookie `CASTGC` de un ingreso previo pide `GET /cas/login?service=http://localhost:8082/` sin credenciales, toma el `ticket` del `Location` y lo valida como servicio `ejemplo`; imprime el usuario y sus roles (demuestra SSO hacia un segundo servicio sin levantar otro contenedor)
- [ ] T029 [US2] GeoAsist: `POST /auth/logout` devuelve `sso_logout_url`; la web ofrece "Cerrar también la sesión universitaria" (`apps/web/src/features/auth/SessionProvider.tsx`)

## Phase 5: US3 — Ingresar desde la app móvil (P1)

**Independent Test**: emulador → navegador del sistema → vuelve a la app autenticado.

- [ ] T030 [P] [US3] `apps/mobile/src/features/auth/casLogin.test.ts`: nonce generado y verificado; retorno con nonce ajeno se descarta; cancelación deja sin sesión; canje usa el mismo `service`
- [ ] T031 [US3] `apps/mobile/src/features/auth/casLogin.ts` con `expo-web-browser` `openAuthSessionAsync` y `expo-crypto` (nonce de 16 bytes), `service=geoasist://auth/callback?n=<nonce>`; `apps/mobile/app.json` `scheme: "geoasist"`; añadir dependencias con `npx expo install`
- [ ] T032 [US3] `apps/mobile/src/features/auth/LoginScreen.tsx`: botón "Ingresar con cuenta universitaria" como acción principal; token de GeoAsist en SecureStore como hoy
- [ ] T033 [US3] `AUTHT/servicios/ServiceMatcherTest.java`: casos del esquema `geoasist://` (prefijo exacto, sin host ajeno)

## Phase 6: US4 — Administrar usuarios simulados (P2)

**Independent Test**: importar CSV, ingresar con un usuario, desactivarlo; su siguiente ingreso y su renovación en GeoAsist se rechazan.

- [ ] T034 [P] [US4] `AUTHT/usuarios/AdminUsersTest.java`: alta (correo único en minúsculas, cédula módulo 10 única, roles válidos, `debe_cambiar_password=true`); duplicados rechazados; desactivar cierra sesiones SSO; restablecer genera temporal de 16 caracteres mostrada una vez y cierra sesiones; acciones en `auditoria_admin` sin contraseñas; solo `ADMINISTRADOR` accede a `/admin/**`; alta/edición de servicios registrados valida prefijos de retorno (FR-010)
- [ ] T035 [P] [US4] `AUTHT/usuarios/CsvImportTest.java`: `correo,cedula,nombres,roles,carreras` (separador `|` en roles y carreras), informe por fila, idempotente por correo, encabezado inválido → error sin cambios, máx. 5 000 filas / 2 MB
- [ ] T036 [P] [US4] `AUTHT/usuarios/StatusEndpointTest.java`: `GET /api/v1/users/status?correo=` con `X-Service-Key` válida → `{activo}`; clave inválida → 401; correo inexistente → 404
- [ ] T037 [US4] `AUTH/usuarios/aplicacion/ManageUsers.java`, `CsvUserImport.java` (parser estándar como el de GeoAsist 002), `AUTH/usuarios/presentacion/AdminController.java` + plantillas `admin/usuarios.html`, `admin/usuario.html`, `admin/importar.html`, `admin/servicios.html`
- [ ] T038 [US4] `AUTH/usuarios/presentacion/StatusController.java` y cambio de contraseña propio (T018) que cierra las demás sesiones SSO
- [ ] T039 [US4] GeoAsist: `/auth/renew` consulta `isActive` y responde 403 `CUENTA_INACTIVA` (prueba en `APIT/identidades/SessionRenewalTest.java` con el servidor falso)
- [ ] T040 [US4] GeoAsist (FR-017): retirar `PUT /me/password` y `POST /admin/accounts/{id}/password-reset` (código, pruebas de 003 que los cubrían y YAML), quitar `password` de `POST/PATCH /admin/accounts`; la importación CSV de 002 crea cuentas de docentes **y estudiantes** sin contraseña (`proveedor='CAS'`); ajustar `APIT/academico/CsvImportTest.java`; actualizar `apps/web` admin si usa esos campos; documentar en `specs/003-ausencias-reportes-cuenta/spec.md` que US5 de 003 queda reemplazada por 004

## Phase 7: US5 — Cambiar al servicio real sin reescribir GeoAsist (P3)

- [ ] T041 [P] [US5] `APIT/soporte/ArchitectureDependencyTest.java`: ningún paquete fuera de `identidades` depende de `CasIdentityProvider` ni de clases HTTP del adaptador
- [ ] T042 [US5] `infra/README.md`: cómo apuntar a otro servicio CAS (variables `SSO_*`) y qué cambiar si el protocolo difiere (solo `CasIdentityProvider`)

## Phase 8: Polish

- [ ] T043 [P] E2E `apps/web/e2e/sso-login.spec.ts`: login por el servicio → `/auth/callback` → "Mis clases"; segundo ingreso sin contraseña; adaptar `apps/web/e2e/*` existentes para ingresar por SSO (o activar `GEOASIST_LOCAL_LOGIN=true` en el job `browser` si se decide mantenerlos con login local) y añadir el servicio `auth` al job `browser` de `.github/workflows/ci.yml`; registrar la duración del ingreso de punta a punta (SC-001 < 30 s)
- [ ] T044 [P] Actualizar `README.md`, `infra/README.md` (puertos, usuarios, `SSO_*`) y la página `/documentacion`
- [ ] T047 [P] Limpieza diaria de `registro_ingresos` con más de 90 días y de tickets vencidos en `AUTH/compartido/RetentionJob.java` (`@Scheduled`, desactivable en pruebas) con prueba `AUTHT/compartido/RetentionJobTest.java`
- [ ] T045 Prueba de privacidad `AUTHT/compartido/NoSecretsInLogsTest.java` y en GeoAsist (SC-004): ningún `ticket`, `password` ni `CASTGC` en logs capturados
- [ ] T046 Ejecutar quickstart y registrar resultados reales en `docs/validation/revision-incremento-004.md`; `/speckit-analyze`; PR a `develop` tras integrar 002 y 003, declarando IA y ADR 0006 pendiente

## Dependencies & Execution Order

- Setup → Foundational → US1. US2 y US3 dependen de US1 (login y tickets). US4 depende de Foundational y, para T039–T040, de US1 en GeoAsist. US5 al final.
- Dentro del servicio y de GeoAsist las líneas son independientes hasta T021–T024 (el adaptador necesita el JSON de T019, pero sus pruebas usan respuestas grabadas, así que puede avanzar en paralelo).
- T024, T040 tocan el YAML único: no en paralelo con otros cambios del YAML.

## Parallel Example

```text
Línea A (servicio):   T006–T013, T016–T020, T026–T027, T034–T038
Línea B (GeoAsist):   T014, T021–T024, T039–T040
Línea C (clientes):   T015, T025, T029–T032 (tras T024)
```

## Implementation Strategy

1. **MVP**: Setup + Foundational + US1 (web) → login universitario funcionando en GeoAsist web.
2. US2 (SSO entre servicios) y US3 (móvil).
3. US4 (administración y desactivación) y retiro de contraseñas en GeoAsist.
4. US5 + Polish + PR.

## Requirement Coverage

| FR | Tareas |
|---|---|
| FR-001, FR-006, FR-007 | T008, T012, T017, T018, T045, T047 |
| FR-002 | T009, T026, T033 |
| FR-003, FR-004 | T013, T017, T019, T020 |
| FR-005 | T026, T027 |
| FR-008, FR-009, FR-010 | T034–T038, T010 |
| FR-011, FR-013, FR-014 | T014, T021–T025, T039 |
| FR-012 | T030–T032 |
| FR-015 | T029 |
| FR-016 | T021, T041, T042 |
| FR-017, FR-018 | T040, T014 |
| SC-001 T043 · SC-002 T026, T043 · SC-003 T013, T026 · SC-004 T045 · SC-005 T023, T046 · SC-006 T042 | |
