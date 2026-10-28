# Contratos: 004

## A. Servicio de autenticación (`services/auth`) — subconjunto CAS 3.0

Documentado en `specs/004-autenticacion-sso/contracts/auth-openapi.yaml` (nuevo, propio del servicio;
no se mezcla con el contrato de GeoAsist porque es otro sistema).

| Método y ruta | Uso | Respuesta |
|---|---|---|
| `GET /cas/login?service=` | Con sesión SSO vigente: 302 a `service` + `ticket=ST-…`. Sin sesión: página de login. `service` no registrado: página de error 400, sin redirección | 302 / 200 HTML / 400 HTML |
| `POST /cas/login` (form `correo`, `password`, `service`) | Valida credenciales, crea TGT (`CASTGC`), 302 con ticket; si `debe_cambiar_password`, 302 a `/cas/password` primero | 302 / 200 HTML con error / 429 |
| `GET /cas/logout?service=` | Cierra TGT y borra cookie; si `service` registrado, 302 a él | 302 / 200 HTML |
| `GET /cas/p3/serviceValidate?service=&ticket=&format=JSON` | Valida y consume el ST | ver abajo |
| `GET/POST /cas/password` | Cambio de contraseña propio (requiere TGT) | HTML |
| `GET /api/v1/users/status?correo=` | `X-Service-Key` del servicio registrado → `{ "activo": bool }`; 401 sin clave válida; 404 si no existe | JSON |
| `/admin/**` (HTML) | Usuarios (lista, alta, edición, roles, carreras, activar, restablecer), importar CSV, servicios registrados. Requiere TGT con rol `ADMINISTRADOR` | HTML |

Respuesta JSON de `serviceValidate` (formato CAS 3.0):
```json
{ "serviceResponse": { "authenticationSuccess": {
  "user": "docente@example.test",
  "attributes": { "correo": ["docente@example.test"], "cedula": ["0100000017"], "nombres": ["Docente Ficticio"],
                  "roles": ["DOCENTE"], "carreras": ["COMP"], "activo": ["true"] } } } }
```
Fallo: `{ "serviceResponse": { "authenticationFailure": { "code": "INVALID_TICKET", "description": "…" } } }`
(códigos: `INVALID_REQUEST`, `INVALID_TICKET`, `INVALID_SERVICE`). El XML equivalente se sirve sin `format`.

CSV de usuarios: `correo,cedula,nombres,roles,carreras` (roles y carreras separados por `|`); contraseña
inicial desde configuración, nunca del CSV; informe por fila como en 002.

## B. GeoAsist — cambios en el contrato único (`specs/001-asistencia-qr-dinamico/contracts/openapi.yaml`)

| Cambio | Detalle |
|---|---|
| **Nuevo** `POST /auth/cas` | Público. Cuerpo `{ ticket: string (≤256), service: string (≤2048, uri) }` → 200 `LoginResult`. Errores: 401 `TICKET_INVALIDO` (inválido, usado, vencido u otro servicio), 403 `CUENTA_INACTIVA`, 422 `IDENTIDAD_INVALIDA` (cédula inválida o sin roles válidos), 502 `SSO_NO_DISPONIBLE` |
| **Nuevo** `GET /auth/sso-config` | Público. `{ login_url, logout_url, web_service, mobile_service_prefix }` para que web y móvil no codifiquen URLs |
| `POST /auth/login` | Documentado como "solo pruebas/desarrollo sin red"; 404 cuando `geoasist.auth.local-login=false` |
| `POST /auth/renew` | Añade 403 `CUENTA_INACTIVA` si el servicio informa la cuenta desactivada |
| `POST /auth/logout` | Respuesta añade `sso_logout_url` para cerrar también la sesión universitaria |
| **Retirados** | `PUT /me/password`, `POST /admin/accounts/{id}/password-reset`; `password` deja de existir en `POST/PATCH /admin/accounts` |
| `LoginResult` | Sin cambios de forma; `debe_cambiar_password` siempre false para cuentas `CAS` (lo gestiona el servicio) |

Configuración de GeoAsist: `geoasist.sso.internal-url`, `geoasist.sso.public-url`, `geoasist.sso.service-key`,
`geoasist.sso.web-service`, `geoasist.sso.mobile-service-prefix`, `geoasist.auth.local-login`.
