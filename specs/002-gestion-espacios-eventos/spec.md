# Especificación de funcionalidad: Gestión académica simulada, espacios, actividades, eventos abiertos y unicidad por dispositivo

**Rama de la funcionalidad**: `feature/002-gestion-espacios-eventos`

**Creado**: 2026-10-25

**Estado**: Borrador — decisiones funcionales aclaradas con el usuario el 2026-10-25. El
autorregistro con cédula en eventos abiertos y los métodos de verificación sin QR
(`SOLO_GPS`, `NINGUNO`) son desviaciones del Principio VI pendientes de revisión del equipo
antes de integrar. La constitución no se modifica ni se declara una excepción aprobada.

**Entrada**: Descripción del usuario: "El backend debe registrar asistencias de estudiantes
universitarios, invitados a eventos cerrados y eventos abiertos. Los estudiantes están en un
servicio externo de autenticación, pero para este desarrollo se simula una sección de
estudiantes y docentes. El docente o encargado crea clases (seleccionando estudiantes de los
paralelos asignados) o eventos, elige el aula o espacio y, si no existe, lo crea en ese
momento (número de bloque, latitud y longitud mediante mapa, nombre). En eventos cerrados
carga el listado de cédulas; en eventos abiertos no hay listado. Según el espacio se
configura más o menos tolerancia geográfica, y hay clases o eventos sin geolocalización.
En la app móvil cada dispositivo puede registrar como máximo una asistencia por sesión."

Continúa `001-asistencia-qr-dinamico` y cubre lo que esa especificación reservó para
`002-autenticacion-identidades` y `003-espacios-eventos`. No duplica las reglas de 001:
token QR, ventana de 40 s, invitados de evento cerrado, revisión, consulta, exportación,
auditoría y conservación se reutilizan tal como están definidas allí.

## Problema que resuelve

Hoy las personas, cuentas, paralelos, espacios, actividades y sesiones existen solo como
datos semilla: un docente no puede crear su clase ni su evento, ni registrar el aula donde
se dicta. Tampoco existen eventos abiertos al público, y un mismo teléfono puede registrar
la asistencia de varias personas en una sesión. Este incremento permite operar el sistema
de extremo a extremo sin intervención técnica y cierra la vía de "un teléfono marca por
todos".

## User Scenarios & Testing *(obligatorio)*

### Historia de usuario 1 — Administrador gestiona la sección académica simulada (Prioridad: P1)

Mientras no exista integración con el sistema académico y de autenticación institucional,
un administrador mantiene personas, cuentas, roles, carreras, paralelos, matrículas de
estudiantes en paralelos y asignaciones de docentes a paralelos. Puede hacerlo registro a
registro o importando archivos CSV de estudiantes, docentes y matrículas.

**Por qué esta prioridad**: sin estudiantes matriculados ni docentes asignados no se pueden
crear clases con su lista autorizada.

**Prueba independiente**: con una base sin datos académicos, importar un CSV de estudiantes,
uno de docentes y uno de matrículas, y verificar que los estudiantes inician sesión y
aparecen en sus paralelos.

**Escenarios de aceptación**:

1. **Dado** un administrador autenticado, **Cuando** crea una persona con cédula válida y
   nombres y le crea una cuenta con correo, contraseña y rol ESTUDIANTE, **Entonces** esa
   persona puede iniciar sesión con esas credenciales.
2. **Dado** una cédula ya registrada, **Cuando** se intenta crear otra persona con ella,
   **Entonces** se rechaza indicando que la cédula ya existe.
3. **Dado** una cédula con dígito verificador inválido, **Cuando** se intenta registrar,
   **Entonces** se rechaza con un mensaje comprensible.
4. **Dado** un CSV de matrículas con 100 filas válidas y 3 inválidas, **Cuando** el
   administrador lo importa, **Entonces** se aplican las filas válidas, se informa cada fila
   rechazada con su número y motivo, y reimportar el mismo archivo no crea duplicados.
5. **Dado** un docente asignado a un paralelo, **Cuando** consulta sus paralelos,
   **Entonces** ve ese paralelo con sus estudiantes matriculados.
6. **Dado** un usuario sin rol ADMINISTRADOR, **Cuando** intenta cualquier operación de
   administración, **Entonces** se rechaza.
7. **Dado** una cuenta desactivada por el administrador, **Cuando** intenta iniciar sesión,
   **Entonces** se rechaza; sus asistencias históricas se conservan.

---

### Historia de usuario 2 — Docente o encargado crea o elige el espacio (Prioridad: P1)

Al crear una clase o un evento, el responsable busca el espacio por código o nombre. Si no
existe, lo crea en ese momento: código de bloque/aula (ej. A334), nombre, tipo, ubicación
marcada en un mapa (latitud y longitud), radio por defecto y precisión máxima por defecto.
El espacio queda disponible para todos los responsables.

**Por qué esta prioridad**: cada sesión necesita un lugar, y la validación geográfica
necesita sus coordenadas.

**Prueba independiente**: crear un espacio con coordenadas, buscarlo por código desde otra
cuenta de docente y usarlo en una sesión con validación geográfica.

**Escenarios de aceptación**:

1. **Dado** un docente autenticado, **Cuando** crea el espacio "A334" con nombre, tipo AULA,
   coordenadas y radio 20 m, **Entonces** queda registrado y aparece al buscar "A334".
2. **Dado** un espacio con código "A334", **Cuando** otro responsable intenta crear otro con
   el mismo código (sin distinguir mayúsculas ni espacios), **Entonces** se rechaza y se le
   ofrece usar el existente.
3. **Dado** coordenadas fuera de rango (latitud fuera de ±90 o longitud fuera de ±180) o
   un radio no positivo, **Cuando** se envían, **Entonces** se rechazan con un mensaje claro.
4. **Dado** un espacio creado por otro docente, **Cuando** un docente que no es su creador ni
   administrador intenta editarlo, **Entonces** se rechaza.
5. **Dado** un espacio sin coordenadas, **Cuando** se usa en una sesión, **Entonces** la
   sesión solo admite métodos de verificación sin GPS.
6. **Dado** un espacio editado (por ejemplo, su radio por defecto), **Cuando** se consulta una
   sesión ya activa o cerrada, **Entonces** esa sesión conserva la configuración geográfica
   con la que fue iniciada.

---

### Historia de usuario 3 — Docente crea una clase con horario recurrente (Prioridad: P1)

El docente crea una clase: nombre, uno o varios paralelos que tiene asignados, espacio y
horario recurrente (días de la semana, hora de inicio y fin, fecha de inicio y fin). El
sistema genera la lista de estudiantes autorizados a partir de los matriculados en esos
paralelos y genera las sesiones programadas del horario. El docente puede quitar o añadir
estudiantes individualmente y crear sesiones sueltas adicionales.

**Por qué esta prioridad**: es el flujo diario principal del docente.

**Prueba independiente**: crear una clase de lunes y miércoles de 08:00 a 10:00 durante
cuatro semanas y verificar que existen ocho sesiones programadas y que solo los estudiantes
matriculados pueden registrar asistencia.

**Escenarios de aceptación**:

1. **Dado** un docente asignado a los paralelos A y B, **Cuando** crea una clase con ambos,
   **Entonces** la lista autorizada contiene a todos los estudiantes matriculados en A y B.
2. **Dado** un paralelo que no tiene asignado, **Cuando** el docente intenta usarlo en una
   clase, **Entonces** se rechaza.
3. **Dado** un horario lunes y miércoles 08:00–10:00 del 2 al 27 de noviembre, **Cuando** se
   crea la clase, **Entonces** se generan las sesiones programadas de esas fechas en la
   zona horaria de la institución, cada una con el espacio de la clase.
4. **Dado** una clase creada, **Cuando** el docente quita a un estudiante o añade a otro
   estudiante existente, **Entonces** la lista autorizada se actualiza, la acción queda
   auditada y las asistencias ya registradas no cambian.
5. **Dado** un estudiante matriculado en el paralelo después de crear la clase, **Cuando**
   el docente sincroniza la lista con los paralelos, **Entonces** el estudiante se añade sin
   reincorporar a quienes el docente quitó manualmente.
6. **Dado** una clase, **Cuando** el docente crea una sesión suelta con fecha, hora y espacio
   (que puede diferir del de la clase), **Entonces** queda programada.
7. **Dado** una sesión programada, **Cuando** el docente cambia su hora o espacio o la
   cancela, **Entonces** el cambio se aplica y queda auditado; una sesión activa o cerrada
   no puede editarse ni cancelarse.
8. **Dado** un horario que generaría más de 200 sesiones o con hora de fin no posterior a
   la de inicio, **Cuando** se envía, **Entonces** se rechaza con un mensaje claro.

---

### Historia de usuario 4 — Encargado crea un evento cerrado o abierto (Prioridad: P1)

El encargado o docente crea un evento: nombre, espacio (existente o nuevo), fecha y hora, y
tipo de acceso. En un **evento cerrado** carga el listado de cédulas de asistentes externos
con el flujo de invitados de 001; los asistentes con cuenta usan credenciales. En un
**evento abierto** no hay listado: cualquier persona con cuenta registra con credenciales y
quien no tiene cuenta se autorregistra con su cédula y nombres.

**Por qué esta prioridad**: los eventos son la segunda modalidad del sistema y hoy no se
pueden crear.

**Prueba independiente**: crear un evento abierto, registrar un asistente con cuenta y un
externo con cédula y nombres, y comprobar que una cédula con cuenta es dirigida a
credenciales.

**Escenarios de aceptación**:

1. **Dado** un encargado autenticado, **Cuando** crea un evento cerrado con nombre, espacio
   y fecha, **Entonces** el evento queda creado con una sesión programada y puede cargar su
   listado de invitados.
2. **Dado** un evento abierto activo, **Cuando** una persona con cuenta registra asistencia
   con credenciales, **Entonces** se acepta si cumple los controles de la sesión, sin
   requerir lista autorizada.
3. **Dado** un evento abierto activo, **Cuando** una persona sin cuenta envía una cédula
   válida y sus nombres, **Entonces** se registra su asistencia como externa con esos datos.
4. **Dado** un evento abierto, **Cuando** se envía como externo una cédula asociada a una
   cuenta, **Entonces** se exige usar credenciales y no se registra por la vía de
   autorregistro.
5. **Dado** una cédula con dígito verificador inválido o nombres vacíos, **Cuando** se envía
   el autorregistro, **Entonces** se rechaza con un mensaje comprensible.
6. **Dado** una cédula externa que ya existe en el sistema con otros nombres, **Cuando** se
   autorregistra en un evento abierto, **Entonces** se registra la asistencia con los nombres
   escritos para ese registro y no se modifican los nombres guardados de la persona.
7. **Dado** un evento cerrado, **Cuando** una persona sin cuenta fuera del listado intenta
   registrarse, **Entonces** se rechaza como en 001; el autorregistro solo existe en eventos
   abiertos.
8. **Dado** un evento, **Cuando** el encargado lo edita o lo cancela antes de iniciar su
   sesión, **Entonces** el cambio queda auditado; el tipo de acceso (abierto/cerrado) no
   puede cambiarse después de iniciar la primera sesión.

---

### Historia de usuario 5 — Responsable elige el método de verificación y la tolerancia geográfica (Prioridad: P2)

Cada actividad define un método de verificación por defecto y cada sesión puede cambiarlo
antes de iniciarse: `QR_Y_GPS`, `SOLO_QR`, `SOLO_GPS` o `NINGUNO`. Al activar el GPS, la
sesión hereda el radio y la precisión máxima por defecto del espacio y el responsable puede
ajustarlos para esa sesión.

**Por qué esta prioridad**: amplía el control de 001 (QR obligatorio + GPS opcional) a lo
que el usuario necesita, pero 001 ya funciona con `SOLO_QR` y `QR_Y_GPS`.

**Prueba independiente**: con un espacio de radio por defecto 30 m, crear una sesión
`SOLO_GPS`, verificar que hereda 30 m, ajustarla a 15 m y registrar asistencia sin QR dentro
y fuera del radio.

**Escenarios de aceptación**:

1. **Dado** una actividad con método por defecto `SOLO_QR`, **Cuando** se generan sus
   sesiones, **Entonces** todas usan `SOLO_QR` salvo que se cambien individualmente.
2. **Dado** un espacio con radio por defecto 30 m y precisión máxima 25 m, **Cuando** se
   activa GPS en una sesión de ese espacio, **Entonces** la sesión propone 30 m y 25 m y el
   responsable puede cambiarlos antes de iniciar.
3. **Dado** una sesión `SOLO_GPS` activa, **Cuando** un participante autorizado envía su
   ubicación válida dentro de la zona sin QR, **Entonces** se acepta.
4. **Dado** una sesión `QR_Y_GPS` o `SOLO_QR`, **Cuando** se envía un intento sin token QR
   vigente, **Entonces** se rechaza como en 001.
5. **Dado** una sesión `NINGUNO` activa, **Cuando** un participante autorizado se autentica
   y envía su asistencia, **Entonces** se acepta sin QR ni ubicación, solo dentro del estado
   activo de la sesión.
6. **Dado** un espacio sin coordenadas, **Cuando** se elige `SOLO_GPS` o `QR_Y_GPS`,
   **Entonces** se rechaza indicando que el espacio no tiene ubicación.
7. **Dado** un participante autenticado, **Cuando** consulta las sesiones disponibles,
   **Entonces** ve solo las sesiones activas en las que puede registrar asistencia (clases
   donde está autorizado, eventos cerrados con cuenta y eventos abiertos) con su método de
   verificación.
8. **Dado** una sesión activa, **Cuando** se intenta cambiar su método o tolerancia,
   **Entonces** se rechaza; la configuración queda fija al iniciar.

---

### Historia de usuario 6 — Un dispositivo registra como máximo una asistencia por sesión (Prioridad: P1)

La app móvil envía en cada intento un identificador de instalación generado al instalarla.
El sistema impide que un mismo dispositivo registre la asistencia de más de una persona en
la misma sesión, para cualquier vía: credenciales, invitado de evento cerrado o autorregistro
de evento abierto.

**Por qué esta prioridad**: es el cambio pedido explícitamente para evitar que un teléfono
registre a varias personas.

**Prueba independiente**: con el mismo identificador de dispositivo, registrar a la persona A
en una sesión y luego intentar registrar a la persona B en la misma sesión.

**Escenarios de aceptación**:

1. **Dado** un dispositivo que ya registró una asistencia aceptada de la persona A en una
   sesión, **Cuando** se intenta registrar a la persona B en la misma sesión desde ese
   dispositivo, **Entonces** se rechaza con el motivo "Este dispositivo ya registró una
   asistencia en esta sesión".
2. **Dado** el mismo dispositivo, **Cuando** registra a la persona B en otra sesión,
   **Entonces** se acepta si cumple los demás controles.
3. **Dado** la persona A ya registrada desde ese dispositivo, **Cuando** reintenta desde el
   mismo dispositivo, **Entonces** se le informa que ya estaba registrada, como en 001.
4. **Dado** un intento de asistencia sin identificador de dispositivo o con un formato
   inválido, **Cuando** llega al servidor, **Entonces** se rechaza indicando usar la app.
5. **Dado** dos intentos simultáneos de personas distintas con el mismo dispositivo en la
   misma sesión, **Cuando** se procesan, **Entonces** se acepta como máximo uno.
6. **Dado** un intento pendiente de revisión geográfica de la persona A desde un dispositivo,
   **Cuando** el docente aprueba otro pendiente de la persona B del mismo dispositivo en la
   misma sesión, **Entonces** la segunda aprobación se rechaza indicando el conflicto de
   dispositivo.
7. **Dado** el listado de asistencias de una sesión, **Cuando** el responsable lo consulta,
   **Entonces** no ve el identificador del dispositivo en claro.

---

### Edge Cases

- Cambio de horario de una clase con sesiones ya generadas → solo se regeneran las sesiones
  programadas futuras sin intentos; las activas, cerradas o con intentos se conservan.
- Fechas del horario que coinciden con un feriado → se generan igual; el docente cancela las
  que no apliquen.
- Estudiante quitado de la lista con asistencia ya registrada → la asistencia se conserva;
  desde ese momento no puede registrar nuevas.
- Paralelo desasignado del docente con una clase ya creada → la clase y su lista se
  conservan; solo se impide sincronizar o crear nuevas clases con él.
- Reinstalar la app genera otro identificador de dispositivo → limitación conocida; no se
  presenta como prevención absoluta del fraude.
- La app no tiene acceso a la MAC real del equipo en iOS y Android modernos → se usa el
  identificador de instalación (decisión del 2026-10-25).
- Sesión `NINGUNO` → no demuestra presencia; solo comprueba identidad, autorización y que la
  sesión está activa. La interfaz del responsable lo advierte al elegirla.
- Evento abierto con persona externa que ya asistió a otro evento → se reutiliza la persona
  existente por cédula; no se crea una cuenta.
- Importación CSV con codificación, separador o encabezados incorrectos → se rechaza el
  archivo completo sin aplicar cambios.
- Espacio con el mismo código escrito distinto ("a-334", "A 334") → se normaliza el código
  antes de comprobar unicidad.

## Requirements *(obligatorio)*

### Requisitos funcionales

**Sección académica simulada**

- **FR-001**: El sistema DEBE ofrecer un rol ADMINISTRADOR, asignado solo por otro
  administrador o por los datos semilla, que gestione personas, cuentas, roles, carreras,
  paralelos, matrículas de estudiantes en paralelos y asignaciones de docentes a paralelos.
- **FR-002**: El sistema DEBE validar la cédula ecuatoriana (10 dígitos, código de provincia
  y dígito verificador) en toda alta de persona, importación, listado de invitados y
  autorregistro, y mantenerla única por persona.
- **FR-003**: El administrador DEBE poder importar CSV de estudiantes, docentes y matrículas;
  la importación DEBE ser idempotente por cédula/código, informar fila y motivo de cada
  rechazo y no dejar el archivo aplicado a medias si el formato general es inválido.
- **FR-004**: Las cuentas DEBEN poder desactivarse sin borrar persona ni asistencias; una
  cuenta desactivada no inicia sesión.
- **FR-005**: La sección académica simulada DEBE poder sustituirse por el proveedor
  institucional sin cambiar las reglas de actividades ni de asistencia.

**Espacios**

- **FR-006**: Cualquier docente, encargado o administrador DEBE poder crear un espacio con
  código (único tras normalizar mayúsculas, espacios y guiones), nombre, tipo (AULA,
  LABORATORIO, AUDITORIO, EXTERIOR, OTRO), coordenadas opcionales, radio por defecto y
  precisión máxima por defecto.
- **FR-007**: El sistema DEBE permitir buscar espacios por código o nombre de forma paginada.
- **FR-008**: Solo el creador del espacio o un administrador DEBE poder editarlo; editarlo
  no cambia la configuración de sesiones activas o cerradas.

**Actividades y sesiones**

- **FR-009**: Un docente DEBE poder crear una clase con nombre, paralelos que tenga
  asignados, espacio y horario recurrente opcional; la lista autorizada inicial DEBE ser la
  de estudiantes matriculados vigentes en esos paralelos.
- **FR-010**: El responsable DEBE poder quitar y añadir estudiantes de la lista autorizada y
  sincronizarla con los paralelos sin reincorporar a quienes quitó manualmente.
- **FR-011**: Un horario recurrente DEBE generar sesiones programadas por días de la semana,
  hora de inicio y fin y rango de fechas, en la zona horaria de la institución, con un
  máximo de 200 sesiones por horario.
- **FR-012**: Un docente o encargado DEBE poder crear un evento con nombre, espacio, fecha y
  hora, y tipo de acceso CERRADO o ABIERTO; el evento genera su sesión programada.
- **FR-013**: El responsable DEBE poder crear sesiones sueltas y editar o cancelar
  actividades y sesiones programadas; las sesiones activas, cerradas o con intentos no se
  editan, cancelan ni regeneran.
- **FR-014**: Las operaciones de creación, edición, cancelación, sincronización y cambios de
  lista DEBEN auditarse con responsable, fecha, valores anterior y nuevo, como en FR-023 de
  001.

**Eventos abiertos**

- **FR-015**: En eventos abiertos, cualquier cuenta activa DEBE poder registrar asistencia
  con credenciales sin lista autorizada.
- **FR-016**: En eventos abiertos, una persona sin cuenta DEBE poder autorregistrarse con
  cédula válida y nombres (1 a 200 caracteres); si la cédula tiene cuenta, el sistema DEBE
  exigir credenciales. El autorregistro DEBE reutilizar la persona externa existente por
  cédula y NO DEBE crear cuentas.
- **FR-017**: El autorregistro DEBE limitarse a eventos abiertos; los eventos cerrados
  mantienen las reglas de invitados de 001 sin cambios.

**Método de verificación y tolerancia**

- **FR-018**: Cada actividad DEBE tener un método de verificación por defecto y cada sesión
  programada DEBE poder cambiarlo entre `QR_Y_GPS`, `SOLO_QR`, `SOLO_GPS` y `NINGUNO`; el
  valor por defecto de una actividad nueva es `SOLO_QR`.
- **FR-019**: Los métodos con GPS DEBEN exigir un espacio con coordenadas, heredar su radio y
  precisión por defecto y permitir ajustarlos por sesión; las reglas de precisión, antigüedad,
  repetición y revisión de 001 aplican igual.
- **FR-020**: Los métodos con QR DEBEN aplicar las reglas de token de 001; los métodos sin
  QR DEBEN aceptar intentos solo mientras la sesión esté activa.
- **FR-021**: El método y la tolerancia DEBEN quedar fijos al iniciar la sesión.
- **FR-022**: El sistema DEBE listar al participante autenticado las sesiones activas en las
  que puede registrar asistencia, con su método de verificación.

**Unicidad por dispositivo**

- **FR-023**: Todo intento de asistencia (credenciales, invitado de evento cerrado y
  autorregistro) DEBE incluir el identificador de instalación de la app; los intentos sin él
  o con formato inválido DEBEN rechazarse con motivo comprensible.
- **FR-024**: El sistema DEBE garantizar como máximo una asistencia aceptada por dispositivo
  y sesión, también ante solicitudes simultáneas y al aprobar revisiones; un dispositivo puede
  registrar a otra persona en otra sesión.
- **FR-025**: El identificador de dispositivo DEBE guardarse solo en forma no reversible, NO
  DEBE mostrarse en consultas ni exportaciones y DEBE eliminarse con la anonimización de 5
  años de 001.
- **FR-026**: El registro de asistencia DEBE hacerse desde la app móvil; la web queda para
  responsables y administradores.

### Privacidad

- **Datos nuevos**: cédula y nombres de externos autorregistrados; identificador de
  instalación del dispositivo; coordenadas de espacios (no son datos personales).
- **Quién accede**: igual que 001 (responsable de la actividad y directores según su
  ámbito); el administrador ve personas y cuentas de la sección simulada. Nadie ve el
  identificador de dispositivo en claro.
- **Conservación y eliminación**: las mismas de 001 (FR-022 y FR-025 de 001): cinco años desde
  el cierre definitivo de la actividad y luego anonimización, que incluye el identificador
  de dispositivo.
- La base legal (LOPDP) sigue PENDIENTE de validación institucional.

### Entidades clave

- **Persona**: cédula y nombres; institucional o externa. Existente en 001.
- **Cuenta**: acceso de una persona con correo, contraseña y roles; se puede desactivar.
  Se añade el rol ADMINISTRADOR.
- **Carrera / Paralelo**: existentes en 001.
- **Matrícula**: estudiante inscrito en un paralelo, vigente o no.
- **Asignación docente**: docente que dicta en un paralelo.
- **Espacio**: código normalizado único, nombre, tipo, coordenadas opcionales, radio y
  precisión por defecto, creador.
- **Actividad**: clase o evento; para eventos, tipo de acceso CERRADO/ABIERTO; método de
  verificación por defecto; estado (abierta, cancelada, cerrada).
- **Horario recurrente**: días, horas, rango de fechas y espacio de una clase.
- **Sesión**: existente en 001; añade método de verificación, tolerancia propia, estado
  CANCELADA y origen (horario o suelta).
- **Exclusión manual de lista**: estudiante que el responsable quitó y que la sincronización
  no debe reincorporar.
- **Intento / Asistencia**: existentes en 001; añaden el identificador de dispositivo protegido
  y la forma AUTORREGISTRO.

## Success Criteria *(obligatorio)*

### Resultados medibles

- **SC-001**: Un docente crea una clase con dos paralelos, un espacio nuevo con ubicación en
  el mapa y un horario de un semestre en menos de 5 minutos.
- **SC-002**: Un administrador importa 1 000 estudiantes con sus matrículas en menos de 2
  minutos, con informe de filas rechazadas, y reimportarlos no crea duplicados.
- **SC-003**: En pruebas con 50 intentos simultáneos desde el mismo dispositivo y personas
  distintas en una sesión, se acepta exactamente una asistencia.
- **SC-004**: El 100 % de los intentos sin identificador de dispositivo se rechazan con motivo.
- **SC-005**: Un externo completa el autorregistro en un evento abierto en menos de 1 minuto
  desde que abre la app.
- **SC-006**: Las sesiones generadas por un horario coinciden al 100 % con las fechas y horas
  esperadas en las pruebas, incluidos cambios de mes.
- **SC-007**: Ningún usuario sin el rol correspondiente puede ejecutar operaciones de
  administración, crear actividades o editar espacios ajenos en las pruebas de autorización.
- **SC-008**: Ninguna consulta, exportación ni registro de auditoría muestra el identificador
  de dispositivo en claro.

## Assumptions

- El identificador de instalación cambia al reinstalar la app o borrar sus datos; no evita
  que una persona use dos teléfonos ni demuestra titularidad. Es una limitación declarada.
  La atestación del dispositivo (Play Integrity / App Attest) queda fuera de este incremento.
- La "dirección MAC" pedida se implementa como identificador de instalación porque iOS y
  Android modernos no permiten leer la MAC real (decisión del usuario, 2026-10-25).
- La unicidad por dispositivo es por sesión, no por cuenta: el teléfono no queda vinculado a
  una persona (decisión del usuario, 2026-10-25).
- La página de asistencia de la web deja de ofrecerse a participantes; la web atiende a
  responsables, directores y administradores.
- La zona horaria de la institución es America/Guayaquil.
- El método por defecto `SOLO_QR` conserva el comportamiento actual de 001.
- El mapa para elegir coordenadas es responsabilidad del cliente web; el servidor recibe y
  valida latitud y longitud.
- Los roles director de 001 no ganan permisos de administración.
- La sección académica simulada no se usa con datos institucionales reales.

### Fuera del alcance de este incremento

Integración real con CAS o el sistema académico, atestación del dispositivo, polígonos,
notificaciones, calendario de feriados, control de permanencia y reportes nuevos.

## Clarifications

### Session 2026-10-25

- Q: ¿El registro usa QR, GPS o ambos? → A: Configurable por sesión (`QR_Y_GPS`, `SOLO_QR`,
  `SOLO_GPS`, `NINGUNO`).
- Q: En un evento abierto, ¿cómo se identifica una persona sin cuenta? → A: Autorregistro con
  cédula y nombres; si la cédula tiene cuenta, debe usar credenciales.
- Q: ¿Qué identificador usa la unicidad por dispositivo? → A: Identificador de instalación de
  la app (la MAC real no es accesible).
- Q: ¿El dispositivo queda vinculado a una cuenta? → A: No; solo único por sesión.
- Q: ¿Quién administra estudiantes, docentes, carreras, paralelos y matrículas? → A: Rol
  ADMINISTRADOR con CRUD e importación CSV.
- Q: ¿Quién crea espacios? → A: Cualquier docente o encargado; editables por su creador o
  un administrador.
- Q: ¿Cómo se reparte la tolerancia geográfica? → A: El espacio define valores por defecto y
  la sesión puede ajustarlos.
- Q: ¿Sesiones a mano u horario recurrente? → A: Horario recurrente que genera sesiones,
  además de sesiones sueltas.
