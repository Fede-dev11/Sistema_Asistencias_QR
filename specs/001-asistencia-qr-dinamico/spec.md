# Especificación de funcionalidad: Registro de asistencia con QR dinámico y validación geográfica opcional

**Rama de la funcionalidad**: `feature/001-asistencia-qr-dinamico`

**Creado**: 2026-10-10

**Estado**: Borrador de diseño — decisiones funcionales aclaradas; el acceso de invitados
por cédula y listado constituye una desviación del Principio VI pendiente de revisión del
equipo antes de integrar. La constitución no se modifica ni se declara una excepción aprobada.

**Entrada**: Descripción del equipo y clarificaciones del usuario: "Un docente o encargado
activa una sesión de clase o evento que muestra un código QR que cambia periódicamente.
En clases solo registran asistencia los estudiantes autorizados de esa clase. En eventos,
los participantes con cuenta usan credenciales y los invitados sin cuenta introducen solo
su cédula. El encargado debe haber precargado sus cédulas y nombres en el listado del evento;
no se permite registro libre de personas fuera del listado. Los invitados cuentan con una
tolerancia configurable por evento para enviar su cédula. El sistema comprueba el acceso según el tipo de participante,
la autorización, la vigencia, la unicidad y, cuando esté habilitada, la ubicación."

## Problema que resuelve

La firma manual consume tiempo de clase y puede suplantarse; un QR estático se comparte por
foto a quien no está presente. El docente necesita un registro de asistencia rápido y
**auditable**, y el estudiante necesita saber de inmediato si su asistencia quedó registrada
y, si no, por qué. Esta funcionalidad es el núcleo mínimo (MVP) del sistema.
El alcance comprende dos modalidades de sesión: clase y evento. En eventos cerrados se
distinguen participantes institucionales UNL e invitados externos sin cuenta permanente;
el formulario de invitado no elimina la comprobación de autorización para ese evento.

## User Scenarios & Testing *(obligatorio)*

### Historia de usuario 1 — Docente o encargado abre una sesión con QR dinámico (Prioridad: P1)

El docente o encargado, autenticado, selecciona una sesión de una actividad ya creada con
su espacio asignado y la inicia. El panel web muestra un QR que se renueva automáticamente
mientras la sesión esté abierta. Al terminar la clase o evento, el responsable cierra la sesión.
La validación geográfica se configura por sesión y está desactivada por defecto.
La modalidad de clase aplica la lista de estudiantes autorizados de esa clase. La modalidad
de evento admite participantes UNL con credenciales y los flujos de invitados externos.
El encargado configura para cada evento la duración `tolerancia_invitado_segundos`, superior
a 40 s, que determina el intervalo disponible para enviar la cédula después de abrir el
formulario con un QR validado por el servidor. No se fija una duración de 2 o 5 minutos.

**Por qué esta prioridad**: sin una sesión activa y un QR vigente no existe ningún otro flujo.

**Prueba independiente**: con datos semilla (un docente, un espacio, una sesión programada),
iniciar la sesión, observar que el QR cambia en el intervalo configurado y cerrarla.

**Escenarios de aceptación**:

1. **Dado** un docente autenticado y una sesión en estado *programada*, **Cuando** pulsa
   "Iniciar sesión", **Entonces** la sesión pasa a *activa* y se muestra un QR.
2. **Dado** una sesión activa, **Cuando** transcurre el intervalo de rotación (30 s),
   **Entonces** se muestra un QR nuevo y el anterior sigue vigente durante 10 s adicionales,
   hasta completar 40 s desde su emisión.
3. **Dado** una sesión activa, **Cuando** el docente la cierra, **Entonces** la sesión pasa a
   *cerrada* y ningún token emitido para ella vuelve a aceptarse, aunque no haya caducado.
4. **Dado** un usuario sin rol de docente u organizador de esa actividad, **Cuando** solicita
   el QR de la sesión, **Entonces** el sistema lo rechaza.
5. **Dado** una sesión recién creada, **Cuando** el responsable consulta su configuración,
   **Entonces** la validación geográfica está desactivada y puede configurarla para esa sesión.
6. **Dado** un encargado responsable de un evento, **Cuando** carga cédulas y nombres de
   participantes externos, **Entonces** esos datos quedan asociados a ese evento y se pueden
   utilizar para identificar a los invitados sin pedirles que vuelvan a escribir sus nombres.
   La opción de invitado se reserva a personas sin cuenta; tener una entrada en el listado
   no permite eludir el acceso con credenciales cuando la cédula ya está asociada a una cuenta.
7. **Dado** un encargado responsable de un evento, **Cuando** configura la tolerancia de
   invitados, **Entonces** el sistema acepta una duración entera superior a 40 s y rechaza
   valores vacíos, no enteros o menores o iguales a 40 s con un mensaje comprensible.

---

### Historia de usuario 2 — Participante registra su asistencia escaneando el QR (Prioridad: P1)

El estudiante autorizado de una clase inicia sesión y escanea el QR proyectado. En eventos,
el participante con cuenta usa sus credenciales. Quien selecciona invitado introduce solo su
cédula: el servidor comprueba si está asociada a una cuenta; si lo está, exige el acceso con
credenciales y no registra asistencia por la vía de invitado. Si no tiene cuenta, la cédula
debe estar en el listado que el encargado cargó previamente para ese evento. El sistema toma
los nombres y los demás datos de ese listado y guarda automáticamente la asistencia cuando
se cumplen los controles del flujo. Si la cédula no aparece en el listado, se rechaza el
registro y se indica acudir al encargado; no se ofrece autorregistro con cédula y nombres.
Solo si la sesión tiene habilitada la validación geográfica se solicita permiso de ubicación.
El sistema muestra el resultado con un motivo comprensible.

**Por qué esta prioridad**: es el valor principal del sistema para docentes y estudiantes.

**Prueba independiente**: con una sesión activa, enviar una solicitud de asistencia con un
token vigente, sin ubicación cuando el control geográfico esté desactivado y con una ubicación
válida cuando esté activado, y verificar que se registra una sola vez en ambos modos.

**Escenarios de aceptación**:

1. **Dado** un estudiante autenticado y autorizado de la clase, una sesión activa sin validación
   geográfica y un token vigente, **Cuando** envía su asistencia sin ubicación, **Entonces**
   queda registrada como *aceptada* sin solicitar permiso de ubicación y se le muestra
   "Asistencia registrada".
2. **Dado** un token caducado, **Cuando** el estudiante lo envía, **Entonces** el intento se
   rechaza con el motivo "Código QR vencido, escanee el código actual".
3. **Dado** un estudiante con asistencia ya aceptada en la sesión, **Cuando** vuelve a
   enviar, **Entonces** no se crea un segundo registro y se le informa que ya estaba registrado.
4. **Dado** una sesión con validación geográfica habilitada y una ubicación reportada fuera
   de la zona autorizada, **Cuando** envía,
   **Entonces** el intento se rechaza con el motivo "Fuera de la zona de la sesión".
5. **Dado** una sesión cerrada, **Cuando** el estudiante envía cualquier token,
   **Entonces** el intento se rechaza con el motivo "La sesión ya no admite registros".
6. **Dado** un estudiante autenticado y autorizado de la clase, una sesión activa con validación
   geográfica habilitada, un token vigente y una ubicación dentro de la zona con precisión y antigüedad válidas,
   **Cuando** envía su asistencia, **Entonces** queda registrada como *aceptada* y se le muestra
   "Asistencia registrada".
7. **Dado** un intento del flujo ordinario que cumple las demás validaciones, **Cuando** el
   servidor lo recibe 39 s después de emitir el token, **Entonces** lo acepta aunque ya se
   muestre un QR nuevo.
8. **Dado** un token del flujo ordinario emitido hace 40 s o más según el reloj del servidor,
   **Cuando** el servidor recibe el intento, **Entonces** lo rechaza como vencido aunque el dispositivo
   informe que se escaneó antes del vencimiento.
9. **Dado** un estudiante autenticado que no pertenece a la lista autorizada de la clase,
   **Cuando** intenta registrar asistencia con un QR vigente, **Entonces** se rechaza por
   falta de autorización para esa clase, sin habilitarle el formulario de invitado.
10. **Dado** un evento activo y un participante UNL, **Cuando** solicita registrar asistencia,
    **Entonces** se le piden credenciales y se valida su identidad y autorización en el servidor.
11. **Dado** un invitado sin cuenta precargado por el encargado de ese evento, **Cuando**
    introduce una cédula válida y coincidente con la lista del evento, **Entonces** el sistema
    utiliza los nombres y demás datos del listado y registra la asistencia automáticamente
    si cumple los controles de sesión, plazo, unicidad y ubicación cuando corresponda.
12. **Dado** una cédula que no está precargada en el listado de invitados de ese evento,
    **Cuando** se intenta registrar asistencia como invitado, **Entonces** se rechaza por no
    constar en el listado, se indica acudir al encargado y no se solicita completar nombres.
13. **Dado** una cédula asociada a una cuenta, aunque aparezca en el listado de invitados,
    **Cuando** se intenta registrar asistencia como invitado, **Entonces** se exige usar
    credenciales y no se crea una asistencia por la vía de invitado.
14. **Dado** un invitado sin cuenta y autorizado en el listado del evento, **Cuando** abre el
    formulario y el servidor valida un QR vigente, **Entonces** comienza un plazo de
    `tolerancia_invitado_segundos`, medido por el servidor, para enviar su cédula. El cambio
    o la caducidad posterior del QR compartido no cancela este inicio de registro ya validado.
15. **Dado** un inicio de registro de invitado validado por el servidor, **Cuando** se envía
    la cédula antes del límite configurado, **Entonces** se comprueban nuevamente la ausencia
    de cuenta, el listado del evento y los demás controles antes de guardar la asistencia.
16. **Dado** un inicio de registro de invitado cuyo plazo se cumplió o cuya sesión se cerró,
    **Cuando** se envía la cédula, **Entonces** no se registra asistencia; al vencer el plazo
    se indica volver a escanear un QR vigente si la sesión sigue activa.

---

### Historia de usuario 3 — Manejo de ubicación imprecisa con validación geográfica habilitada (Prioridad: P2)

En sesiones con validación geográfica habilitada, cuando la precisión reportada por el
dispositivo es insuficiente o la ubicación es antigua,
el sistema no acepta automáticamente: pide repetir la medición y, si persiste, deja el
intento *pendiente de revisión* para el docente.

**Por qué esta prioridad**: evita falsos positivos y falsos negativos en interiores, el
riesgo más alto identificado, pero el MVP puede demostrarse sin ella.

**Prueba independiente**: en una sesión con validación geográfica habilitada, enviar una
solicitud con precisión de 80 m en un aula de radio 20 m
y verificar que el resultado es *repetir medición* y luego *pendiente de revisión*.

**Escenarios de aceptación**:

1. **Dado** una ubicación cuya precisión supera el umbral de la sesión, **Cuando** el
   estudiante envía, **Entonces** se le pide repetir la medición y no se registra asistencia.
2. **Dado** que el círculo de incertidumbre (ubicación ± precisión) cruza el borde de la zona,
   **Cuando** el estudiante envía por segunda vez con el mismo resultado, **Entonces** el
   intento queda *pendiente de revisión* y se le informa.
3. **Dado** un intento pendiente, **Cuando** el docente lo aprueba o rechaza indicando una
   razón, **Entonces** se registra la decisión con responsable, fecha y razón.

---

### Historia de usuario 4 — Responsables y directores consultan asistencias según su alcance (Prioridad: P3)

Durante la sesión, el docente ve la lista de asistencias aceptadas, rechazadas y pendientes
con su motivo, y al final puede exportarla.
El docente o encargado solo consulta las clases y eventos de los que es responsable.
El director con alcance institucional consulta todas las actividades. El director de carrera
consulta únicamente las actividades de su carrera, incluidos sus paralelos. Estos permisos
de consulta no conceden por sí solos permisos de modificación ni de exportación adicionales.

**Por qué esta prioridad**: aporta control y trazabilidad, pero no bloquea el registro.

**Prueba independiente**: con varias asistencias de distintos estados, consultar el listado
de la sesión y verificar conteos y motivos.

**Escenarios de aceptación**:

1. **Dado** una sesión con intentos registrados, **Cuando** el docente abre el listado,
   **Entonces** ve cada participante con estado, hora y motivo.
2. **Dado** una sesión cerrada, **Cuando** el docente exporta, **Entonces** obtiene un
   archivo CSV con las asistencias de esa sesión.
3. **Dado** un docente o encargado que no es responsable de una actividad, **Cuando** intenta
   consultar sus cédulas, nombres o asistencias, **Entonces** el servidor rechaza el acceso,
   incluso si conoce el identificador de esa actividad.
4. **Dado** un director con alcance institucional autenticado, **Cuando** consulta las
   actividades y sus asistencias, **Entonces** puede acceder a todas las clases y eventos.
5. **Dado** un director de carrera autenticado, **Cuando** consulta actividades y asistencias,
   **Entonces** solo obtiene las vinculadas a su carrera y sus paralelos; el acceso directo
   a una actividad de otra carrera también es rechazado.

---

### Casos límite

- Dos solicitudes simultáneas de la misma persona en la misma sesión → un único registro.
- Pérdida de red durante el envío → el reintento con la misma clave de idempotencia no
  duplica el registro.
- El QR cambia mientras el estudiante envía la solicitud o concede permisos de ubicación
  → en el flujo ordinario, el token anterior sigue siendo válido solo si el servidor recibe
  el intento antes de que se cumplan 40 s desde su emisión. El instante de escaneo informado por el dispositivo
  no amplía la vigencia.
- Un invitado necesita más tiempo para enviar su cédula → dispone del intervalo entre el
  inicio validado por el servidor y ese inicio más `tolerancia_invitado_segundos`. Al cumplirse
  el límite se rechaza el envío. El cierre de la sesión termina ese intervalo inmediatamente.
- El invitado recarga el formulario o el dispositivo informa otra hora → el inicio de
  registro ya validado conserva su plazo original; el cliente no puede reiniciar ni ampliar
  el tiempo concedido por el servidor.
- El QR ya venció al intentar abrir el formulario de invitado → el servidor no inicia el
  registro ni concede la tolerancia adicional; se debe escanear un QR vigente.
- Una persona conoce una cédula ajena incluida en el listado → la comprobación de lista y
  ausencia de cuenta no acredita la titularidad de esa cédula. Es una limitación del flujo
  expresamente elegido por el usuario, sin verificación presencial ni código adicional.
  La desviación del Principio VI se registra en *Complexity Tracking* del plan para revisión
  antes de integrar; no se presenta como una garantía de identidad.
- Un invitado vuelve a registrarse con la misma cédula o después accede mediante una cuenta
  vinculada a esa persona → se mantiene como máximo una asistencia por persona y sesión.
- El estudiante niega el permiso de cámara → se le explica el motivo y la alternativa
  (usar la web o acudir al docente).
- Si la validación geográfica está habilitada y el estudiante niega el permiso de ubicación
  o no puede obtener una medición, no se acepta automáticamente y se ofrece repetición o
  revisión docente.
- Si la validación geográfica está desactivada, no se solicita ni almacena ubicación y su
  ausencia no impide el registro.
- Muchos estudiantes escanean el mismo token → el token **no** se invalida tras el primer uso.
- Reloj del dispositivo desajustado → irrelevante, la caducidad usa la hora del servidor.

## Requirements *(obligatorio)*

### Requisitos funcionales

- **FR-001**: El sistema DEBE permitir a un docente u organizador iniciar y cerrar una
  sesión de la que es responsable.
- **FR-002**: El sistema DEBE emitir, mientras la sesión esté activa, un token QR aleatorio
  criptográficamente seguro asociado a la sesión, renovado cada 30 s y con vigencia total
  de 40 s desde su emisión para el flujo ordinario (30 s más 10 s de tolerancia). El servidor
  DEBE comprobar la vigencia con su propia hora de recepción: el token caduca al cumplirse los 40 s, sin
  ampliar ese plazo por la hora o el instante de escaneo que indique el dispositivo.
  La tolerancia adicional de invitados se define en FR-020 y no extiende la vigencia del QR
  compartido: permite completar un inicio de registro previamente validado por el servidor.
- **FR-003**: El QR NO DEBE contener datos personales ni secretos; solo una referencia
  opaca al token.
- **FR-004**: El sistema DEBE mostrar el QR únicamente a usuarios autorizados de la sesión.
- **FR-005**: El sistema DEBE recibir intentos ordinarios de asistencia con token QR. En el
  flujo de invitados DEBE comprobar el inicio de registro previamente validado conforme a
  FR-020; un QR vencido por sí solo no habilita ese flujo. Solo cuando la sesión
  tenga habilitada la validación geográfica DEBE solicitar coordenadas, precisión estimada
  y marca de tiempo de la medición. Con el control desactivado NO DEBE solicitar ni almacenar
  datos de ubicación.
- **FR-006**: El sistema DEBE validar en servidor las credenciales de participantes con cuenta
  o, para invitados, la cédula, la ausencia de cuenta y la pertenencia al listado del evento.
  DEBE comprobar su autorización para la clase o evento, la sesión activa y dentro
  de la ventana, la vigencia aplicable al flujo, la ausencia de asistencia previa y, solo
  si está habilitada en la sesión, la proximidad geográfica. Validar la cédula contra el
  listado identifica una entrada autorizada, pero no acredita la titularidad de esa identidad;
  el usuario confirma expresamente este alcance sin verificación presencial. La desviación
  del Principio VI queda documentada en el plan, pendiente de revisión del equipo antes de integrar.
- **FR-007**: Solo cuando la sesión tenga habilitada la validación geográfica, el sistema
  DEBE evaluar la proximidad contra el punto y radio de la sesión (heredado del espacio si
  la sesión no lo define). El rechazo automático por ubicación requiere mediciones reales
  documentadas de falsos positivos y falsos negativos conforme al Principio VII.
- **FR-008**: En sesiones con validación geográfica habilitada, el sistema NO DEBE aceptar
  automáticamente ubicaciones con precisión peor que el umbral, con antigüedad mayor a la
  permitida o no disponibles; DEBE ofrecer repetición o revisión docente (RN-05).
- **FR-009**: El sistema DEBE garantizar como máximo una asistencia aceptada por persona y
  sesión, incluso ante solicitudes concurrentes (RN-01).
- **FR-010**: El sistema DEBE registrar cada intento con su resultado (aceptado, rechazado,
  repetir medición, pendiente de revisión) y un motivo legible (RN-12).
- **FR-011**: El sistema DEBE dejar de aceptar registros al cerrar la sesión (RN-10).
- **FR-012**: El sistema DEBE permitir al docente o encargado responsable resolver intentos pendientes registrando
  responsable, fecha y razón (RN-09).
- **FR-013**: El sistema DEBE permitir al docente o encargado responsable consultar y exportar a CSV las asistencias
  de una sesión.
- **FR-014**: El sistema DEBE aplicar límites de peticiones a los intentos de asistencia,
  incluidos los flujos de invitados sin cuenta.
- **FR-015**: El sistema DEBE permitir al docente u organizador responsable configurar la
  validación geográfica por sesión. DEBE estar desactivada por defecto; cuando esté
  desactivada, la ausencia de ubicación NO DEBE impedir registrar asistencia.
- **FR-016**: El sistema DEBE distinguir sesiones de clase y sesiones de evento. En clases,
  solo DEBE autorizar a estudiantes autenticados incluidos en la lista de esa clase; NO DEBE
  permitir el acceso como invitado para eludir esa restricción.
- **FR-017**: En eventos, el sistema DEBE pedir credenciales a los participantes UNL y
  distinguirlos de los invitados externos, incluidos participantes de otras universidades.
  El proveedor de autenticación institucional y el acceso a CAS siguen pendientes de
  confirmación; NO DEBE asumirse una integración institucional disponible.
- **FR-018**: Solo el encargado responsable DEBE poder precargar las cédulas y nombres de
  invitados autorizados para su evento. El invitado DEBE introducir únicamente su cédula;
  el servidor DEBE validar que no tiene cuenta y que está incluido en el listado de ese
  evento, y guardar automáticamente la asistencia con los datos precargados cuando se cumplan
  los demás controles. NO DEBE aceptar nombres ni otros datos introducidos por el invitado
  para sustituir los del listado, ni crearle una cuenta permanente.
- **FR-019**: Si la cédula está asociada a una cuenta, el sistema DEBE impedir el registro
  como invitado y exigir el acceso con credenciales, incluso si está precargada en el listado.
  Si la cédula no tiene cuenta pero no consta en el listado de invitados del evento, DEBE
  rechazar el registro y remitir al encargado. NO DEBE existir autorregistro libre de invitados.
- **FR-020**: El encargado responsable DEBE poder configurar por evento la duración
  `tolerancia_invitado_segundos` como entero superior a 40 s, sin fijar un valor obligatorio
  de 2 o 5 minutos. DEBE establecerse antes de habilitar el registro de invitados. El intervalo
  permitido empieza cuando el servidor valida un QR vigente al abrir el formulario y termina
  al cumplirse la duración configurada o al cerrar la sesión, lo que ocurra primero.
  El servidor DEBE conservar ese inicio y su límite para comprobar el envío posterior;
  cambiar o caducar el QR compartido no invalida un inicio ya validado dentro de su plazo.
  Una recarga o la hora del dispositivo NO DEBEN ampliar ese plazo. Al enviar la cédula se
  DEBEN volver a comprobar la ausencia de cuenta, el listado del evento y los demás controles.
- **FR-021**: El sistema DEBE limitar la consulta de cédulas, nombres, actividades y
  asistencias al alcance autorizado en el servidor: docentes y encargados acceden solo a
  las actividades de las que son responsables; el director con alcance institucional puede
  consultar todas; el director de carrera solo las de su carrera, incluidos sus paralelos.
  El alcance DEBE aplicarse tanto a listados como a accesos directos por identificador.
  Los permisos de consulta NO DEBEN otorgar automáticamente facultades de modificación.
  La exportación ya autorizada en FR-013 conserva el mismo alcance de responsabilidad;
  los roles de director no conceden exportación. Ampliar ese permiso requerirá otra decisión.
- **FR-022**: El sistema DEBE conservar los datos de clases y eventos, sus listados de
  participantes, asistencias e historial de auditoría durante un plazo provisional de
  cinco años calendario desde el cierre definitivo de la clase o evento. Al cumplirse ese
  plazo DEBE anonimizarse la información para conservar únicamente estadísticas no
  identificables conforme a FR-025. Esta política de producto sigue sujeta a validación
  institucional y NO DEBE interpretarse como conservación nominal indefinida.
  Este plazo no extiende la vigencia ni exige conservar tokens
  QR o inicios temporales de registro durante cinco años.
- **FR-023**: El sistema DEBE permitir auditar las acciones administrativas sobre listados,
  asistencias y revisiones, registrando responsable, acción y fecha; para cambios manuales
  de asistencia, también razón y valores anterior y nuevo. El acceso al historial DEBE
  respetar el alcance de la actividad establecido en FR-021.
- **FR-024**: El responsable DEBE poder cerrar definitivamente una clase o evento. Este cierre
  DEBE impedir nuevas sesiones y registros, cerrar sus sesiones aún abiertas y fijar una fecha
  de cierre inmutable que inicia el plazo de cinco años. El cierre de una sola sesión de una
  actividad con varias sesiones no equivale por sí solo al cierre definitivo de la actividad.
- **FR-025**: Al vencer el plazo de FR-022, el sistema DEBE impedir consultas y exportaciones
  identificadas de esa actividad y anonimizar sus listados, asistencias, intentos e historial
  de auditoría, incluidos los metadatos nominales de actividad, sesiones y autorizaciones.
  Solo DEBEN conservarse estadísticas agregadas sin cédulas, nombres, vínculos
  a personas o cuentas, textos libres identificativos ni combinaciones que permitan deducir
  identidades en grupos pequeños. Sustituir una cédula por un hash no constituye por sí solo
  anonimización. El tratamiento DEBE abarcar copias de respaldo y restauraciones para evitar
  volver a exponer datos vencidos. No DEBE eliminarse una cuenta global vigente ni sus datos
  de otras actividades por vencer una actividad; se retiran los datos y vínculos vencidos.

### Acceso, conservación y auditoría

Los datos identificativos y las asistencias se consultan exclusivamente dentro de estos ámbitos:

| Rol | Alcance de consulta |
|-----|---------------------|
| Docente o encargado | Sus clases y eventos, de los que es responsable |
| Director con alcance institucional | Todas las clases y eventos |
| Director de carrera | Clases y eventos de su carrera, incluidos sus paralelos |

La asignación de actividad a carrera y paralelo DEBE comprobarse en el servidor a partir
de los datos de la actividad, no de un filtro enviado por el cliente. Una actividad sin
vinculación a la carrera del director no se vuelve accesible por tener este ese rol.
Los roles de director solo conceden la consulta descrita; no añaden permiso de exportación.

El plazo de conservación acordado es **cinco años calendario desde el cierre definitivo
de la clase o evento**, para datos de actividad, listados, asistencias e historial de auditoría.
Después se anonimiza la información para conservar estadísticas, incluyendo el tratamiento
de respaldos y restauraciones. El acceso identificado se bloquea al vencer el plazo aunque
un proceso de limpieza deba reintentarse. No se afirma que esta decisión constituya una
política institucional o una validación de la base legal pendiente.

### Entidades clave

- **Participante**: persona institucional UNL o externa; identidad separada de su cuenta de
  acceso. El flujo de invitados solo admite personas sin cuenta, con cédula y nombres
  precargados por el encargado; una persona con cuenta debe usar credenciales.
- **Autorización de participación**: vínculo de un estudiante con su clase o de un externo
  precargado con el evento; identifica al encargado responsable de cargar los datos del evento.
- **Carrera y paralelo**: ámbito académico al que se vincula una clase o evento, utilizado
  para limitar las consultas del director de carrera; la asignación del director a su carrera
  se comprueba independientemente de los identificadores que envíe el cliente.
- **Acción de auditoría**: responsable, actividad afectada, acción y fecha; en cambios manuales
  de asistencia, razón y valores anterior y nuevo. Conservación nominal de cinco años desde
  el cierre definitivo de la actividad y posterior tratamiento conforme a FR-025.
- **Actividad**: clase o evento, responsable, carrera y paralelo cuando correspondan; estado
  abierta/cerrada, fecha de cierre definitivo y fecha de anonimización a los cinco años calendario.
- **Estadística anonimizada**: conteos agrupados por dimensiones suficientemente amplias,
  sin identificadores de personas, cuentas o actividad nominal ni detalle de grupos pequeños.
- **Espacio**: lugar físico reutilizable; nombre, tipo y, para validación geográfica, punto
  central y radio por defecto.
- **Sesión**: ventana concreta de control; actividad, espacio, inicio, fin, estado
  (programada/activa/cerrada), modalidad (clase/evento), responsable,
  validación geográfica habilitada (por defecto no), intervalo
  de rotación y, cuando se habilite la validación geográfica, radio y umbral de precisión.
- **Configuración de invitados del evento**: duración `tolerancia_invitado_segundos`,
  establecida por el encargado como entero superior a 40 s.
- **Inicio de registro de invitado**: apertura de formulario validada por el servidor con un
  QR vigente; asociado a la sesión, con inicio y límite calculados según la configuración
  del evento. El cierre de la sesión lo invalida aunque no haya vencido su plazo.
- **Token QR**: referencia temporal a una sesión; valor (almacenado como hash), emitido,
  expira (40 s después de la emisión).
- **Asistencia**: resultado confirmado por persona y sesión; estado, hora, forma de validación.
- **Intento de asistencia**: cada solicitud; persona, sesión, resultado, motivo y marca de
  tiempo. Solo con validación geográfica habilitada incluye precisión y distancia calculada
  (sin almacenar coordenadas exactas en rechazos).
- **Decisión de revisión**: resolución manual de un intento; responsable, fecha, razón.

## Success Criteria *(obligatorio)*

### Resultados medibles

- **SC-001**: Un estudiante con buena señal completa el registro (escaneo → resultado) en
  menos de 10 segundos.
- **SC-002**: La validación en servidor responde en p95 < 2 s, sin contar la adquisición de GPS.
- **SC-003**: En 100 pruebas de solicitudes concurrentes de la misma persona, se crea
  exactamente 1 asistencia en el 100 % de los casos.
- **SC-004**: En el flujo ordinario, el 100 % de los intentos recibidos cuando han transcurrido
  40 s o más desde la emisión del token, o cuando la sesión está cerrada, son rechazados. En pruebas a los 39 s,
  el token sigue vigente y el intento se acepta si cumple las demás validaciones.
  En todos los flujos, incluido invitados, una sesión cerrada impide confirmar nuevos registros.
- **SC-005**: El 100 % de los resultados mostrados al usuario incluyen estado y motivo.
- **SC-006**: Antes de habilitar el rechazo automático por ubicación, se documentan las tasas
  de falsos positivos y falsos negativos medidas en al menos un aula y un espacio abierto reales.
- **SC-007**: En el 100 % de las pruebas con validación geográfica desactivada no se solicita
  permiso de ubicación ni se almacenan datos geográficos; la ausencia de ubicación no causa
  rechazos cuando se cumplen los demás requisitos de registro.
- **SC-008**: En el 100 % de las pruebas de clases, estudiantes autenticados fuera de la lista
  autorizada son rechazados; no pueden registrarse mediante el flujo de invitados.
- **SC-009**: En el 100 % de las pruebas de eventos, el flujo de invitados rechaza cédulas
  asociadas a cuentas y cédulas ausentes del listado de ese evento. Un invitado autorizado
  sin cuenta introduce solo su cédula; cuando cumple los demás controles, se guarda una sola
  asistencia con los datos del listado, sin pedir nombres ni crearle una cuenta.
- **SC-010**: Para cada duración configurada `T > 40 s` probada, un invitado que cumple los
  demás controles puede enviar hasta antes de `inicio + T`; al cumplirse ese límite o al
  cerrar la sesión se rechaza el envío. Las pruebas DEBEN cubrir apertura con QR vencido,
  rotación y caducidad del QR después de abrir válidamente, recarga, reloj del dispositivo
  alterado y valores de configuración inválidos. Ninguno puede ampliar el plazo original.
- **SC-011**: En el 100 % de las pruebas de consulta por rol, un docente o encargado no accede
  a actividades ajenas y un director de carrera no accede a actividades de otra carrera,
  tanto por listado como por identificador directo. Un director con alcance institucional
  puede consultar todas. Los intentos de exportación sin permiso específico se rechazan.
- **SC-012**: El 100 % de los cambios manuales de asistencia probados generan un registro
  de auditoría con responsable, actividad, fecha, razón y valores anterior y nuevo, cuyo
  acceso también respeta el ámbito del rol.
- **SC-013**: En los escenarios de vencimiento de cinco años calendario desde el cierre de
  actividad, las consultas identificadas quedan bloqueadas y el tratamiento elimina datos y
  vínculos personales de la actividad y de su auditoría, conserva solo agregados no
  identificables y no afecta a cuentas ni datos de otras actividades vigentes. Repetir el
  tratamiento no duplica estadísticas; una restauración no vuelve a habilitar datos vencidos.

## Assumptions

- Los estudiantes, docentes, espacios y actividades existen como datos semilla; su gestión
  completa corresponde a las especificaciones futuras `002-autenticacion-identidades` y
  `003-espacios-eventos`.
- Las pruebas de clases utilizan estudiantes y listas autorizadas como datos semilla. Los
  eventos incorporan la carga de externos por el encargado y el registro mínimo de invitados.
- La gestión de datos semilla (personas, espacios, actividades) pasa a la especificación 002;
  dispositivo y canal móvil se rigen por FR-023..FR-026 de 002.
- Los participantes UNL usan credenciales; el proveedor institucional está pendiente de
  confirmación. Las pruebas con cuentas semilla no demuestran integración con CAS real.
- La identificación de invitados usa únicamente cédula contra listado por decisión expresa
  del usuario. No se añaden comprobaciones presenciales ni credenciales de invitado. La
  revisión de la desviación del Principio VI se conserva como condición de integración,
  sin presumir la aprobación de otro integrante ni modificar la constitución.
- Los ámbitos de consulta y el plazo de cinco años desde el cierre definitivo, seguido de
  anonimización para estadísticas, están definidos. Los roles de director no conceden
  exportación. El diseño técnico concreta el tratamiento de respaldos sin afirmar una
  validación institucional de la base legal.
- Cuando se habilita la validación geográfica, la zona se representa como punto + radio;
  los polígonos quedan para una versión posterior.
- La validación geográfica es opcional por sesión y está desactivada por defecto. Un QR
  vigente por sí solo no demuestra presencia física; el GPS tampoco garantiza presencia
  dentro del aula ni descarta ubicaciones simuladas.
- Este incremento incluye la precarga de cédulas y nombres por el encargado y el registro
  de invitados sin cuenta mediante solo cédula, con mayor tolerancia de envío, por ampliación
  de alcance confirmada el 2026-10-19. La gestión más
  amplia de invitados podrá desarrollarse en `004-invitados`, sin duplicar estas reglas.
- El control de permanencia (segundo escaneo) queda fuera de este incremento.

### Fuera del alcance de este incremento

Polígonos, control de permanencia, integración con el sistema académico real,
reconocimiento facial, detección avanzada de GPS simulado y notificaciones.

## Clarifications

> La sesión del 2026-10-10 contiene propuestas del equipo a partir del documento de contexto.
> Las respuestas de la sesión del 2026-10-19 registran decisiones confirmadas por el usuario
> durante la clarificación frente a la constitución 2.0.0. Las propuestas geográficas previas
> solo aplican a sesiones con validación geográfica habilitada; los valores experimentales
> siguen sujetos a validación.

### Session 2026-10-10

- Q: ¿Cuánto dura cada token QR y hay margen de gracia? → A: Rotación cada 30 s; cada token
  es válido 30 s más un margen de gracia de 10 s para compensar la latencia de red y la
  concesión de permisos. La validación usa la hora del servidor.
- Q: ¿Qué hacer cuando la precisión del GPS es insuficiente? → A: Umbral por defecto 30 m
  (configurable por sesión). Si la precisión es peor que el umbral, o si el círculo de
  incertidumbre cruza el borde de la zona, se pide repetir; al segundo intento igual, el
  resultado es *pendiente de revisión*. Nunca se acepta automáticamente.
- Q: ¿Qué antigüedad máxima se acepta para la ubicación? → A: 60 segundos respecto a la hora
  del servidor al recibir el intento.
- Q: ¿Un token se invalida tras el primer escaneo? → A: No. Todos los participantes pueden
  usar el mismo token durante su vigencia; la unicidad es por persona y sesión.
- Q: ¿Qué radio por defecto usa un aula? → A: 20 m (rango de pruebas 15–25 m), heredado del
  espacio y ajustable por el docente en la sesión. Es un valor experimental, no una garantía.

### Session 2026-10-19

- Q: ¿Qué papel debe tener la geolocalización en el primer incremento? → A: Incluir GPS como
  control configurable por sesión, desactivado por defecto (opción B).
- Q: ¿Durante cuánto tiempo debe aceptar el servidor cada token QR desde su emisión? → A:
  Renovar cada 30 s y aceptar cada token durante 40 s en total (opción A), según la hora de
  recepción del servidor. Cerrar la sesión impide nuevos registros inmediatamente.
- Q: ¿Qué estudiantes pueden registrar asistencia en una sesión? → A: Se requieren dos
  modalidades: clases restringidas a sus estudiantes autorizados y eventos con participantes
  UNL autenticados mediante credenciales y externos identificados por cédula. El encargado
  carga los datos de externos para registrar automáticamente su asistencia al ingresar una
  cédula válida, sujeto a las demás verificaciones. El rol invitado necesita mayor tolerancia
  para enviar los datos. La propuesta inicial de autorregistro para externos no precargados
  queda sustituida por la respuesta siguiente: solo se admiten invitados precargados sin cuenta.
- Q: ¿Cómo se comprobará que la cédula ingresada pertenece al invitado? → A: El encargado
  debe precargar únicamente los datos de invitados del evento. El participante introduce
  su cédula y el sistema comprueba si tiene cuenta; si la tiene, debe usar credenciales.
  Solo si no tiene cuenta y la cédula está en el listado del evento, se guarda automáticamente
  la asistencia con los datos precargados. No se permite autorregistro fuera del listado.
  Esta comprobación valida la autorización del listado, sin acreditar por sí sola titularidad.
- Q: ¿Cuánto tiempo tendrá el invitado para enviar su cédula después de abrir el formulario
  con un QR vigente? → A: Usar una duración configurable por evento mediante
  `tolerancia_invitado_segundos`, en lugar de fijar 2 o 5 minutos. El encargado configura una
  duración superior a los 40 s del flujo ordinario; el intervalo se mide en el servidor desde
  la apertura validada del formulario y termina antes si se cierra la sesión.

### Aclaraciones durante planificación 2026-10-19

- Q: ¿Quién podrá consultar y exportar los datos, durante cuánto tiempo deben conservarse
  y cómo se eliminarán? → A: El docente o encargado consulta sus propias clases y eventos;
  el director con alcance institucional consulta todos; el director de carrera consulta
  solo los de su carrera y sus paralelos. Los datos se almacenan en una base relacional,
  permiten auditoría y se conservan provisionalmente cinco años. El inicio de cómputo,
  la eliminación y la exportación por directores no quedaron definidos en esa primera
  respuesta; las decisiones posteriores se registran a continuación.
- Q: ¿El encargado comprobará presencialmente la identidad del invitado antes de confirmar
  asistencia? → A: No. Solo se comprueba la cédula contra el listado precargado del evento,
  además de ausencia de cuenta y demás controles del flujo. Se mantiene documentada la
  limitación de titularidad y la desviación constitucional pendiente de revisión de equipo.
- Q: ¿Qué política se aplica al finalizar los cinco años contados desde el cierre de la clase
  o evento? → A: Anonimizar los datos al cumplir cinco años para conservar estadísticas.
