# Revisión del incremento 006 (vistas web para probar toda la funcionalidad)

Entorno: Windows 11, Git Bash, Docker Desktop. Pila local aislada con el proyecto compose `geoasist-e2e`
(PostgreSQL 5434, API 18080, auth 18081, web `http://localhost:13000` con `npm run start`), sin tocar la pila de desarrollo.
El web se construyó con `NEXT_PUBLIC_HERRAMIENTAS_PRUEBA=true` y `NEXT_PUBLIC_API_URL=http://127.0.0.1:18080/api/v1`.
La API se levantó con `WEB_ORIGIN=http://localhost:13000`, `SSO_WEB_SERVICE`/`SSO_WEB_RETURN` hacia `http://localhost:13000/auth/callback`
y `GEOASIST_RATE_LIMIT_MULTIPLIER=10` (mediante un archivo compose adicional fuera del repositorio).

| Comprobación | Comando (en `apps/web`) | Resultado |
|---|---|---|
| Lint | `npm run lint` | sin errores ni avisos |
| Tipos | `npm run typecheck` | sin errores |
| Unitarias | `npm test` | `Test Files 23 passed (23)`, `Tests 72 passed (72)` |
| Build | `npm run build` | `Compiled successfully`; 16 páginas estáticas generadas |
| E2E | `npx playwright test --reporter=line` | `8 passed (1.1m)` (admin, client-recovery, director, docente, reporting-scopes, session-lifecycle, simulador, sso-login) |
| Cobertura web | `npm run coverage:web` (en `tools/contracts`) | `Cubiertas: 73 de 73 operaciones` |

## Cambios hechos durante la verificación

- `tools/contracts/web-coverage.mjs`: además de los segmentos literales en orden, una operación cuenta como cubierta si el archivo tiene el prefijo
  literal y el último segmento como cadena entre comillas o `/segmento` (rutas como `/sessions/${id}/${kind}`). Pasó de no cubrir
  esas operaciones a 73 de 73.
- Nuevas specs `docente`, `director`, `admin` y `simulador`; datos propios por prueba (`e2e/data.ts`: fixture `create-activity.py` y SQL para preparar datos).
- `client-recovery` y `session-lifecycle` estaban desactualizadas respecto a la UI de 006 (ya no hay campo "Código de sesión", se elige con "Elegir"): se adaptaron.
  En `session-lifecycle` se eliminó el paso que probaba un código de sesión ajeno (`No tienes autorización`), porque la UI ya no permite escribir códigos;
  esa autorización sigue cubierta por las pruebas de la API.
- UI: `AbsencesPanel` se remonta al cambiar el estado de la sesión, de modo que tras cerrar la sesión se ven las ausencias sin recargar.
- CI (`browser`): `NEXT_PUBLIC_HERRAMIENTAS_PRUEBA: 'true'` para build y arranque del web.
- `simulador.spec` cierra por SQL las sesiones activas que dejan otras specs: el simulador solo lista 10 sesiones activas.

## No ejecutado / límites

- El job `browser` de CI no se ejecutó (solo la suite equivalente en local).
- Las pruebas de la API (Maven) no se ejecutaron en este incremento.
- La suite se ejecutó una sola vez completa sobre la base ya usada por ejecuciones parciales anteriores de la misma sesión.
- La cédula `1710034065` del invitado es sintética.
