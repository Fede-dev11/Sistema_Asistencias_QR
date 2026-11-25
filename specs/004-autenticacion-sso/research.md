# Research: 004 servicio de autenticación simulado (SSO)

**Fecha**: 2026-10-26. Base técnica: [001 research](../001-asistencia-qr-dinamico/research.md) R1–R3
(stack, contrato, cuenta/persona). Solo decisiones nuevas.

## R1. Un servicio separado (justificación Principio II)

- **Decisión**: nuevo servicio `services/auth` (Spring Boot, Java 21), con su propia base lógica
  `geoasist_auth` en el mismo PostgreSQL de desarrollo.
- **Motivo**: simula un sistema **externo** que GeoAsist no controla. Si viviera dentro del monolito,
  GeoAsist podría leer sus tablas y el adaptador no probaría la frontera real (redirección, canje
  servidor a servidor, caída del servicio). Otros servicios de la universidad lo usan, no solo GeoAsist.
- **Coste aceptado**: un despliegue más en docker-compose y en CI. Sin colas ni cachés distribuidas.
- **Registro**: Complexity Tracking del plan y ADR 0006 (propuesto).

## R2. Protocolo: CAS 3.0, como hipótesis (Principio V)

- **Decisión**: implementar el subconjunto de CAS 3.0 que usan los clientes web y móviles:
  `GET/POST /cas/login?service=`, `GET /cas/logout?service=`, `GET /cas/p3/serviceValidate?service=&ticket=[&format=JSON]`
  con atributos. Sin proxy tickets ni SAML.
- **Motivo**: decisión del usuario; es el protocolo más común en universidades y permite pasar al
  servicio real cambiando URLs si la UNL lo confirma.
- **Límite**: el protocolo real de la UNL sigue PENDIENTE. El dominio de GeoAsist depende solo del puerto
  `IdentityProvider`; un protocolo distinto implica otro adaptador, no otro dominio.
- **Alternativas**: OIDC (descartado por el usuario); usar un CAS existente como Apereo CAS en contenedor
  (descartado: pesado de configurar con atributos y semillas propias, y oculta la frontera que se quiere
  documentar; queda como opción si el equipo prefiere no mantener código propio).

## R3. Sesión SSO y tickets

- **TGT** (sesión universitaria): secreto aleatorio de 32 bytes en cookie `CASTGC` (`HttpOnly`,
  `SameSite=Lax`, `Secure` fuera de dev, `Path=/cas`); en BD solo su hash SHA-256. Vida 8 h por
  inactividad, 24 h absoluta.
- **ST** (prueba de identidad por servicio): `ST-` + 32 bytes aleatorios; en BD solo hash; vida 10 s
  (configurable); consumo atómico (`UPDATE ... SET usado_en=now() WHERE hash=? AND usado_en IS NULL AND
  vence_en>now() RETURNING ...`) → un solo uso aunque lleguen dos validaciones a la vez.
- **service**: se guarda la cadena exacta del login; la validación exige igualdad exacta (CAS
  `INVALID_SERVICE`). La pertenencia a un servicio registrado se decide por prefijo de URL de retorno
  permitido (sin comodines en el host).
- **Errores CAS**: `INVALID_REQUEST`, `INVALID_TICKET`, `INVALID_SERVICE` (formato estándar XML/JSON).

## R4. GeoAsist: canje de ticket y sesión propia

- **Decisión**: `POST /api/v1/auth/cas` `{ ticket, service }` → el backend llama a
  `serviceValidate?format=JSON` por la URL **interna** del servicio, provisiona la cuenta y devuelve el
  mismo `LoginResult` de `/auth/login` (token opaco 8 h, renovable 30 días; 003).
- **Web**: botón → `${SSO_PUBLIC_URL}/cas/login?service=${WEB_URL}/auth/callback`; la página
  `/auth/callback` lee `ticket` y llama al canje; el token sigue solo en memoria (Principio IX).
- **Móvil**: `expo-web-browser` `openAuthSessionAsync` con `service=geoasist://auth/callback?n=<nonce>`;
  la app compara el `n` devuelto con el que generó (FR-012) y canjea con el mismo `service`.
  El servicio registrado `geoasist-movil` permite el prefijo `geoasist://auth/callback`.
- **Por qué no reenviar el ticket al cliente para usarlo con la API**: el ticket es de un uso y 10 s;
  GeoAsist necesita su propia sesión revocable (001).

## R5. Provisión de cuentas en GeoAsist

- **Decisión**: en cada canje: buscar persona por cédula (`Cedula.of`); crearla `INSTITUCIONAL` si no
  existe o pasarla de `EXTERNA` a `INSTITUCIONAL`; crear/actualizar cuenta `proveedor='CAS'`,
  `identificador=correo`, `password_hash=NULL`; reemplazar roles por los recibidos que GeoAsist conoce;
  sincronizar `direcciones_carrera` para `DIRECTOR_CARRERA` según el atributo `carreras` (códigos).
  Todo en una transacción y auditado como acción del sistema.
- **Importación CSV de 002**: sigue creando personas y, para docentes, cuentas **sin contraseña**
  (pre-provisionadas) para poder asignar paralelos antes del primer ingreso; el primer canje las enlaza
  por cédula.
- **Cuenta desactivada**: `GET /api/v1/users/status?correo=` del servicio, protegido con una clave por
  servicio registrado (cabecera `X-Service-Key`, solo hash en BD), consultado en `/auth/renew`.

## R6. Inicio de sesión local de GeoAsist

- **Decisión**: `/auth/login` con contraseña queda **desactivado por defecto**
  (`geoasist.auth.local-login=false` → 404) y activo en el perfil de pruebas y, opcionalmente, en `dev`
  sin red. Se retiran de GeoAsist `PUT /me/password` y `POST /admin/accounts/{id}/password-reset`
  (pasan al servicio); `POST /admin/accounts` deja de aceptar contraseña.
- **Motivo**: las 146 pruebas obtienen tokens por contraseña; reescribirlas no aporta valor (SC-005).
  Una bandera explícita evita que el atajo llegue a producción.

## R7. Páginas del servicio

- **Decisión**: Thymeleaf para login, cambio de contraseña obligatorio, mensajes de error y una
  administración mínima (usuarios, importar CSV, servicios registrados). Sin JS de terceros.
- **Alternativa**: solo API REST de administración (descartada: el administrador del servicio no tendría
  interfaz y US4 la necesita para pruebas manuales).

## R8. Despliegue local

- **Decisión**: `infra/docker-compose.yml` añade `auth` (perfil `api`, puerto 8081) y un script de
  inicialización que crea la base `geoasist_auth`. GeoAsist usa `SSO_INTERNAL_URL=http://auth:8081` para
  validar y el navegador usa `SSO_PUBLIC_URL=http://localhost:8081`. Semillas `dev` con los mismos
  correos y cédulas de las cuentas actuales.
