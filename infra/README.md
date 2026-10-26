# Entorno local ficticio

Requiere Java 21, Node 22.20.0 y Docker. Maven 3.9.11 se descarga mediante el wrapper.

1. Ejecutar `python3 infra/init-local-env.py` una sola vez. Genera `.env` privado e ignorado; no imprime secretos.
2. Ejecutar `docker compose --env-file .env -f infra/docker-compose.yml up -d db`. Usa PostgreSQL 17.7 en `127.0.0.1:5433` para no ocupar el servicio local de 5432.
3. Exportar las variables del archivo `.env` en la terminal local sin registrarlas y desde `services/api`, ejecutar `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.
4. Las cuentas solo existen en perfil `dev`: `docente@example.test`, `encargado@example.test`, `estudiante@example.test`, `director@example.test`, `director-carrera@example.test`, `otro-docente@example.test`, `otro-estudiante@example.test` y `inactiva@example.test`. La contraseña es el valor local de `DEV_SEED_PASSWORD`; la última cuenta está inactiva. No son cuentas UNL.

UUID sintéticos: prefijo `00000000-0000-0000-0000-` seguido de doce dígitos. Carreras 1/2; paralelos 11/12; clase 301, evento 302, evento ajeno 303; sesiones 401/402/403; personas 100..107; cuentas 200..207. Director de carrera 204 dirige carrera 1. Estudiante 102 está autorizado solo en clase 301. El encargado 201 gestiona evento 302; docente 200 la clase 301; docente 205 el evento de carrera 2.

Semillas de 002 (solo `dev`): administrador `admin@dev.geoasist.local` (persona 108, cuenta 208, rol ADMINISTRADOR, misma contraseña local). Espacio 501 "UNL" (-4.0328, -79.2026). Matrículas: personas 102 y 107 en paralelo 11, persona 106 en paralelo 12. Docentes de paralelo: cuenta 200 en paralelo 11, cuenta 205 en paralelo 12. Las cédulas sembradas son ficticias pero cumplen el módulo 10 (`0100000017` a `0100000082` para las personas 100..107, `2400000002` para la 108).

Los listados de invitados se precargan mediante la operación contractual; no se publican ni se habilitan por defecto. Para capturar QR o elegir sesión en este incremento se utiliza el ID de sesión sembrado: todavía no existe un endpoint de descubrimiento de sesiones.

No borrar volúmenes de Docker para reiniciar pruebas: Testcontainers usa una base independiente efímera. `.env` nunca se incluye en backups compartidos ni en Git.

Propiedades de 003: `geoasist.correccion.dias` (por defecto 7) fija el plazo de correcciones
(asistencia manual, justificaciones y revisión) tras el cierre de una sesión. El programador del
ciclo automático (`SessionCycleScheduler`) ejecuta `SessionCycle.tick()` cada 60 s; inicia y cierra
las sesiones de actividades con `ciclo_automatico` y reintenta cada 60 s un inicio fallido
(auditado como `SISTEMA_INICIO_FALLIDO`). La prueba de rendimiento SC-003 queda fuera de `verify`:
`./mvnw -B test -DexcludedGroups= -Dgroups=performance -Dtest=StatsPerformanceTest`.

Importación CSV (002, solo `dev`): `POST /api/v1/admin/imports/students` como administrador; requiere `geoasist.dev.import-password` (o `geoasist.dev-seed-password`), de lo contrario responde 409 `CONFIGURACION_INCOMPLETA`.

## Servicio de autenticación simulado (004)

`services/auth` es un subconjunto simulado de CAS 3.0 que hace de SSO local mientras no existe el servicio institucional. Es un servicio independiente con su propia base `geoasist_auth` y escucha en el puerto `8081`. La decisión de arquitectura está en `docs/adr/0006-servicio-autenticacion-simulado.md` (estado: Propuesto, pendiente de aprobación).

### Arranque

Se levanta junto con el resto de servicios (perfil `api`):

```sh
docker compose --env-file .env -f infra/docker-compose.yml --profile api up -d --build --wait
```

Variables necesarias en `.env` (ver `.env.example`):

- `SSO_SERVICE_KEY`: clave de los servicios registrados; obligatoria para `auth` y para la API.
- `SSO_PUBLIC_URL`: URL del SSO que usa el navegador (por defecto `http://localhost:8081`).
- `AUTH_PORT`: puerto del servicio de autenticación (por defecto `8081`).
- `NEXT_PUBLIC_SSO_URL`: URL del SSO para la aplicación web (`http://localhost:8081`).

`infra/db-init/01-auth-db.sql` crea la base `geoasist_auth` y solo se ejecuta al crear un volumen de Postgres nuevo. Si el volumen ya existe, créala a mano:

```sh
docker compose --env-file .env -f infra/docker-compose.yml exec db psql -U geoasist -c "CREATE DATABASE geoasist_auth"
```

### Cuentas de desarrollo y administración

- Las cuentas de `dev` usan los mismos correos y cédulas que las semillas de GeoAsist. La contraseña es el valor de `DEV_SEED_PASSWORD`.
- Administración HTML: `http://localhost:8081/admin/login` con `admin@dev.geoasist.local`.
- Servicios registrados en `dev`: `geoasist-web` (`http://localhost:3000/auth/callback`), `geoasist-movil` (`geoasist://auth/callback`) y `ejemplo` (`http://localhost:8082/`).

### Apuntar GeoAsist a otro servicio CAS

1. Cambiar `SSO_INTERNAL_URL` (dirección que usa la API para validar tickets), `SSO_PUBLIC_URL` (dirección que usa el navegador) y `SSO_SERVICE_KEY`.
2. Registrar en ese servicio las URL de retorno de GeoAsist (`geoasist-web` y `geoasist-movil`).
3. Si el protocolo institucional es distinto, solo cambia `CasIdentityProvider` en `services/api/src/main/java/ec/edu/unl/geoasist/identidades/aplicacion/`. El dominio depende únicamente del puerto `IdentityProvider`. (Pendiente: `CasIdentityProvider` aún no existe en el código.)

### Inicio de sesión local (`GEOASIST_LOCAL_LOGIN`)

`GEOASIST_LOCAL_LOGIN` vale `false` por defecto: `POST /auth/login` de GeoAsist responde 404 y el ingreso es solo por SSO. Las pruebas automáticas lo activan para obtener tokens por contraseña. (Pendiente: la variable aún no aparece en `.env.example` ni en la configuración de `services/api`.)
