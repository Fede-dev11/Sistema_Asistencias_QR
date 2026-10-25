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
