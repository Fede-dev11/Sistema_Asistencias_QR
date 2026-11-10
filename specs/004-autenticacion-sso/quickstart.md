# Quickstart de validación: 004

## Levantar

```sh
cp .env.example .env            # completar contraseñas; añadir SSO_SERVICE_KEY
docker compose --env-file .env -f infra/docker-compose.yml --profile api up -d --build --wait
cd apps/web && npm run dev      # http://localhost:3000
```
Servicios: auth `http://localhost:8081/cas/login`, API `http://localhost:8080/api/v1`, web `http://localhost:3000`.

## Pruebas automáticas

```sh
cd services/auth && ./mvnw -B spotless:check verify     # servicio de autenticación
cd services/api  && ./mvnw -B spotless:check verify     # GeoAsist (inicio local activo solo en pruebas)
cd tools/contracts && npm run validate && npm run check
cd apps/web && npm run lint && npm run typecheck && npm test && npx playwright test
```

## Recorrido manual

1. **US1** Web → "Ingresar con cuenta universitaria" → `docente@example.test` / `DEV_SEED_PASSWORD` →
   vuelve a `/auth/callback` y luego a "Mis clases". Contraseña errónea → "Correo o contraseña incorrectos".
2. **US2** Con la sesión abierta, abrir `http://localhost:8081/cas/login?service=http://localhost:3000/auth/callback`
   en otra pestaña → vuelve sin pedir contraseña. Copiar un ticket y validarlo dos veces con
   `curl "http://localhost:8081/cas/p3/serviceValidate?format=JSON&service=…&ticket=…"` → la segunda da `INVALID_TICKET`.
   `service=https://ejemplo.invalido` → página de error, sin redirección.
3. **US3** Emulador Android: "Ingresar con cuenta universitaria" → navegador del sistema → vuelve a la app con sesiones activas.
4. **US4** `admin@dev.geoasist.local` en `http://localhost:8081/admin` → importar CSV de usuarios, desactivar uno → su
   siguiente ingreso se rechaza y su `/auth/renew` en GeoAsist responde `CUENTA_INACTIVA`.
5. **US5** Cambiar `SSO_PUBLIC_URL`/`SSO_INTERNAL_URL` y reiniciar la API → usa la nueva dirección sin recompilar.

Registrar resultados reales en `docs/validation/revision-incremento-004.md`.
