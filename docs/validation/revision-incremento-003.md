# Revisión del incremento 003 (ausencias, reportes, cuenta)

Rama `feature/003-ausencias-reportes-cuenta`, tras integrar `feature/002-gestion-espacios-eventos`.
Resultados reales de la ejecución local de integración.

| Comprobación | Resultado |
|---|---|
| `./mvnw -B verify` (services/api) | BUILD SUCCESS, Tests run: 145, Failures: 0, Errors: 0, Skipped: 0 |
| `npm run validate` (tools/contracts) | OpenAPI 3.0.3: válido; 349 ejemplos verificados |
| `npm run check` (tools/contracts) | Tipos de web y móvil sincronizados |
| apps/web lint, typecheck, test | sin errores; 7 archivos, 13 pruebas pasadas |
| apps/mobile lint, typecheck, test | sin errores; 14 pruebas pasadas |

## SC-003 (rendimiento)
`StatsPerformanceTest` (etiqueta `performance`, excluida de `verify`): 1 000 estudiantes, 30 sesiones
cerradas (80 % de asistencia). Medido en un solo equipo local con Testcontainers/PostgreSQL 17:
`/activities/{id}/stats?size=100` 113 ms, `/stats/parallels` 71 ms, `/stats/careers` 69 ms
(umbral: 3 000 ms). Una sola ejecución; no es una medición de carga.

## Integración con 002
- Regla de inicio con GPS única en `SessionRepository.requireGpsConfigured`, usada por
  `SessionLifecycle` (manual) y `SessionCycle` (automático, que conserva `SISTEMA_INICIO_FALLIDO`).
- `EditActivity` combina `ciclo_automatico`/`umbral_asistencia` (003) con `acceso` inmutable (002).
- V015 y V018 ya cubren los valores de 002 (CREDENCIALES, INVITADO_LISTADO, REVISION, AUTORREGISTRO,
  MANUAL; limpieza de personas externas autorregistradas); `RetentionLifecycleTest` pasa.

## Notas
- `GET /me/stats` no está paginado (se deja así; el volumen es el de una sola persona).
- Las pruebas de `AttendanceScreen` en móvil fallaron una vez bajo carga de CPU simultánea con el
  backend y pasan en aislamiento y en la ejecución final.
