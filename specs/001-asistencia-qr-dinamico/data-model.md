# Modelo de datos: 001-asistencia-qr-dinamico

**Estado**: borrador de Fase 1 condicionado a revisión de la desviación constitucional de
invitados. [spec.md](spec.md) define reglas; [research.md](research.md) explica decisiones.
Modelo lógico relacional para PostgreSQL, sin entidades JPA como contrato público.
Tiempos `timestamptz` en UTC; IDs UUID salvo códigos de configuración. Las cédulas son texto,
nunca enteros, para conservar ceros iniciales. No se implementan migraciones durante el plan.

## Identidad y ámbito académico

| Entidad | Campos y relaciones | Invariantes |
|---------|---------------------|-------------|
| `personas` | `id`, `cedula_normalizada` única, nombre de referencia, tipo institucional/externa | Cédula funcional de diez dígitos; no acredita titularidad. No se exponen datos por consulta pública de cédula. |
| `cuentas` | `id`, `persona_id` único FK, proveedor, identificador de acceso único, hash de contraseña local nullable, estado | Persona puede existir sin cuenta. Cualquier cuenta existente, incluso inactiva, impide vía invitada. Credencial local solo en perfil de desarrollo. |
| `roles_cuenta` | cuenta FK, rol `DOCENTE`, `ENCARGADO`, `ESTUDIANTE`, `DIRECTOR_INSTITUCIONAL`, `DIRECTOR_CARRERA` | Poseer un rol no crea automáticamente vínculo de responsabilidad ni facultades de exportación. |
| `carreras` | `id`, código, nombre | Datos semilla en este incremento; no se presume sincronización académica real. |
| `paralelos` | `id`, carrera FK, código | Carrera del paralelo consistente con los vínculos de la actividad. |
| `direcciones_carrera` | cuenta FK, carrera FK, vigencia | Solo fuentes del servidor deciden qué carrera dirige una cuenta; petición no puede asignársela. |
| `sesiones_acceso` | cuenta FK, hash de secreto único, creada/vencimiento/revocación | Secreto opaco; no guardar valor original, no mezclar con inicio de invitado. |

La persona identifica la referencia canónica que permite deduplicar cuentas e invitados.
Los nombres del invitado usados en un evento proceden del listado precargado de ese evento,
no de campos suministrados por quien introduce la cédula. Datos sensibles con protección de
acceso y almacenamiento; no se incluyen en logs operativos ni en estadísticas vencidas.
Retención de una cuenta global con uso vigente se trata separadamente de la retención de una actividad.

## Actividades, autorización y sesiones

| Entidad | Campos y relaciones | Invariantes |
|---------|---------------------|-------------|
| `actividades` | `id`, modalidad `CLASE/EVENTO`, nombre, responsable cuenta FK, estado `ABIERTA/CERRADA`, `cerrada_en`, `anonimizar_en` | Cierre definitivo inmutable. `anonimizar_en` = cierre + cinco años calendario. Actividad cerrada no admite nuevas sesiones ni registros. |
| `actividad_carreras` | actividad FK, carrera FK, paralelos vinculados mediante tabla de relación | Eventos multicarrera requieren asociación explícita; pertenecer a una carrera no implica acceso a todas. |
| `estudiantes_autorizados` | actividad de clase FK, persona FK, vigencia | Un estudiante autenticado necesita vínculo vigente con la clase. No puede usar flujo invitado para eludirlo. |
| `configuracion_invitados` | actividad de evento FK única, `habilitado`, `tolerancia_invitado_segundos`, versión, `acceso_generacion` | Duración entera >40 requerida al habilitar. No es configuración del QR ni de usuarios con cuenta. |
| `invitados_evento` | actividad FK, persona FK, nombres precargados, activo, cargado por cuenta, fecha/versión | Único actividad/persona. Cargar lista no convierte una persona con cuenta en invitada. Reemplazo atómico y auditado. |
| `espacios` | `id`, nombre, tipo | Centro/radio solo en configuración geográfica opcional. Sin GPS no se exige dato espacial para registrar. |
| `sesiones` | `id`, actividad FK, espacio FK, estado `PROGRAMADA/ACTIVA/CERRADA`, inicio/fin de ventana, iniciada/cerrada en | Activar/cerrar solo responsable. No reabrir una sesión cerrada para revivir QR o formularios. |
| `configuracion_geografica` | sesión FK única, habilitada false por defecto, radio, precisión máxima, antigüedad máxima, modo `OFF/REVIEW_ONLY/AUTO`, evidencia FK opcional | Configurar antes de activar sesión; AUTO requiere evidencia de campo verificada en servidor. Valores nulos admitidos cuando OFF. |
| `evidencias_geograficas` | versión, referencia documental, fecha y responsable de validación | Referencia a ensayo de aula/espacio abierto y tasas medidas; nunca un booleano autoafirmado por el cliente. |

Si la actividad representa una clase o evento de una sola sesión, el responsable igualmente
realiza su cierre definitivo para iniciar conservación. Cuando hay varias sesiones, cerrar
una de ellas solo bloquea registros en esa sesión. No existe reapertura de actividad durante
este incremento. Cierre de actividad cierra todas sus sesiones dentro de la misma transacción.
Para aniversarios de 29 de febrero se usa 28 de febrero cuando corresponde, en UTC.

Autorización de lectura: responsable de la actividad, director institucional o director de
una carrera explícitamente vinculada. Exportar y modificar requieren responsabilidad; no
se conceden por ser director. Si existen varios roles se evalúa el permiso efectivo de cada
uno, sin convertir una lectura institucional en facultad de modificación.

## Secretos temporales y plazo invitado

| Entidad | Campos | Invariantes |
|---------|--------|-------------|
| `tokens_qr` | `id`, sesión FK, hash único, `emitido_en`, `expira_en` | Secreto aleatorio de 32 bytes; expira emisión +40 s, rotación cada 30 s. Sirve a muchos participantes; no se consume al primer uso. |
| `inicios_invitado` | `id`, sesión FK, hash de secreto único, `inicio_en`, `vence_en`, tolerancia copiada, estado `ABIERTO/CONSUMIDO`, persona/recibo consumido nullable, `persona_vinculada_id` nullable, generación de acceso copiada | Solo EVENTO con invitados habilitados y QR vigente al abrir. Tiempo fijado por servidor; nunca lo amplían recargas, cambios de configuración ni reloj local. Un inicio se consume para una persona. |
| `idempotencias` | operación, actor o inicio FK, clave UUID, huella HMAC de contenido canónico, resultado mínimo, creado/vencimiento | Único operación/actor/clave. Otra carga con misma clave da 409. No conservar el cuerpo original con cédula/coordenadas ni secretos. TTL técnico inicial 24 h y retirada al anonimizar antes si corresponde. |
| `contadores_limite` | clave de ámbito no reversible a IP, ventana, conteo, vencimiento | Efímeros y separados de auditoría; no guardar IP cruda como historial de cinco años. |

La caducidad se evalúa con `recibido_en` capturado por el servidor al entrar la solicitud;
el estado actual de sesión, cuenta y listado se vuelve a comprobar al adquirir los bloqueos.
El bloqueo del estado evita confirmar un registro cuya solicitud llegó antes pero perdió
la carrera frente al cierre. Cerrar/deshabilitar acceso impide nuevos envíos aunque su
`vence_en` original no haya llegado. Un reintento de operación anterior no crea una nueva asistencia.

La limpieza retira secretos vencidos y referencias ya innecesarias después de la ventana
operativa de reintento, no los guarda cinco años por defecto. Se conservan solo IDs de intento
sin secreto mientras el motivo y la auditoría de la actividad requieran ese registro.
El valor de inicio/QR nunca aparece en logs, listados, CSV ni estadísticas.
Para reanudar tras recarga, web conserva solo la capacidad opaca, tiempos y clave idempotente
en sessionStorage de esa pestaña, sin cédula ni sesión de cuenta. Se justifica exclusivamente
para recuperar el mismo inicio y se retira al vencer/consumirse; la recarga no crea otro inicio.
Un nuevo escaneo explícito válido sí puede abrir uno nuevo. Móvil usa almacenamiento temporal
seguro. Estas capacidades siguen expuestas al código autorizado del cliente y requieren
protección de la página frente a XSS; no se presentan como prueba de identidad.

## Asistencia, intentos y auditoría

| Entidad | Campos y relaciones | Invariantes |
|---------|---------------------|-------------|
| `intentos_asistencia` | `id`, sesión FK, persona FK cuando identificada, resultado `ACEPTADO/RECHAZADO/REPETIR/PENDIENTE_REVISION`, código/mensaje, canal, recibido en, secuencia técnica única, nombres de referencia nullable, distancia redondeada y precisión nullable | No guardar coordenadas exactas ni cédula desconocida introducida. Rechazos previos a identificar persona pueden tener persona nula. GPS OFF implica campos geográficos nulos. |
| `asistencias` | `id`, sesión FK, persona FK, intento FK, registrado en, forma `CREDENCIALES/INVITADO_LISTADO/REVISION`, nombres de registro obligatorios (1..200 caracteres) | Restricción única sesión/persona; única asistencia confirmada aunque dos solicitudes o vías compitan. La forma de invitado no se etiqueta como identidad acreditada. |
| `revisiones` | `id`, intento FK, revisor cuenta FK, decisión, razón, fecha | Decisión inmutable y no vacía. Aprobar creando asistencia solo con sesión activa; repetir petición no crea otra decisión/asistencia. |
| `auditoria_actividad` | `id`, actividad FK, actor cuenta FK, acción, instante, correlación, razón cuando proceda, diferencias mínimas anterior/nuevo | Incluye configuración, listado, cierres, exportación y revisión. No copiar secretos, listados enteros, contraseñas ni ubicación cruda. Solo lectores dentro del ámbito de esa actividad. |

Las auditorías son append-only durante su conservación nominal. El proceso de anonimización
es una operación de ciclo de vida autorizada y elimina ese detalle al vencer: append-only
no implica conservar responsables y textos identificativos indefinidamente.

Con GPS AUTO, precisión/antigüedad inválida o círculo incierto produce repetición y, tras
dos resultados consecutivos de incertidumbre de la misma persona/sesión, revisión. Sin
medición también se ofrece repetición/revisión. En REVIEW_ONLY las mediciones se someten a
revisión y no originan rechazo automático por proximidad sin evidencia. No se almacenan
coordenadas crudas en intentos ni asistencias; el adaptador puede procesarlas transitoriamente.

## Transacciones y carreras

Orden de bloqueo para los casos de asistencia: actividad compartida → sesión exclusiva →
persona exclusiva → inicio de invitado exclusivo. El trabajo dentro de la transacción debe
ser corto, sin llamadas CAS o a otros servicios. Cerrar actividad usa bloqueo exclusivo de
actividad y luego sesiones en orden determinista. Cerrar sesión usa actividad compartida y
sesión exclusiva. Sustituir listado usa actividad exclusiva; alta/vinculación de cuenta usa
la misma persona exclusiva, sin adquirir luego bloqueos en orden inverso.

El caso invitado revalida listado, ausencia de cuenta, plazo y sesión; crea asistencia,
consume inicio y guarda resultado idempotente en una sola transacción. La restricción de
unicidad resuelve solicitudes concurrentes incluso con dos inicios. Un duplicado devuelve
la asistencia existente solo si el solicitante sigue autorizado y el detalle no ha vencido.
No se interpreta el código de unicidad como motivo para seguir trabajando en una transacción abortada.

## Retención y estadísticas

| Entidad | Campos | Invariantes |
|---------|--------|-------------|
| `tratamientos_conservacion` | UUID aleatorio de lote, actividad FK solo mientras existe la fuente, versión política, vencimiento, fase `PENDIENTE/PROCESANDO/COPIAS_PENDIENTES/COMPLETADO`; contadores/alertas agregados en `RetentionMonitoring` | Unicidad actividad/versión mientras existe la fuente; al completar se retiran la FK, vencimiento y fecha de proceso. Lote idempotente, no se publica como estadística ni enlaza participantes. |
| `estadisticas_anonimas` | año UTC, modalidad, conteo de personas distintas y versión del proceso | Sin persona/cuenta/actividad nominal, cédulas, hashes, nombres ni horas individuales. Mínimo diez personas y dos actividades en año completamente vencido; celda sellada sin actualizaciones, sin totales complementarios ni endpoint público. Lotes insuficientes/posteriores se suprimen. No admite consulta para recuperar identidades. |
| `catalogo_copias` | soporte, fecha, alcance temporal, estado de saneamiento y destrucción | Inventario operativo restringido; las copias recuperables pendientes impiden declarar tratamiento completo. |

A los cinco años de cierre se bloquea acceso nominal antes de cualquier lectura o exportación,
no solo en un trabajo nocturno. Se forma agregado seguro y se retira detalle de la actividad:
listado, asistencias, intentos, auditoría, resultados idempotentes y cualquier enlace que
revele personas o autores. Los vínculos de otra actividad vigente no se tocan; el individuo
invitado huérfano se retira si ya no existe propósito vigente. Una cuenta global activa
no se elimina automáticamente por vencer una actividad.
También se eliminan/sanean encabezados de actividad y sesiones, nombres libres, responsable,
espacio y relaciones de autorización de clase y carrera de esa actividad. El control de lote
completado retiene solo operación, versión y conteos técnicos no identificativos, sin FK a
actividad nominal ni correspondencia pública con el agregado. No se conserva la actividad
identificada para sostener una respuesta HTTP: antes de retirarla el bloqueo de vencimiento
puede responder 410; después de retirarla puede responder 404 sin exponer ningún dato nominal.

Las estadísticas no se conservan como tabla de individuos con nombres borrados. Si guardar
un grupo por actividad permite reconocer una clase pequeña se combina por trimestre/modalidad
o se suprime. El umbral técnico inicial de diez participantes requiere evaluación y supresión
complementaria; no constituye garantía matemática de anonimato ni política institucional.
El lote operativo de control no se vincula públicamente con el agregado estadístico.

Se retiran y reemplazan respaldos/WAL/exportaciones recuperables afectados; una restauración
se hace aislada y aplica vencimientos antes de abrir servicio. Los fallos mantienen acceso
bloqueado, estado pendiente y alerta. El procedimiento completo de copias se ensaya antes
de datos reales; eliminar una fila no prueba destrucción física del dato en todas las copias.

## Índices iniciales y motivos

- Único cédula normalizada y único cuenta/persona: búsqueda de elegibilidad y exclusión de vía invitada.
- Único clase/persona y evento/persona: autorización de registros y reemplazo del listado.
- Único sesión/persona en asistencia: regla de negocio y concurrencia.
- Únicos hash de QR/inicio/sesión autenticada: localización de secreto sin persistir su valor.
- Actividades por responsable, relación carrera/actividad, sesiones por actividad: consultas por ámbito.
- Intentos y auditoría por actividad/sesión e instante: paginación por sesión y revisión.
- Retención por vencimiento/estado: lotes pendientes de anonimización.

No se añade GiST automáticamente: una comprobación contra una sola zona localizada por su
sesión no justifica índice espacial por sí misma. Cada migración documentará su consulta
objetivo y el plan de ejecución cuando haya datos suficientes.

## Motivos de resultado

`OK`, `YA_REGISTRADO`, `TOKEN_INVALIDO`, `TOKEN_VENCIDO`, `SESION_INACTIVA`,
`ACTIVIDAD_CERRADA`, `USAR_CREDENCIALES`, `NO_AUTORIZADO`, `FORMULARIO_EXPIRADO`,
`FUERA_DE_ZONA`, `PRECISION_INSUFICIENTE`, `UBICACION_ANTIGUA`, `UBICACION_REQUERIDA`,
`EN_REVISION`, `DATOS_ANONIMIZADOS`, `IDEMPOTENCIA_CONFLICTO`, `LIMITE_EXCEDIDO`.
Los estados, cuerpos y códigos HTTP exactos se rigen por [openapi.yaml](contracts/openapi.yaml).
No se devuelven nombres o cédulas desde el flujo público de invitado.

## Detalles concretados al registrar asistencia

Los nombres se toman del listado de ese evento o de la identidad autenticada y se conservan
como instantánea nominal en intento/asistencia; reemplazar luego un listado no cambia el
nombre histórico de una asistencia confirmada. Nunca se aceptan nombres del formulario
público. Esas instantáneas también se retiran al anonimizar. La secuencia técnica única
del intento establece el orden cuando varias recepciones tienen igual timestamp.

Una primera entrada elegible vincula el inicio a una persona, incluso si su medición
requiere repetición o revisión; no permite usar el mismo inicio para otra persona.
Deshabilitar invitados incrementa una generación de acceso e invalida los inicios
anteriores aunque se habilite después; cambiar únicamente T conserva su límite original.
Las claves idempotentes guardan una FK de cuenta o inicio, según el actor.
El reloj de producción y las recepciones se normalizan a microsegundos UTC para coincidir
con PostgreSQL y no desplazar los límites por redondeo.
