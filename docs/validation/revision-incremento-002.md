# Revisión del incremento 002 (gestión de espacios y eventos)

Fecha: 2026-10-25. Rama: `worktree-agent-acbc2a3be9ed12a0b`. Solo resultados realmente obtenidos.

## Resultados ejecutados

| Comprobación | Resultado |
|---|---|
| `./mvnw -B verify` (services/api, PostgreSQL por Testcontainers) | Éxito, 113 pruebas, 0 fallos, 0 errores, 0 omitidas (suma de `target/surefire-reports`) |
| `npm run validate` (tools/contracts) | OpenAPI 3.0.3 válido; 270 ejemplos verificados |
| `npm run generate` y `npm run check` | Tipos de web y móvil sincronizados |
| apps/web: `lint`, `typecheck`, `test` | Sin errores; 7 archivos, 13 pruebas pasan |
| apps/mobile: `lint`, `typecheck`, `test` | Sin errores; 14 pruebas pasan. Una primera ejecución concurrente con Maven falló 2 pruebas (carga de CPU); repetida sola pasó 14/14 |
| `npm view leaflet@1.9.4` / `@types/leaflet@1.9.21` | Ambas versiones existen en el registro |

## Cambios de revisión cubiertos por pruebas

- `ActiveSessionsTest.closedEventsAreListedOnlyForActiveGuestsAndOpenEventsForEveryone`: evento CERRADO visible solo para quien está activo en `invitados_evento`; ABIERTO visible para todos.
- `GuestConfigurationContractTest`: cédula inválida en el listado devuelve 400 `CEDULA_INVALIDA` (validación solo vía `Cedula.of`).
- `AuditValues` del contrato admite propiedades adicionales y documenta claves conocidas.

## Quickstart (T083)

Se cubrió únicamente lo que ejercitan las pruebas automatizadas anteriores (flujos de US1 a US5 en backend y pruebas de cliente).

No se ejecutó: recorrido manual completo de `quickstart.md`, pruebas Playwright de navegador, `infra/tests/retention-restore.sh`, ejecución en dispositivo o emulador móvil, y la medición cronometrada de SC-001, SC-002 y SC-005 (no hay mediciones reales que declarar).

## Análisis (T084)

Se ejecutó una verificación de trazabilidad FR/SC contra `tasks.md` (tabla de cobertura por rangos): sin requisitos sin tarea, sin marcadores pendientes. Se registró la decisión de US4 (eventos CERRADOS exigen listado también a personas con cuenta) en spec 002 (FR-015/FR-017, Assumptions) y en el ADR 0004; queda pendiente de revisión de otro integrante. No se abrió PR ni se hizo push.
