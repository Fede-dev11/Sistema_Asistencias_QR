# Investigación y decisiones: 001-asistencia-qr-dinamico

**Fecha**: 2026-10-19. **Estado**: propuesta técnica para revisión; no aprueba la excepción
constitucional de invitados. Decisiones de producto: [spec.md](spec.md). Registro formal:
[ADR de arquitectura](../../docs/adr/0001-arquitectura-y-contrato.md),
[desviación de invitados](../../docs/adr/0002-identificacion-invitados.md) y
[anonimización](../../docs/adr/0003-conservacion-y-anonimizacion.md).

## R1. Stack y versiones

**Decisión técnica propuesta**: Java 21, Spring Boot 4.1.x, Spring MVC, Spring Security y
Spring Data JPA, Maven Wrapper; PostgreSQL 17.x y Flyway. Web Next.js 16.x, React 19.2 y
TypeScript estricto; móvil Expo SDK 57 con React Native 0.86 y React 19.2.3 según su matriz.
Node 22 LTS, parche al menos 22.13, para ambos clientes. JUnit Jupiter 6 y Testcontainers 2
administrados por el BOM de Boot; no fijar versiones independientes que contradigan ese BOM.
Al implementar se fijarán parches exactos y archivos de bloqueo; estas familias no son un lockfile.

**Justificación**: es el stack preliminar de la constitución con familias compatibles
verificadas en documentación oficial el 2026-10-19. Spring Boot 3.5 terminó soporte OSS en
junio de 2026; no se adopta para un proyecto nuevo sin soporte comercial. Las versiones
se consideran propuestas técnicas, no cambios constitucionales aprobados.

**Alternativas**: Laravel no está en el stack preliminar vigente; Boot 3.5 implicaría resolver
su soporte. No se introduce Kubernetes, Redis, colas ni microservicios para el piloto.

**Fuentes**: [Boot](https://docs.spring.io/spring-boot/system-requirements.html),
[fin OSS 3.5](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/),
[BOM](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html),
[PostgreSQL](https://www.postgresql.org/support/versioning/),
[Next.js 16](https://nextjs.org/blog/next-16), [Expo](https://docs.expo.dev/versions/latest/).

## R2. Arquitectura, contrato y errores

**Decisión**: monolito modular con dominios identidades, actividades, sesiones, QR,
asistencia, reportes y conservación. Cada dominio separa presentación, aplicación, dominio
y persistencia. Un mismo contrato REST `/api/v1` sirve a web y móvil. YAML OpenAPI 3.0.3
es la fuente de verdad; DTOs, paginación y Problem Details con `code` estable y mensaje.

**Justificación**: OpenAPI puede definirse antes de implementar el backend. El contrato
anterior solo para estudiantes se reemplaza por flujos ordinario e invitado separados.

**Alternativas**: documentación Markdown sola no cumple el contrato formal; no se crean
endpoints por cliente. springdoc 3.x puede contrastar la implementación Boot 4 con el YAML,
pero no sustituye la validación del contrato versionado.

**Fuentes**: [OpenAPI 3.0.3](https://spec.openapis.org/oas/v3.0.3),
[springdoc](https://springdoc.org/), [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457).

## R3. Cuenta, persona y proveedor de credenciales

**Decisión**: persona separada de cuenta. Para desarrollar y demostrar este incremento se
usan cuentas locales ficticias y `PasswordEncoder` adaptativo mediante un puerto de proveedor
de identidad. CAS real queda fuera del adaptador local: no se inventa protocolo ni acceso
institucional. Sesión autenticada opaca, revocable y persistida como hash en PostgreSQL;
Bearer en el contrato, memoria en web y SecureStore en móvil. Sin credenciales en localStorage.
Web debe advertir la necesidad de iniciar sesión de nuevo al perder su sesión en memoria.

**Justificación**: el desarrollo no depende de un servicio institucional no disponible.
No se afirma que una cuenta semilla pruebe pertenencia real a UNL. Cualquier cuenta existente,
incluso inactiva, bloquea el acceso de esa cédula como invitado; se deriva a credenciales o
recuperación administrativa, nunca al formulario ampliado como alternativa.

**Alternativas**: JWT sin persistencia facilita validación distribuida pero complica revocación
inmediata sin necesidad demostrada. Cookies HttpOnly con CSRF serían otra opción válida;
esta propuesta utiliza Bearer explícito para compartir el mismo mecanismo entre clientes.

**Fuentes**: [proveedor DAO](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html),
[SecureStore](https://docs.expo.dev/versions/latest/sdk/securestore/),
[OWASP sesiones](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).

## R4. Invitados: alcance decidido y riesgo residual

**Decisión del usuario**: únicamente listado precargado por encargado y cédula sin cuenta.
No OTP, revisión presencial ni autorregistro. Validez funcional de la entrada: cédula como
texto de diez dígitos ASCII, conservando ceros iniciales, con espacios exteriores retirados,
y coincidencia exacta con el listado del evento. No se consulta Registro Civil ni se afirma
validación oficial de identidad. Otros documentos requieren una ampliación de alcance.
No se muestran nombres del listado en una consulta pública ni existe un endpoint de búsqueda
de cuentas; el formulario devuelve solo el resultado mínimo necesario.

**Justificación**: preservar el flujo reiteradamente elegido. El cotejo identifica una entrada
autorizada, no demuestra titularidad: una cédula ajena conocida puede usarse indebidamente.
La desviación de los principios V/VI se documenta como propuesta de excepción, sin aprobación
de otro integrante ni cambio a la constitución. El control constitucional sigue condicionado
para aprobación/integración, aunque el borrador concrete el diseño pedido por el usuario.

**Alternativas descartadas por el usuario**: verificación presencial y código personal adicional.
No se vuelven a introducir bajo otro nombre. Auditar, limitar peticiones y limitar listados
no convierten este flujo en prueba de identidad ni eliminan el riesgo.

## R5. Dos plazos independientes

**Decisión**: QR CSPRNG de 32 bytes base64url, hash SHA-256 persistido, emisión cada 30 s y
vencimiento a los 40 s. El QR contiene enlace con el secreto en el fragmento (`#qr=...`),
no en query ni ruta registrada por servidores; el cliente extrae el fragmento y envía el
secreto en el cuerpo de una petición sin logging de cuerpos sensibles. La página elimina
el fragmento del historial y usa política de referencia restrictiva.

Un QR vigente abre un inicio opaco de invitado con sesión, apertura y vencimiento del
servidor. `tolerancia_invitado_segundos` es requerida, entera y mayor que 40; no tiene valor
institucional por defecto de 2/5 minutos. Se valida contra capacidad del tipo numérico y
la ventana de sesión; el cierre prevalece siempre. El valor se copia al abrir: cambios
posteriores de configuración, recargas o reloj local no amplían ese inicio. El secreto de
inicio se consume para una sola persona y solo en el flujo de invitados. Una cuenta no puede
usarlo para ampliar su propio plazo. GPS solo se pide al confirmar si está habilitado.

**Alternativas**: alargar el QR compartido para todos contradice la decisión del usuario;
confiar en la hora de escaneo enviada por el cliente no permite comprobar vigencia.

**Reanudación**: en web, sessionStorage de la pestaña conserva únicamente la capacidad
invitada opaca, inicio/vencimiento y clave de idempotencia; nunca la cédula, los nombres ni
la sesión autenticada. Se justifica este almacenamiento limitado para recuperar el mismo
inicio al recargar sin volver a abrirlo. Se elimina al vencer o consumirse, con CSP y sin
scripts de terceros en esa página; no evita el riesgo XSS. La pantalla no vuelve a ejecutar
la apertura al recargar. Un nuevo escaneo explícito con QR vigente puede crear otro inicio,
sin prolongar el anterior. En móvil, la capacidad se conserva temporalmente en SecureStore.

**Fuente**: [OWASP sesiones y vencimiento](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).

## R6. Unicidad, cierre, revisión e idempotencia

**Decisión**: índice único `(sesion_id, persona_id)`, transacción y `Idempotency-Key`.
Registrar y cerrar obtienen bloqueos con orden actividad, sesión, persona e inicio invitado.
El registro toma bloqueo compartido de actividad y exclusivo de sesión; la confirmación
y el cierre se serializan. Cerrar actividad toma bloqueo exclusivo de actividad y cierra
sus sesiones. Altas de cuenta se coordinan bloqueando la misma persona; reemplazar listado
bloquea la actividad. Se vuelve a comprobar cuenta/listado dentro de la transacción.

Guardar una asistencia y consumir el inicio se confirma atómicamente. Conflicto de unicidad:
`ON CONFLICT DO NOTHING` y lectura del existente, no continuar una transacción abortada.
Una clave se vincula a operación, actor/inicio y huella del contenido; distinta carga con
la misma clave da conflicto. Se reevalúa autorización antes de devolver una respuesta guardada.
Reproducir un resultado anterior no crea un registro nuevo después del cierre.
Aprobar una revisión que crea asistencia requiere sesión activa; el rechazo de una revisión
no crea asistencia. Una revisión aprobada repetida conserva la decisión original.

**Justificación**: impedir carreras cierre/envío, cuenta/invitado y dos envíos del mismo inicio.
La serialización es una elección inicial medible; no se asegura su rendimiento sin carga.

**Fuentes**: [bloqueos PostgreSQL 17](https://www.postgresql.org/docs/17/explicit-locking.html),
[INSERT](https://www.postgresql.org/docs/17/sql-insert.html).

## R7. Ámbitos de consulta y carga del listado

**Decisión**: responsable de actividad consulta/exporta solo su ámbito; director institucional
consulta todos; director de carrera solo actividades vinculadas a su carrera, incluidos
sus paralelos. Directores no heredan exportación ni modificación. Eventos multicarrera
requieren vínculos explícitos con cada carrera autorizada; sin vínculo no se permite consulta
por ese rol. Las asignaciones se obtienen de servidor, no de filtros del cliente.

La carga de invitados será JSON conforme al contrato (cédula y nombres), reemplazo atómico,
con límite técnico inicial de 1000 entradas por solicitud. No es matrícula institucional ni
un nuevo formato de requisitos; puede revisarse en el contrato si el volumen exige lotes.
Duplicados o cédulas con nombres discordantes rechazan el lote completo con errores por
índice, sin publicar el listado. Al retirar entradas quedan inaccesibles para nuevos envíos;
las asistencias ya confirmadas se conservan y cualquier rectificación se audita.

**Alternativa**: autorizar solo por rol o filtrar después de devolver resultados expone datos
fuera del ámbito. Una carga parcialmente aplicada dificulta explicar quién está autorizado.

## R8. GPS opcional y evidencia

**Decisión**: interfaz de validación geográfica con adaptador PostGIS opcional. Migraciones
básicas y arranque sin GPS no requieren extensión PostGIS; configuración habilitada exige
capacidad disponible. El piloto puede enviar mediciones a revisión; el rechazo geográfico
automático exige evidencia real documentada. La distancia se calcula en metros con geography;
precisión y antigüedad son reglas separadas. Las propuestas históricas de radio 20 m,
precisión 30 m y antigüedad 60 s son datos de experimento, no garantías de aula.
Sin control geográfico no se aceptan ni se almacenan coordenadas; con control se conservan
solo resultado, distancia redondeada y precisión necesarios para auditoría. No se guardan
coordenadas crudas en asistencias ni logs. No se añade un índice espacial sin consulta que lo justifique.

**Alternativa**: instalar geolocalización obligatoria reproduce el alcance anterior. GPS no
acredita presencia dentro de un aula ni impide ubicaciones simuladas.

**Fuente**: [ST_DWithin](https://postgis.net/docs/manual-dev/en/ST_DWithin.html).

## R9. Cinco años y anonimización

**Decisión del usuario**: cinco años calendario desde `actividad.cerrada_en` y posterior
anonimización para conservar estadísticas. Cierre de sesión y cierre definitivo de actividad
son distintos cuando hay varias sesiones. El cierre definitivo es inmutable y no se reabre
para desplazar el vencimiento. Para un cierre el 29 de febrero, el aniversario de un año no
bisiesto se trata como 28 de febrero en UTC; no se convierten años a 1825 días fijos.

Al vencer se bloquea inmediatamente lectura/exportación identificada. Un trabajo idempotente
forma agregados por periodo amplio y modalidad; elimina detalle de asistentes, listados,
intentos, idempotencia, textos libres y enlaces personales de auditoría. No deja hashes de
cédula ni seudónimos estables. No retiene nombres de evento, aula, encargado o IDs de actividad
en estadísticas. Punto técnico inicial: celdas de al menos diez personas; agrupar o suprimir
celdas menores y totales que permitan deducirlas. El umbral no garantiza anonimato y exige
evaluación del conjunto antes de liberar estadísticas; si no es seguro no se publica el agregado.

Una cuenta global vigente o una participación de otro evento vigente no se elimina por
vencer este evento; sí sus vínculos vencidos. Se purgan personas invitadas huérfanas sin otro
propósito vigente. La anonimización tiene lote único por actividad y reemplazo por versión,
no suma repetida al reintentar. El tratamiento de toda copia es condición para declarar
el proceso finalizado, no solo el UPDATE de la base activa.
Se eliminan o sanean también el encabezado nominal de actividad/sesiones, su responsable,
nombre, espacio y autorizaciones. El control de lote finalizado no mantiene una FK hacia
esos encabezados ni un enlace a las estadísticas publicadas que reconstruya identidades.
Después de retirar el recurso nominal puede responder 404 en lugar de conservar datos
personales únicamente para seguir respondiendo 410.

**Respaldos**: elegir rotación/purga coordinada frente a claves por actividad. La operación
prepara un respaldo saneado y elimina los respaldos/WAL/exportaciones recuperables que
contengan el detalle vencido, coordinándolo con la disponibilidad requerida. Los restores
permanecen aislados y ejecutan la política vigente antes de habilitar acceso. Un catálogo
registra soportes y tratamientos pendientes sin datos personales. Si una copia todavía es
recuperable, el lote no se declara finalizado y se alerta; no se afirma irreversibilidad física
solo por borrar filas en PostgreSQL. El diseño de operación y su ensayo son requisito previo
a usar datos reales. Se evita criptografía por actividad por su complejidad no justificada
para el piloto; no se presenta una política de backups nominales indefinidos como anonimización.

**Alternativas**: hash de cédula conserva relaciones; conservar auditorías nominales contradice
anonimización. Destruir todas las cuentas globales afectaría datos vigentes de otras actividades.

**Fuentes**: [NIST SP 800-188](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-188.pdf),
[PostgreSQL VACUUM](https://www.postgresql.org/docs/current/routine-vacuuming.html),
[PostgreSQL PITR](https://www.postgresql.org/docs/current/continuous-archiving.html),
[OWASP logging](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html).

## R10. Rendimiento, límites y validación

**Decisión técnica de laboratorio**: perfil inicial reproducible de 200 intentos/minuto
sostenidos durante diez minutos, cincuenta personas y cinco sesiones, con mezcla documentada
de aceptados/rechazados y GPS apagado. Es una hipótesis de banco para SC-002, no capacidad
institucional aprobada. Medir también latencia escaneo-respuesta en Android, iOS y web con
red documentada para SC-001. SC-003 se interpreta como cien ejecuciones independientes de
cien solicitudes simultáneas para la misma persona/sesión, con exactamente una asistencia
por ejecución; cambiarlo requiere registrar explícitamente la variación del escenario.

Límites iniciales de laboratorio configurables: 30 intentos/minuto por persona/sesión;
300 aperturas de invitado/minuto por IP/sesión y seis envíos/minuto por inicio, con límite
adicional de 300/minuto por IP. Login: cinco/minuto por identidad y 300 por IP, para no bloquear
una clase completa detrás de una misma red. Respuesta 429 con Retry-After. Contadores efímeros
separados de auditoría, sin guardar IP cruda a largo plazo; despliegue inicial de una instancia.
Revisar cifras con mediciones reales antes del piloto; no introducir Redis preventivamente.

Pruebas: JUnit/MockMvc, Testcontainers PostgreSQL, pruebas contra YAML de contrato, clientes
con pruebas de componentes/estados y un recorrido web; Android e iOS al menos simulador.
Linter y tipos se ejecutan explícitamente, además del build. GPS automático deshabilitado
hasta completar ensayo de campo. No se declaran suites ni métricas ejecutadas durante planificación.

## R11. Límites de entrega

Las decisiones de diseño quedan concretadas en este borrador. La excepción de identidad
requiere revisión de otro integrante para integrar; CAS real, base legal institucional y
operación con datos reales son condiciones externas de puesta en uso, no capacidades fingidas
por las semillas. El desglose de `tasks.md` se actualiza mediante `$speckit-tasks` a partir
de este diseño; su generación no acredita aprobación de la excepción ni implementación.

## R12. Ajuste de dependencias en implementación

La prueba `ArchitectureDependencyTest` detectó ciclos reales. Se centralizan utilidades
transversales en `compartido`: errores, reloj/secretos, bloqueos, idempotencia y política de
conservación. No se añade infraestructura distribuida ni se duplican reglas. Los módulos
consumen ese kernel, que no importa entidades o servicios de los dominios.
