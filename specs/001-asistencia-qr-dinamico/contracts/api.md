# Contrato de API — borrador condicionado

**Estado:** propuesta de diseño, condicionada a revisión de la excepción constitucional del
flujo invitado por cédula y listado. No declara aprobado el control constitucional ni habilita
implementación. [openapi.yaml](./openapi.yaml), OpenAPI 3.0.3, es la fuente de verdad; este
documento explica sus decisiones. Las rutas se publican bajo `/api/v1` y ambos clientes
consumen los mismos casos de uso.

## Identidad y permisos

`POST /auth/login` devuelve un bearer opaco revocable con respaldo en PostgreSQL. La web
lo mantiene solo en memoria; móvil utiliza SecureStore. `POST /auth/logout` lo revoca.
Credenciales semilla ficticias solo en desarrollo; CAS queda aislado detrás del proveedor
de identidad y su protocolo permanece pendiente de confirmación institucional.

La cuenta y la persona son entidades separadas. Tener cuenta, incluso inactiva, impide la
vía invitada; no habilita saltarse las credenciales. No existe consulta pública de cuentas
o listados por cédula. HTTPS fuera del entorno local y `Cache-Control: no-store` para datos
personales y secretos; no registrar tokens, cédulas o coordenadas exactas en logs.

| Operación | Autorización comprobada en servidor |
|---|---|
| Consultar actividad, asistencias y auditoría | Responsable de su actividad; director institucional en todas; director de carrera en actividades vinculadas a su carrera y paralelos |
| Configurar, iniciar/cerrar, cargar invitados y revisar | Solo responsable vinculado a la actividad |
| Exportar CSV | Solo responsable; ser director no concede exportación |
| Registrar asistencia autenticada | Identidad de la cuenta y autorización vigente para clase o evento |
| Abrir/enviar formulario invitado | Capacidad temporal del formulario y controles del evento; sin bearer de cuenta |

Los roles de cuenta son `DOCENTE`, `ENCARGADO`, `ESTUDIANTE`, `DIRECTOR_INSTITUCIONAL` y
`DIRECTOR_CARRERA`. `RESPONSABLE` es un predicado: tener rol DOCENTE o ENCARGADO y ser
el responsable vinculado a la actividad, comprobado en servidor; no es un rol global que
conceda acceso suficiente por sí solo.

Las actividades incluyen `responsable_id`, `carrera_ids` y `paralelo_ids`; un evento puede
vincular varias carreras y paralelos. El filtro `carrera_id` sigue siendo singular.

Los roles de dirección solo conceden lectura en su ámbito. No se deduce autorización de
un `carrera_id` enviado por el cliente. La interfaz no declara prevención absoluta del fraude.

`GET /activities` permite descubrir las actividades visibles, con paginación y filtros opcionales
`modalidad` (`CLASE` o `EVENTO`) y `carrera_id`. Los filtros solo restringen el ámbito calculado
en servidor; solicitar una carrera fuera de ese ámbito devuelve `403`. Sin filtros se devuelven
solo actividades autorizadas. Se excluyen del listado las actividades con plazo nominal vencido;
la lectura individual de una de ellas devuelve `410` mientras sea reconocible; tras sanear
o eliminar sus encabezados nominales devuelve `404`. En ambos casos se bloquea la lectura nominal.

## QR y formulario de invitados

El QR rota cada **30 segundos** y cada token vale **40 segundos** desde su emisión. El
servidor utiliza la hora de recepción; cerrar la sesión o actividad impide nuevos registros,
aunque un token conserve tiempo. Los tokens no contienen datos personales. El cliente puede representar el QR como una URL
con fragmento `#qr=token`: lo lee, lo retira del historial y envía el token en el cuerpo al
backend. Nunca se incluye el QR en ruta o query, logs o telemetría; esta excepción para
el fragmento QR no permite incluir el secreto del formulario invitado en ninguna URL.

`PUT /activities/{activityId}/guest-settings` configura `habilitado` y
`tolerancia_invitado_segundos` (entero mínimo 41), exclusivamente para invitados sin cuenta
precargados. No modifica el QR ni el flujo autenticado. `PUT .../guest-list` reemplaza el
listado completo de forma atómica: cédulas como cadenas de diez dígitos y nombres precargados.
El límite inicial de 1000 entradas es técnico y fijo en este contrato, no una regla institucional.
Cambiarlo requiere actualizar el contrato y su versión; no puede alterarse solo por configuración
del despliegue mientras el esquema siga declarando `maxItems: 1000`.
Rechazar cédulas repetidas o filas inválidas sin aplicar un reemplazo parcial. La validación
por patrón no equivale a comprobar titularidad ni a una consulta a un registro oficial.

1. `POST /guest-registrations` recibe un QR ordinario vigente. Comprueba sesión activa,
   evento abierto y flujo invitado habilitado; devuelve `registro_invitado_token`, `inicio_en`
   y `vence_en`. Toma una instantánea de la tolerancia al validar la apertura en servidor.
2. `POST /guest-registrations/submit` recibe la capacidad opaca y la cédula. Dentro de la
   transacción vuelve a comprobar cuenta, listado y estado; únicamente usa datos precargados.
   `403 USAR_CREDENCIALES` si existe cuenta, `403 NO_AUTORIZADO` si no figura en el listado,
   `410 FORMULARIO_EXPIRADO` al vencer. No añade códigos personales, revisión presencial
   ni autorregistro.
3. La capacidad solo puede consumirse para una persona. Una confirmación o repetición devuelve
   un recibo mínimo sin nombres, cédula ni datos del listado. Cambiar la configuración no
   prolonga capacidades existentes; deshabilitar el flujo o cerrar invalida nuevos envíos.

Login y estos dos endpoints invitados declaran `security: []`; el envío invitado autentica
su capacidad en el cuerpo, y no queda libre de controles. El inicio tiene límite de solicitudes
y no requiere clave de idempotencia. El secreto del formulario no se transmite en URL.

Para reanudar el mismo formulario al recargar, se permite `sessionStorage` exclusivamente
para `registro_invitado_token`, `inicio_en`, `vence_en` y su clave de idempotencia. No guardar
cédula, nombres, otra información personal ni el bearer de cuenta. Se justifica por la
capacidad temporal limitada al formulario y el ámbito de pestaña: recuperar esos valores
no amplía el vencimiento. Limpiarlos al vencer o consumirse la capacidad. Recargar no vuelve
a ejecutar `POST /guest-registrations`. Un nuevo escaneo explícito de QR vigente sí puede
crear otro inicio, sin reiniciar ni prolongar el anterior; no requiere otro endpoint.

## GPS condicional

`PUT /sessions/{sessionId}/geolocation` solo se permite con sesión PROGRAMADA. Está
desactivado por defecto; entonces no se solicita permiso ni se conservan coordenadas, y se
rechaza ubicación innecesaria enviada al registro. Para habilitarlo se requieren radio,
precisión máxima, antigüedad máxima y capacidad del despliegue; si falta capacidad: `409`.
El servidor devuelve el modo efectivo: piloto/revisión o validación automática. Solo admite
rechazo automático cuando existe evidencia de campo verificada; una petición no puede
autodeclarar esa evidencia. Mediciones inciertas permiten repetir o revisión; el GPS no
garantiza presencia física ni detecta por sí mismo ubicación simulada.

## Idempotencia, cierre y resultados

Asistencia autenticada, envío invitado, revisión e inicio/cierre requieren `Idempotency-Key`
UUID. La misma identidad o capacidad, operación, clave y carga canónica recuperan el resultado
previo tras revalidar autenticación y ámbito. Otra carga con la misma clave devuelve `409`.
La respuesta guardada no permite leer datos nominales vencida su conservación ni crear un
nuevo registro después del cierre. Los PUT de configuración y listado son reemplazos naturales.

La unicidad persona/sesión es obligatoria en base de datos. Cierre y registro comparten un
protocolo de bloqueo transaccional sobre la sesión; los controles de cuenta/listado y consumo
del formulario no se resuelven fuera de esa transacción. Aprobar una revisión que crea asistencia
requiere sesión ACTIVA y actividad abierta; después del cierre devuelve `409`. Rechazar un pendiente
no crea asistencia y puede resolverse después del cierre mientras no venza la conservación.
Un duplicado autorizado devuelve el registro
existente. El contrato no impone carga institucional ni declara metas medidas.

| HTTP | Resultado |
|---|---|
| 201 | Nueva asistencia confirmada; también creación de inicio invitado |
| 200 | Consulta, cambio o asistencia existente/repetición autorizada |
| 202 | Medición a repetir o intento pendiente de revisión |
| 204 | Sesión autenticada revocada |
| 400 / 401 | Sintaxis inválida / autenticación inválida |
| 403 / 404 | Ámbito o elegibilidad insuficientes / recurso inexistente |
| 409 / 410 | Estado o clave incompatible / formulario vencido o dato nominal anonimizado |
| 422 / 429 | Rechazo de negocio / límite de solicitudes |

Los errores usan `application/problem+json`: `type`, `title`, `status`, `code`, `mensaje` y
opcionalmente `detail`, `instance`, `attempt_id`, `correlation_id`. No devolver siempre 200
para rechazos. Registrar motivos comprensibles; un intento rechazado puede aportar su ID.
Las listas se paginan con `page` y `size` (máximo técnico 100). La lista de asistencias
contiene una fila por persona identificada: su confirmación prevalece y, si no tiene
asistencia, se muestra el último intento identificado pendiente o rechazado. El filtro
`estado` se aplica después de seleccionar esa fila. `asistencia_id` puede ser null;
`attempt_id`, `code` y `mensaje` identifican el intento y explican el resultado. Nunca
se exponen intentos sin persona identificada ni cédulas desconocidas. Exportar CSV es una ruta
distinta y auditable; no otorga derechos a los directores ni expone tokens o ubicación exacta.

## Conservación y auditoría

`POST /activities/{activityId}/close` es el cierre definitivo: cierra sesiones, impide nuevas
sesiones y fija el inicio de los **cinco años calendario**. Cerrar solo una sesión no inicia
el plazo. La actividad devuelve `cerrada_en` y `anonimizar_en`. Cumplido el plazo, lecturas
nominales, auditoría nominal y exportaciones devuelven `410 DATOS_ANONIMIZADOS` mientras
el recurso vencido siga siendo reconocible, incluso si hay una copia física todavía pendiente
del trabajo de anonimización. Tras sanear o eliminar sus encabezados nominales pueden devolver
`404`; ambos resultados impiden lectura nominal. No se retienen responsable, carrera ni datos
personales solo para preservar la respuesta `410`. Actividades, sesiones y autorizaciones
se eliminan o sanean, y el control de retención completado ya no mantiene una FK a la fila nominal. Los agregados anónimos
se definirán fuera de estos endpoints individuales.

La auditoría identifica actor, acción, fecha, razón cuando corresponde y valores anterior/nuevo
pertinentes, incluyendo revisión manual, configuración, cambios de listado y exportaciones.
No copia listados completos ni secretos. El procedimiento operativo de respaldos y restauración
debe conservar la regla de anonimización; este contrato no inventa un plazo adicional de copias.

## Validación prevista

Validar YAML y referencias OpenAPI en CI; contrastar implementación con el contrato y probar
autorización positiva/negativa por ámbito, cuenta frente a invitado, límites 30/40 segundos,
vencimiento exclusivo del formulario, GPS apagado, cierre concurrente, unicidad, replay y
anonimización. Las comprobaciones de documentación no acreditan implementación ni pruebas
de carga realizadas.
