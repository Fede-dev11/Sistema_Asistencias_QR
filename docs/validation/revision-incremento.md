# Revisión del incremento 001

**Fecha:** 2026-10-20. **Estado:** implementación técnica validada con datos ficticios;
integración y uso real sujetos a las revisiones pendientes de este documento.

## Resultado y reproducción

Se implementaron las dieciocho operaciones del contrato común, web y móvil, con ocho
migraciones base y una geográfica opcional. Se ejecutaron validaciones desde una copia
de fuentes sin `.env`, `.git`, `node_modules`, `.next`, `target` ni bundles previos; se
instalaron los lockfiles con `npm ci`, se compiló Java desde cero y se ejecutaron suites.
Esta es una exportación de la copia de trabajo; no se presenta como un commit integrado.

Resultados: **60 pruebas API**, **12 web** y **10 móviles**, sin fallos; lint, tipos y builds
web/Android/iOS correctos. Contrato válido, 129 ejemplos comprobados y tipos sincronizados.
Restore sin red aprobado. El banco ejecutó 2.000 solicitudes en diez minutos sin errores,
con p95 HTTP de 487,77 ms. Los **cinco recorridos Chromium** pasaron sobre JAR y web de
producción construidos desde las fuentes limpias (1,4 min): clases/invitados OFF, plazo tras QR
vencido y cierre, recuperación de QR/logout, reportes por rol y ciclo completo del responsable.
La prueba de renovación compara el token en memoria y la imagen por separado, con margen para
el intervalo de consulta de cinco segundos, y no imprime el token ni el PNG del QR en fallos.

Comandos reproducidos:

```bash
# services/api, Java 21
./mvnw -B clean spotless:check verify
# ambos clientes, Node 22.20.0
npm ci
npm run lint
npm run typecheck
npm test
npm run build
# tools/contracts
npm ci
npm run validate
npm run check
# raíz
bash infra/tests/retention-restore.sh
```

Las pruebas de límites usan una ventana propia y multiplicador efectivo uno. Las suites
funcionales usan multiplicador de laboratorio para permitir autenticaciones repetidas;
no modifica la política por defecto del servicio. Las versiones concretas están en
manifiestos, wrapper y lockfiles. Los componentes móviles exportados no equivalen a
ejecutar Android o iOS.

## Trazabilidad funcional

| Requisito | Implementación y evidencia principal |
|---|---|
| FR-001 | SessionLifecycle; SessionContractTest y recorrido responsable |
| FR-002 | IssueSessionQr; límites 39/40 s en QrLifecycleTest |
| FR-003 | Tokens opacos/hash, fragmento retirado; attendance-flows.spec.ts |
| FR-004 | ActivityAuthorization/FindVisibleActivities; matrices de ámbito |
| FR-005 | AttendanceRecording y dos flujos; AttendanceContractTest/LocationDisabledTest |
| FR-006 | Elegibilidad/revalidación; ParticipantEligibilityTest y GuestDeadlineTest |
| FR-007 | GeographicPolicy/PostGIS; OptionalPostgisTest; ensayo real pendiente |
| FR-008 | Dos incertidumbres, precisión/edad y revisión; GeographicPolicyTest |
| FR-009 | Unicidad y locks; AttendanceConcurrencyTest (100×100) y revisión concurrente |
| FR-010 | Intentos mínimos y resultado/motivo; AttendanceContractTest/ReportingContractTest |
| FR-011 | Cierres con locks y veto a nuevas aprobaciones; concurrencia y revisión |
| FR-012 | ResolveAttendanceReview, revisión inmutable y auditada; ReviewConcurrencyTest |
| FR-013 | Proyección paginada/CSV por ámbito; ReportingContractTest y recorrido reportes |
| FR-014 | HMAC efímero, 429/Retry-After y alertas; PrivacyAndLimitsTest |
| FR-015 | OFF sin extensión/permisos; LocationDisabledTest y pruebas de clientes |
| FR-016 | Estudiante autenticado y autorizado; ParticipantEligibilityTest |
| FR-017 | Modalidad clase/evento explícita; escenarios API y navegador |
| FR-018 | Invitados precargados y solo cédula; GuestConfigurationContractTest y recorrido |
| FR-019 | Cuenta activa/inactiva exige credenciales; matriz de elegibilidad y carreras |
| FR-020 | T entero >40 exclusivo del formulario; GuestDeadlineTest y recarga real |
| FR-021 | Responsabilidad/carrera en servidor; ReportingAuthorizationTest, lectura y comandos |
| FR-022 | Aniversario calendario/29 de febrero, guardia independiente; RetentionPolicyTest |
| FR-023 | Ocho acciones administrativas, actor/fecha/razón/antes/nuevo; AdministrativeAuditTest |
| FR-024 | Cierre definitivo inmutable y sin reapertura; ActivityClosureTest |
| FR-025 | V008, eliminación nominal, supresión anual, catálogo y restore; RetentionLifecycleTest y ensayo aislado |

## Criterios de éxito

| Criterio | Resultado y límite |
|---|---|
| SC-001 | Tiempo real escaneo→resultado pendiente en dispositivo y buena señal |
| SC-002 | p95 HTTP local 487,77 ms en 2.000 intentos/10 min; no capacidad institucional |
| SC-003 | Cien rondas independientes de cien solicitudes concurrentes; una asistencia por ronda |
| SC-004 | 39/40 s y QR vencido después de abrir el formulario, probados |
| SC-005 | Estados/motivos y etiquetas accesibles probados; revisión TalkBack/VoiceOver pendiente |
| SC-006 | Reglas automáticas probadas con fixtures; ensayo real aula/exterior pendiente, AUTO bloqueado |
| SC-007 | OFF sin PostGIS/coordenadas; pruebas de permisos/almacenamiento, validación nativa pendiente |
| SC-008 | Clase autorizada/ajena y evento autenticado probados |
| SC-009 | Cuenta incluso inactiva bloquea invitado; nombres solo del listado |
| SC-010 | Plazo servidor inmutable, recarga, reintento y QR posterior probados |
| SC-011 | Ámbitos con dos carreras, evento multicarrera/sin vínculo y multirrol probados |
| SC-012 | Auditoría y revisión concurrente inmutables; ocho acciones verificadas |
| SC-013 | Retención, rollback, reintento, supresión y restore sin red probados; operación institucional pendiente |

## Contrato implementado

| Operación | Ruta | Pruebas |
|---|---|---|
| login | POST /auth/login | AuthContractTest |
| logout | POST /auth/logout | AuthContractTest |
| listActivities | GET /activities | ActivityScopeTest |
| getActivity | GET /activities/{activityId} | ActivityScopeTest/ReportingAuthorizationTest |
| closeActivity | POST /activities/{activityId}/close | ActivityClosureTest |
| setGuestSettings | PUT /activities/{activityId}/guest-settings | GuestConfigurationContractTest |
| replaceGuestList | PUT /activities/{activityId}/guest-list | GuestConfigurationContractTest |
| startSession | POST /sessions/{sessionId}/start | SessionContractTest |
| closeSession | POST /sessions/{sessionId}/close | SessionContractTest |
| getSessionQr | GET /sessions/{sessionId}/qr | QrLifecycleTest |
| setSessionGeolocation | PUT /sessions/{sessionId}/geolocation | GeographyReviewContractTest/OptionalPostgisTest |
| recordAuthenticatedAttendance | POST /attendance/attempts | AttendanceContractTest |
| startGuestRegistration | POST /guest-registrations | GuestDeadlineTest |
| submitGuestAttendance | POST /guest-registrations/submit | AttendanceContractTest/GuestDeadlineTest |
| listAttendances | GET /sessions/{sessionId}/attendances | ReportingContractTest |
| exportAttendances | GET /sessions/{sessionId}/attendances/export | ReportingContractTest |
| reviewAttempt | POST /attempts/{attemptId}/review | ReviewConcurrencyTest |
| listActivityAudit | GET /activities/{activityId}/audit | AdministrativeAuditTest |

El YAML validado incluye 129 ejemplos. La generación de tipos se verifica sin diferencias
en web y móvil. La cobertura indicada describe escenarios ejecutados; no afirma verificación
exhaustiva de todas las combinaciones posibles de cuerpos y respuestas.

## Pendientes para integración y uso real

- T071: ensayo real de un aula y un espacio abierto con tasas y responsables verificables.
- T088: el banco HTTP está ejecutado; falta escaneo real y buena señal para SC-001.
- T089: dispositivos/simuladores Android e iOS, cámara y accesibilidad nativa.
- T091: workflow ampliado y comprobaciones locales equivalentes ejecutadas; falta la ejecución
  remota de GitHub Actions después de incorporar el incremento. No se publica un estado verde ficticio.
- T093: revisión por otro integrante, excepción de identidad ADR 0002 y validación institucional
  de datos reales/copias. Solicitar esa revisión con este documento antes de integrar.

El proveedor DEV y las semillas no son CAS ni sincronización académica. No se cargó evidencia
geográfica real ni se atribuyó aprobación institucional. El ensayo de copias no ejecutó una
cadena PITR ni certifica discos/snapshots/réplicas fuera de los contenedores ficticios.

`npm ci` informó cinco avisos altos en web (cadena de lint/braces) y 58 en el árbol móvil
(12 moderados, 46 altos). Deben revisarse antes del despliegue real. No se aplicó el downgrade
incompatible que propone `npm audit fix --force`; instalar y compilar no demuestra ausencia
de vulnerabilidades. La revisión de dependencias debe preservar las matrices Next/Expo fijadas.

## Aporte de IA y revisión humana

Codex generó y corrigió implementación, migraciones, pruebas, scripts operativos y documentación
a partir de spec/plan/tasks y las decisiones del usuario. Ejecutó las comprobaciones técnicas
descritas. La IA no realizó trabajo de campo, pruebas nativas ni aprobaciones de equipo.
La checklist de calidad de requisitos y la constitución permanecen sin marcar/modificar por
automatismo. Responsable humano de revisión y fecha: **PENDIENTES**.
