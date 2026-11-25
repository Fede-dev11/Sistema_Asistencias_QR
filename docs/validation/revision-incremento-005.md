# Revisión del incremento 005 — Consultas faltantes, contrato completo y evidencia de GPS

**Rama**: `feature/005-consultas-faltantes` (desde `feature/004-autenticacion-sso`) · **Fecha**: 2026-10-28

Solo se registran resultados ejecutados realmente.

## Resultados

| Comprobación | Comando | Resultado |
|---|---|---|
| Backend GeoAsist | `services/api: ./mvnw -B spotless:check verify` | `Tests run: 162, Failures: 0, Errors: 0, Skipped: 0` · BUILD SUCCESS |
| Servicio de autenticación | `services/auth: ./mvnw -B spotless:check verify` | `Tests run: 105, Failures: 0, Errors: 0, Skipped: 0` · BUILD SUCCESS |
| Contrato | `tools/contracts: npm run validate && npm run check` | `OpenAPI 3.0.3: válido; 383 ejemplos verificados.` · `Tipos y documentación sincronizados.` |
| Web | `apps/web: npm run lint && npm run typecheck && npm test` | lint y tipos OK · `Test Files 9 passed (9)`, `Tests 21 passed (21)` |
| Móvil | `apps/mobile: npm run lint && npm run typecheck && npm test` | lint y tipos OK · `Test Suites: 7 passed`, `Tests: 21 passed` (tras `npm ci`: el `node_modules` local era anterior a 004) |

## SC-001 — toda escritura tiene lectura

Rutas con `PUT`/`PATCH` sin `GET` en la misma ruta (según `apps/web/src/generated/api-docs.json`, 73 operaciones): 4.
Las cuatro se leen por su colección: `PATCH /admin/accounts/{id}` → `GET /admin/people` (campo `cuenta`);
`PATCH /admin/careers/{id}` → `GET /admin/careers`; `PATCH /spaces/{id}` → `GET /spaces`;
`PATCH /sessions/{id}` → `GET /activities/{id}/sessions`. Criterio cumplido.

## SC-002 y SC-003

- `ActivityContractTest`: claves reales de `GET /activities/{id}` (clase y evento) = propiedades de `Activity` en el YAML. Faltaban `acceso`, `espacio_id`, `metodo_verificacion`; añadidas al contrato sin cambiar la API.
- `VerificationMethodTest`: sin evidencia → `REVIEW_ONLY`; con evidencia registrada → `AUTO`.
- `GeoEvidenceTest`: 4 altas concurrentes en un espacio → versiones únicas y sin huecos.

## T017 — pruebas inestables

Solo `apps/mobile/src/features/attendance/AttendanceScreen.test.tsx` fallaba bajo carga (primer render > 5 s). Arreglo: render previo en `beforeAll` con tiempo propio; las pruebas mantienen sus tiempos. 5 corridas seguidas en verde (las primeras con carga).

## No ejecutado

- Playwright E2E en local (lo ejecuta el job `browser` del CI al subir la rama).
- Ejemplo `curl` de `infra/README.md` contra una API levantada.
- Prueba HTTP de un director de carrera fuera de su ámbito en las nuevas lecturas (la regla está cubierta por `ActivityScopeTest`; la semilla de desarrollo da al director también el rol institucional).
