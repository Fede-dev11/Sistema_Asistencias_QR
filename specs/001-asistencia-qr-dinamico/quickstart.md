# Guía de validación: 001-asistencia-qr-dinamico

**Estado**: implementación y validación final. Preparación, base, US1–US4 y conservación están implementadas y
comprobadas con datos ficticios; la geolocalización automática permanece bloqueada hasta el
ensayo real. El gate constitucional de invitados sigue
pendiente de revisión del equipo; las pruebas técnicas no lo sustituyen.

## Componentes disponibles

La API Spring Boot, el cliente Next.js, la app Expo y PostgreSQL 17.7 están disponibles en
`services/api`, `apps/web`, `apps/mobile` e `infra`. El contrato OpenAPI tiene validación de
estructura, referencias, esquemas y 129 ejemplos, además de generación comprobable de tipos
para ambos clientes. Las marcas de [checklist](checklists/acceso-privacidad.md) pertenecen al
revisor y permanecen sin modificación automática.

## Preparación del entorno local

Requiere JDK 21, Maven 3.9.11 mediante el wrapper, Node 22.20.0, Docker/Compose y PostgreSQL
17. Las dependencias están fijadas en lockfiles. El perfil base arranca sin PostGIS; el perfil
geográfico está ensayado por separado mediante `OptionalPostgisTest`. Copiar `.env.example` mediante
`python3 infra/init-local-env.py`; `.env` contiene secretos locales y no se versiona.

Desde la raíz o los directorios indicados:

```bash
# Raíz: base de desarrollo
docker compose --env-file .env -f infra/docker-compose.yml up -d db
# services/api: API de desarrollo
python3 ../../infra/with-local-env.py -- env SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
# apps/web
 npm ci
 npm run dev
# apps/mobile
npm ci
npm start
```

La API carga semillas ficticias solo con perfil `dev` y recibe su contraseña por configuración
local. Configurar `EXPO_PUBLIC_API_URL` con un host accesible desde el dispositivo; `localhost`
del móvil no representa el equipo de desarrollo. Cámara y ubicación requieren HTTPS fuera de
las excepciones locales del navegador.

Para compilar e instalar el cliente nativo Android, disponer del SDK, licencias y un dispositivo
o emulador, exportar `ANDROID_HOME` y ejecutar `npm run android` desde `apps/mobile`.
Para iOS, usar `npm run ios` en macOS con Xcode. Expo genera los directorios `android/` e
`ios/` a partir de `app.config.ts`; esos artefactos están excluidos de Git y ESLint.
`npm run build` exporta los bundles y no sustituye estos recorridos nativos.
En equipos con poca RAM, usar la [compilación remota y teléfono por USB](../../docs/validation/android-recursos-limitados.md)
para evitar ejecutar Gradle y el emulador localmente.

## Datos semilla requeridos

- Dos carreras y al menos dos paralelos, director institucional y director de cada carrera.
- Dos docentes/encargados con actividades diferentes, una clase y un evento de prueba.
- Estudiante autorizado y otro no autorizado para la clase; cuentas activas e inactivas.
- Invitado precargado sin cuenta, cédula existente solo en otro evento, persona con cuenta
  incluida por error en el listado y persona ausente de todos los listados.
- Un evento cerrado y actividades con cierre de hace cinco años para ensayar retención.
- Invitados vinculados a dos actividades con vencimientos diferentes para comprobar aislamiento.

No se utilizan identidades reales como ejemplos. El listado se configura con JSON del
contrato, con cédulas de diez dígitos ficticias y nombres de prueba. Toda carga inválida o
con cédulas repetidas debe explicar el índice inválido sin aplicar una sustitución parcial.

## Recorrido mínimo con GPS apagado

1. Como responsable, consultar su actividad y configurar invitados en el evento con una
   tolerancia de prueba, por ejemplo 120 segundos; ese valor **no** es un valor predeterminado.
2. Cargar el listado de invitados. Confirmar que el acceso con rol de director no permite
   modificarlo y que conocer otro activityId no concede acceso fuera del ámbito.
3. Iniciar una sesión. Observar rotación de QR cada 30 s y vigencia ordinaria de 40 s.
4. Como estudiante autorizado, escanear y registrar sin solicitar ubicación. Un estudiante
   ajeno a la clase es rechazado y no puede eludirlo seleccionando invitado.
5. Como invitado, abrir mediante QR vigente y enviar solo cédula. El resultado de éxito no
   devuelve nombres del listado; la asistencia autorizada utiliza los datos precargados.
6. Introducir cédula con cuenta, incluso inactiva: USAR_CREDENCIALES y ninguna asistencia
   por vía invitada. Cédula fuera del listado del evento: NO_AUTORIZADO.
7. Consultar asistencias como responsable, director institucional y director de carrera;
   comprobar filtros y acceso directo a actividades de otra carrera. Exportar solo responsable.
8. Cerrar sesión y luego actividad definitivamente. No se aceptan nuevos registros y se fija
   la fecha de anonimización a cinco años calendario del cierre definitivo de actividad.

## Matriz de validación para implementación

| Caso | Preparación o límite | Evidencia esperada | Origen |
|------|----------------------|--------------------|--------|
| QR ordinario | Recepción a 39 s y a 40 s desde emisión | A 39 s vigente; a 40 s rechazado, sin usar hora de escaneo del cliente | FR-002, SC-004 |
| Exclusividad del plazo | Cuenta intenta usar un inicio invitado todavía vigente | Se exige credencial; no se amplían sus 40 s ordinarios | FR-019, FR-020, SC-009 |
| Plazo invitado | Inicio con T=120; enviar antes y al cumplirse inicio+T | Antes puede completar si autorizado; al límite FORMULARIO_EXPIRADO | FR-020, SC-010 |
| QR posterior a apertura | QR expira/cambia tras apertura válida y antes del plazo T | No invalida el inicio ya abierto ni alarga el QR compartido | FR-020, SC-010 |
| QR vencido al abrir | Abrir formulario con QR ya vencido | No se concede inicio ni tolerancia de invitado | FR-005, SC-010 |
| Recarga/cambio de configuración | Recargar o cambiar T tras apertura | Se conserva vence_en original; cambiar hora del teléfono no altera el plazo | FR-020 |
| Carrera con cierre | Cierre compite con envío ordinario/invitado | Ningún envío que pierda el bloqueo frente al cierre crea asistencia | FR-011, FR-024 |
| Carrera de elegibilidad | Crear cuenta o retirar listado mientras se envía cédula | Revalidación dentro de transacción; no se confirma por una lectura antigua | FR-019, FR-020 |
| Duplicados | Cien ejecuciones independientes de cien solicitudes simultáneas | Exactamente una asistencia por persona/sesión en cada ejecución | FR-009, SC-003 |
| Reintento | Misma clave/payload; luego misma clave con otro payload | Sin nuevo registro; otra carga da 409, tras comprobar autorización vigente | FR-009 |
| Reutilizar inicio | Dos cédulas compiten por el mismo secreto de formulario | Un solo consumo y persona; no reutilizar para registrar otras personas | FR-020 |
| Sin GPS | Sesión OFF y permisos de ubicación denegados | Sin petición/almacenamiento de coordenadas y sin rechazo por ausencia | FR-015, SC-007 |
| GPS piloto | REVIEW_ONLY; medición ausente/imprecisa/antigua | Repetición/revisión, sin afirmar presencia ni rechazo automático sin evidencia | FR-007, FR-008 |
| GPS automático | Evidencia real verificada en aula/espacio abierto | Medición documentada de falsos positivos/negativos antes de habilitar AUTO | SC-006 |
| Revisión | Pendiente y decisión con razón; repetir la misma decisión | Auditoría responsable/fecha/razón/antes/nuevo; no duplicar asistencia | FR-012, FR-023, SC-012 |
| Revisión tras cierre | Aprobar un pendiente cuando la sesión ya cerró | No crear una asistencia nueva; rechazo de revisión no crea asistencia | FR-011 |
| Acceso por ámbito | Listados y acceso directo; dos carreras y roles distintos | Solo ámbito permitido; director no hereda exportación/modificación | FR-021, SC-011 |
| Retención | Cierre 2026-10-19, vencimiento 2031-10-19; reloj controlado en pruebas | Al vencimiento, 410 si el recurso aún existe; 404 una vez retirado. Nunca lectura nominal aunque la limpieza requiera reintento | FR-022, SC-013 |
| Anonimización | Dos eventos con distintos vencimientos; repetir lote | Retirar solo vínculos vencidos, no borrar cuenta vigente ni duplicar agregados | FR-025, SC-013 |
| Copias | Restaurar copia anterior al vencimiento en entorno aislado | Aplicar política antes de abrir servicio; no volver a exponer identidades | FR-025, SC-013 |
| Grupos pequeños | Agregados de pocos asistentes y totales deducibles | Agrupar/suprimir; no conservar hashes, autores ni IDs nominales de actividad | FR-025 |
| Rendimiento | Perfil de laboratorio R10, red/hardware y mezcla registrados | p95 servidor <2 s; recorrido estudiante <10 s en condiciones descritas | SC-001, SC-002 |
| Límites de peticiones | Superar presupuesto del ámbito definido | 429 y Retry-After sin revelar nombres ni conservar IP cruda como auditoría | FR-014 |

No se esperan resultados reales por escribir esta matriz. El reloj controlado pertenece a
pruebas de servidor; no debe publicarse un endpoint de producción que permita cambiarlo.

## Comprobaciones de entrega

API: `./mvnw verify` con pruebas unitarias, contrato e integración PostgreSQL; si falla
Docker/Testcontainers se declara la limitación, no se sustituye por una BD en memoria.
Web/móvil: scripts documentados `lint`, `typecheck` y `test`; build web y recorrido de UI.
Contrato: validador OpenAPI dedicado y pruebas de respuestas contra YAML en CI. Android e
iOS: permisos, red intermitente y secretos en almacenamiento adecuado. Registrar medidas,
hardware, carga y resultados; no presentar el perfil propuesto como capacidad ya comprobada.

Antes de usar datos reales se requiere tratar la desviación constitucional del invitado,
validación institucional aplicable y ensayo completo de backup/restauración/anonimización.
El desglose actualizado está en [tasks.md](tasks.md), con GPS opcional, invitados y conservación.
Revisar el plan y registrar la decisión real sobre su excepción antes de integrar el incremento.

## Evidencia de implementación

T001–T070 y T072–T079 están implementadas; T071 permanece pendiente porque exige dispositivos y espacios
reales. US1 conserva su recorrido Chromium con renovación real de QR
y cierre definitivo. US2 añadió contrato, elegibilidad de clase/evento, formulario invitado,
idempotencia, limpieza de secretos, CSP, cámara y recuperación web/móvil. La prueba SC-003
ejecutó 100 rondas independientes de 100 solicitudes simultáneas en PostgreSQL 17.7 y obtuvo
una sola asistencia por persona/sesión en cada ronda; la clase de concurrencia completó seis
pruebas en 57.13 s. Dos recorridos Chromium US2 aprobaron en 49.2 s: autorización de clase y
evento, invitado precargado, bloqueo de cuentas incluso inactivas, recarga sin nueva apertura,
QR vencido después de abrir, cierre y ausencia de ubicación con GPS OFF.

US3 incorpora perfil PostGIS y migración Flyway separados del esquema base, configuración previa
de sesión, política OFF/REVIEW_ONLY/AUTO, círculo de precisión, dos incertidumbres consecutivas y
revisión responsable inmutable con idempotencia, bloqueos y auditoría. La regresión completa pasó
47 pruebas API en PostgreSQL 17.7/PostGIS 3.5, incluidas seis pruebas concurrentes en 46.56 s y la
prueba arquitectónica sin ciclos. El perfil base siguió registrando sin extensión ni permisos GPS;
el perfil opcional calculó distancia y confirmó que no existen columnas de latitud/longitud en los
intentos. El protocolo real pendiente está en `docs/validation/geografia-campo.md`; sin sus tasas y
responsables verificados el servidor deriva `PILOTO_REVISION`, nunca `VALIDACION_AUTOMATICA`.

US4 añadió una proyección nominal de una fila por persona, paginación y filtros de estado,
exportación CSV neutralizada contra fórmulas y auditoría mínima. El responsable conserva escritura
y exportación; dirección institucional lee todas las actividades y dirección de carrera solo las
vinculadas. La regresión completa pasó 51 pruebas API. El recorrido Chromium US4 pasó en 4.8 s:
consulta/exportación/auditoría del responsable, lectura institucional sin botón de exportación y
403 para un responsable ajeno. La web de producción incluye `/reportes`.

Las pruebas web son doce y las móviles diez; lint, tipos y builds están correctos en ambos
clientes. La web solicita geolocalización únicamente después de `REPETIR`, la app móvil usa permiso
en primer plano y ninguno persiste coordenadas. Los bundles Android/iOS exportados todavía no
constituyen ensayos en dispositivo o simulador. PostgreSQL de desarrollo usa 5433 para no modificar
el servicio existente en 5432. La conservación se implementó mediante V008 y un procedimiento
transaccional común al programador y las restauraciones. Bloquea datos vencidos aunque falle el
lote, retira sus enlaces y preserva cuentas/actividades vigentes. Suprime celdas pequeñas y
complementarias; no mantiene hashes de personas. Un inventario sin verificar o una copia
recuperable impide `COMPLETADO`. El ensayo aislado incluyó backup previo al vencimiento, segmento
WAL real catalogado, exportación, fallo de copia y preservación de datos vigentes. No sustituye un
ensayo PITR/snapshots de infraestructura institucional ni acredita aprobación de la excepción.

Validación final de fase 7: sesenta pruebas API, sin fallos/errores/omitidas, incluidas cinco de
conservación y privacidad con límites efectivos; OpenAPI válido con 129 ejemplos y tipos sin
diferencias. Se reprodujeron instalaciones `npm ci`, lint, tipos, pruebas y builds desde una
exportación de fuentes sin artefactos ni `.env`. También pasó de nuevo el restore sin red.
Los cinco recorridos Chromium pasaron sobre web de producción y JAR empaquetado en 1,4 min,
incluidas renovación del QR, recuperación de sesión activa tras autenticación y cierre local.
El [banco de rendimiento](../../docs/validation/rendimiento.md) ejecutó 2.000 intentos en 600 s,
sin errores y con p95 HTTP de 487,77 ms. Escaneo real y pruebas nativas continúan pendientes.

El [registro de revisión](../../docs/validation/revision-incremento.md) diferencia resultados
técnicos, pendientes de dispositivo/campo, ejecución remota de CI y aprobación de equipo.
