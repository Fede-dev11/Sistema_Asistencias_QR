# Revisión del incremento 004: autenticación SSO simulada

Fecha: 2026-10-26. Rama de trabajo derivada de `feature/004-autenticacion-sso`. Todos los datos son ficticios.
Entorno: Windows 11, Docker Desktop, JDK 21, Node del repositorio. Pila local aislada con el proyecto compose
`geoasist-e2e` (PostgreSQL en 5434, API en 18080, auth en 18081, web en `http://localhost:13000`), sin tocar la pila de desarrollo.

## Resultados reales

| Comprobación | Comando | Resultado |
|---|---|---|
| Servicio auth | `cd services/auth && ./mvnw -B spotless:check verify` | `Tests run: 105, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS` |
| API GeoAsist | `cd services/api && ./mvnw -B spotless:check verify` | `Tests run: 153, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS` |
| Contratos | `cd tools/contracts && npm run validate && npm run check` | `OpenAPI 3.0.3: válido; 350 ejemplos verificados.` y `Tipos y documentación sincronizados.` |
| Web | `cd apps/web && npm run lint`, `typecheck`, `test`, `build` | lint y typecheck sin errores; build correcto; `Tests 21 passed (21)` |
| Móvil | `cd apps/mobile && npm run lint`, `typecheck`, `test` | lint y typecheck sin errores; `Tests: 21 passed, 21 total` |
| E2E | `cd apps/web && npx playwright test` | `4 passed (1.1m)` (client-recovery, reporting-scopes, session-lifecycle, sso-login) |
| Demostración SSO | `python infra/tests/sso_demo.py --base http://127.0.0.1:18081 --correo docente@example.test` | `Usuario: docente@example.test`, `Roles: DOCENTE`, `SSO correcto hacia http://localhost:8082/ sin pedir credenciales.` |

Notas de honestidad:

- La primera ejecución de `npm test` (web: 1 prueba fallida de 21) y de `npm test` (móvil: 2 fallidas de 21) coincidió con
  `mvnw verify` corriendo en paralelo en la misma máquina; repetidas sin carga pasaron todas (21/21 y 21/21). No se identificó
  la prueba exacta; parece sensibilidad a tiempos bajo carga. Conviene vigilar si reaparece en CI.
- La suite E2E se ejecutó sobre una base ya usada por una ejecución previa de la misma sesión (no solo sobre volumen nuevo).

## SC-001: ingreso de punta a punta

`sso-login.spec.ts` mide desde abrir `/login` hasta ver la vista autenticada con "Gestionar actividades" pasando por el
servicio SSO y `/auth/callback`: **1152 ms** (umbral < 30 000 ms, comprobado con `expect`). El segundo ingreso, tras cerrar
solo la sesión de GeoAsist, no vuelve a mostrar el formulario de contraseña (SSO).

## Cambios necesarios descubiertos al ejecutar

- Los E2E existentes entraban por el formulario local, que el web oculta cuando `/auth/sso-config` responde. Se migraron al
  flujo SSO (`apps/web/e2e/sso.ts`). `reporting-scopes` obtiene los tokens de API por el flujo CAS completo (`ssoToken`),
  porque tras un ingreso SSO la cuenta cambia de proveedor y `/auth/login` local deja de servir.
- El servicio auth solo admite `http` en `localhost`: el web de CI y local se sirve como `http://localhost:13000`. La URL de
  retorno de la semilla ahora es configurable con `SSO_WEB_RETURN` (por defecto `http://localhost:3000/auth/callback`).
- El job `browser` de CI levanta el servicio auth (jar en 8081) y define `SSO_SERVICE_KEY`, que `docker-compose.yml` exige
  incluso para levantar solo `db`.
- `tests/fixtures/create-activity.py` lee `GEOASIST_DB_CONTAINER` (por defecto `geoasist-db-1`).
- `infra/tests/sso_demo.py` estaba roto contra el servicio: enviaba la cookie CSRF sin enmascarar como `_csrf` (respuesta 403).
  Ahora lee el token enmascarado del formulario; el servicio de retorno del ingreso se puede cambiar con `SSO_DEMO_LOGIN_SERVICE`.

## No ejecutado

- Prueba en dispositivo o emulador Android (US3, `casLogin.ts` solo cubierto por pruebas unitarias de Jest).
- CAS real de la UNL: solo se valida contra el servicio simulado.
- Recorrido manual de US4 (consola de administración e importación CSV) y US5 (cambio de URL en caliente): cubiertos solo por
  las pruebas automáticas de los servicios.
- `bash infra/tests/retention-restore.sh` y los jobs de GitHub Actions (el workflow modificado no se ha ejecutado en CI).
- `/speckit-analyze` de T046.

## Pendiente de gobernanza

- La ADR 0006 (servicio de autenticación simulado) requiere aprobación antes de fusionar a `develop`.
- PR a `develop` de T046: no abierto; debe hacerse tras integrar 002 y 003, declarando el uso de IA y la ADR 0006 pendiente.
