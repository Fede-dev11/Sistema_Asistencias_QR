# Quickstart de validación: 002

Prerrequisitos y arranque iguales a [001 quickstart](../001-asistencia-qr-dinamico/quickstart.md)
(Docker Compose con PostgreSQL, perfil `dev`, `GEOASIST_HMAC_KEY`, `DEV_SEED_PASSWORD`). La semilla
`dev` de 002 añade una cuenta `admin@dev.geoasist.local` con rol ADMINISTRADOR.

## Pruebas automáticas

```sh
cd services/api && ./mvnw verify          # JUnit + Testcontainers PostgreSQL (incluye 002)
cd tools/contracts && npm run validate && npm run check   # contrato y tipos sin diferencias
```

## Recorrido manual (API)

1. **Administración (US1)**: login como admin → `POST /admin/imports/students` con
   `docs/validation/fixtures/estudiantes.csv`, luego `teachers` y `enrollments`. Esperado: informe con
   filas rechazadas por cédula inválida; repetir la importación ⇒ `creadas: 0`.
2. **Espacio (US2)**: login como docente → `POST /spaces` `{codigo:"A334", ..., latitud, longitud}` ⇒ 201;
   repetir con `codigo:"a-334"` ⇒ 409 `ESPACIO_CODIGO_EXISTE`.
3. **Clase (US3)**: `POST /activities` CLASE con dos paralelos y horario lunes/miércoles de 4 semanas ⇒
   8 sesiones; `GET /activities/{id}/students` ⇒ matriculados de ambos paralelos.
4. **Método y tolerancia (US5)**: `PATCH /sessions/{id}` `SOLO_GPS`; `PUT .../geolocation {habilitada:true}`
   ⇒ hereda radio del espacio; iniciar; `POST /attendance/attempts` con `sesion_id`, ubicación y
   `dispositivo_id` ⇒ ACEPTADO.
5. **Dispositivo (US6)**: con el mismo `dispositivo_id`, login como otro estudiante y enviar ⇒
   `DISPOSITIVO_YA_REGISTRADO`; sin `dispositivo_id` ⇒ `DISPOSITIVO_REQUERIDO`.
6. **Evento abierto (US4)**: `POST /activities` EVENTO ABIERTO, iniciar sesión, `POST /guest-registrations`
   con QR y `submit` con cédula válida sin cuenta + nombres ⇒ ACEPTADO forma `AUTORREGISTRO`; con una
   cédula que tiene cuenta ⇒ `USAR_CREDENCIALES`.

## Evidencia
Registrar resultados reales en `docs/validation/revision-incremento-002.md`; no declarar resultados no
ejecutados.
