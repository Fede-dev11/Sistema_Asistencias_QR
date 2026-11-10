# Research: 002 gestión académica, espacios, eventos y dispositivo

**Fecha**: 2026-10-25. Base: stack y decisiones de [001 research](../001-asistencia-qr-dinamico/research.md)
(R1–R12). Aquí solo se registran decisiones nuevas; no se reabren las de 001.

## R1. Identificador de dispositivo en lugar de MAC

- **Decisión**: la app genera un UUID v4 en el primer arranque, lo guarda en el almacenamiento
  seguro del sistema y lo envía como `dispositivo_id` en el cuerpo de cada intento. El servidor
  guarda solo `HMAC-SHA256(clave, dispositivo_id)` con el servicio `OpaqueSecrets` existente.
  La unicidad es un índice único parcial `(sesion_id, dispositivo_hash)` en `asistencias`.
- **Motivo**: Android 10+ devuelve una MAC aleatoria por red y bloquea la real a apps sin
  privilegios de sistema; iOS 7+ devuelve un valor constante (`02:00:00:00:00:00`). Un
  identificador de instalación es estable mientras la app esté instalada y no requiere permisos.
- **Limitación declarada**: reinstalar o borrar datos genera otro identificador; un cliente
  modificado puede enviar valores arbitrarios. No es prevención absoluta del fraude (Principio VI).
- **Alternativas descartadas**: MAC (no accesible); `ANDROID_ID`/`identifierForVendor`
  (dependen de plataforma y también cambian; no aportan frente al UUID propio); atestación
  Play Integrity/App Attest (fuera de alcance, candidata a un incremento futuro).
- **Por qué en el cuerpo y no en cabecera**: entra en la huella de idempotencia de 001, así un
  reintento con otro dispositivo y la misma clave produce conflicto en lugar de reutilizar el recibo.

## R2. Concurrencia de la unicidad por dispositivo

- **Decisión**: el registro ya serializa por sesión con `AttendanceLocks.session` (001 R6).
  Dentro de esa sección se comprueba si existe una asistencia con el mismo hash y se rechaza con
  `DISPOSITIVO_YA_REGISTRADO`; el índice único es la última barrera. La aprobación de revisiones
  usa el mismo bloqueo, por lo que el escenario US6-6 queda cubierto sin código nuevo de bloqueo.
- **Intentos previos a 002**: filas sin hash (`NULL`) quedan fuera del índice parcial.

## R3. Método de verificación por sesión

- **Decisión**: columna `metodo_verificacion` en `actividades` (valor por defecto) y en `sesiones`
  (`QR_Y_GPS`, `SOLO_QR`, `SOLO_GPS`, `NINGUNO`). `configuracion_geografica` de 001 sigue siendo
  la fuente de la tolerancia; su `habilitada` pasa a derivarse del método (`*_GPS` ⇒ habilitada).
  Al iniciar la sesión se comprueba la coherencia y se congela (FR-021).
- **QR**: `AttendanceAttemptRequest.qr_token` pasa a opcional en el contrato; el caso de uso lo
  exige si el método incluye QR, y lo rechaza si no lo incluye (evita aceptar un token de otra sesión
  como señal). En métodos sin QR la sesión se identifica con `sesion_id`.
- **Inicio de invitado sin QR**: `POST /guest-registrations` acepta `qr_token` o `sesion_id`
  (exactamente uno); `sesion_id` solo vale si el método de la sesión no incluye QR.
- **Desviación**: `SOLO_GPS` y `NINGUNO` no usan QR temporal; queda registrada en Complexity
  Tracking y en el ADR 0004 propuesto.

## R4. Espacios y coordenadas sin depender de PostGIS

- **Decisión**: `espacios` gana en la migración base `codigo_normalizado` (único), `latitud`,
  `longitud` (numéricas, ambas o ninguna), `radio_defecto_m`, `precision_defecto_m`, `creado_por`.
  La migración geográfica opcional sustituye `centro` por una columna generada a partir de
  latitud/longitud y elimina `radio_recomendado_m` (sin usos en código).
- **Motivo**: el arranque base no tiene PostGIS (001 R8); las coordenadas deben poder guardarse
  igual. Una sola fuente de verdad evita que `centro` y lat/long diverjan.
- **Normalización del código**: mayúsculas, sin espacios ni guiones (`a-334` → `A334`).

## R5. Horario recurrente

- **Decisión**: tabla `horarios_clase` (días ISO 1–7, hora inicio/fin locales, rango de fechas,
  espacio). La generación usa `java.time` en `America/Guayaquil` (sin cambio de hora) y guarda
  `ventana_inicio/fin` en UTC como en 001. Límite 200 sesiones por horario.
- **Edición**: se borran y regeneran solo sesiones `PROGRAMADA`, futuras y sin intentos ni tokens.
- **Alternativa descartada**: reglas RRULE (iCalendar): más expresivas pero innecesarias para
  "días + horas + rango".

## R6. Lista autorizada desde paralelos

- **Decisión**: crear la clase inserta en `estudiantes_autorizados` los matriculados vigentes; quitar
  marca `vigente=false`; sincronizar inserta con `ON CONFLICT DO NOTHING`. Como la fila quitada sigue
  existiendo, la sincronización no la reincorpora (FR-010) sin columna extra.

## R7. Evento abierto y autorregistro

- **Decisión**: `actividades.acceso` (`CERRADO`/`ABIERTO`, solo eventos). El autorregistro reutiliza el
  inicio temporal de invitado de 001 (plazo `tolerancia_invitado_segundos`) y su envío; en eventos
  abiertos el envío acepta `nombres` y, si la persona externa no existe, la crea (`tipo=EXTERNA`) sin
  cuenta. Forma de asistencia nueva: `AUTORREGISTRO`. `nombres_registro` guarda lo escrito (US4-6).
- **Riesgo**: cualquiera puede registrar una cédula ajena; mitigado parcialmente por la unicidad por
  dispositivo y los límites de 001. Declarado como desviación V/VI.

## R8. Validación de cédula

- **Decisión**: algoritmo módulo 10 con provincia 01–24 o 30 y tercer dígito < 6, en una sola clase
  de dominio `Cedula` en `compartido.dominio`, usada por administración, importación, listado y
  autorregistro (Principio III: una regla, un lugar).
- **Impacto**: las cédulas ficticias de semillas y fixtures deben ser válidas; se ajustan en tareas.

## R9. Sección académica simulada e importación CSV

- **Decisión**: nuevo módulo `academico` (presentación/aplicación/persistencia) para CRUD y matrículas;
  las cuentas se siguen autenticando con `DevelopmentIdentityProvider` (puerto `IdentityProvider` de 001),
  que es lo único a sustituir por CAS. No se crea otro puerto (Principio III, sin segundo uso).
- **CSV**: `multipart/form-data`, UTF-8, separador `,` o `;` detectado en el encabezado, máx. 5 000 filas
  y 2 MB. Encabezado inválido ⇒ 400 sin cambios; filas inválidas ⇒ informe por fila, el resto se aplica
  en una transacción. Idempotente por cédula (personas), correo (cuentas) y par persona/paralelo.
  Se usa el parser de la librería estándar (lectura línea a línea con soporte de comillas); no se añade
  dependencia nueva.

## R10. Retiro del registro de asistencia en la web

- **Decisión**: la página `apps/web/src/app/asistencia` se elimina; la web no puede generar un
  identificador de instalación fiable. El backend rechaza intentos sin `dispositivo_id`, por lo que la
  regla no depende del cliente (Principio II).
