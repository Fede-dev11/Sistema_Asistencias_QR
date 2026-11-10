# Especificación de funcionalidad: Faltas, estadísticas, asistencia manual, historial, ciclo automático y cuenta

**Rama de la funcionalidad**: `feature/003-ausencias-reportes-cuenta`

**Creado**: 2026-10-25

**Estado**: Borrador — alcance elegido por el usuario el 2026-10-25 ("toda la parte backend
que falta"): faltas y estadísticas, asistencia manual y justificaciones, historial del
participante, ciclo automático de sesiones y gestión de la cuenta propia.

**Entrada**: "Necesito toda la parte backend que falta" tras 001 (registro con QR/GPS) y 002
(gestión académica, espacios, actividades, eventos, dispositivo).

Depende de 001 y 002. No cambia sus reglas de registro, unicidad, revisión, ámbito,
auditoría ni conservación; las reutiliza.

## Problema que resuelve

Hoy el sistema registra quién asistió, pero no quién faltó, ni porcentajes, ni permite al
docente corregir casos reales (estudiante sin teléfono, falta justificada). El estudiante no
puede ver su propio historial, las sesiones dependen de que el docente las abra y cierre a
mano, y la cuenta no permite cambiar la contraseña ni mantener la sesión de la app sin
volver a ingresar cada 8 horas.

## User Scenarios & Testing *(obligatorio)*

### Historia de usuario 1 — Faltas al cerrar la sesión y estadísticas (Prioridad: P1)

Al cerrarse una sesión de clase, cada estudiante autorizado vigente sin asistencia aceptada
queda registrado como ausente. El docente y los directores, según su ámbito de 001, consultan
porcentajes de asistencia por estudiante, por clase y por paralelo en un periodo, con
indicación de quienes están bajo el umbral de asistencia.

**Por qué esta prioridad**: es el resultado académico que el docente necesita.

**Prueba independiente**: clase con 3 estudiantes y 4 sesiones cerradas; un estudiante asiste
a 3, otro a 1 con 1 justificada; verificar ausencias, porcentajes y la marca bajo umbral.

**Escenarios de aceptación**:

1. **Dado** una sesión de clase activa con 30 autorizados y 25 asistencias aceptadas, **Cuando**
   se cierra, **Entonces** quedan 5 ausencias registradas, con la lista vigente en ese momento.
2. **Dado** un intento pendiente de revisión al cerrar, **Cuando** se cierra la sesión,
   **Entonces** el estudiante figura como ausente; si luego se aprueba la revisión, la ausencia
   se elimina y la asistencia cuenta.
3. **Dado** un estudiante quitado de la lista después del cierre, **Cuando** se consultan las
   estadísticas, **Entonces** sus ausencias anteriores se conservan.
4. **Dado** sesiones canceladas o aún no cerradas, **Cuando** se calculan porcentajes,
   **Entonces** no cuentan.
5. **Dado** una clase con sesiones cerradas, **Cuando** el responsable consulta las estadísticas,
   **Entonces** obtiene por estudiante: sesiones computables, asistencias, ausencias, ausencias
   justificadas, porcentaje de asistencia y marca `bajo_umbral` si el porcentaje es menor al umbral.
6. **Dado** un director de carrera, **Cuando** consulta estadísticas por paralelo en un periodo,
   **Entonces** solo obtiene paralelos de su carrera; un director institucional obtiene todos;
   un docente solo sus clases.
7. **Dado** un evento, **Cuando** se cierra su sesión, **Entonces** no se generan ausencias
   (los eventos solo reportan asistentes).

---

### Historia de usuario 2 — Asistencia manual y justificaciones (Prioridad: P1)

El responsable puede registrar manualmente la asistencia de un estudiante autorizado (por
ejemplo, sin teléfono o con la app fallando) indicando una razón, y justificar una ausencia.
El estudiante puede enviar una solicitud de justificación con un motivo; el responsable la
aprueba o rechaza.

**Por qué esta prioridad**: sin corrección manual el registro deja fuera casos reales y la
constitución exige que toda modificación manual quede auditada.

**Prueba independiente**: registrar a mano a un ausente con razón; justificar a otro; un
estudiante envía una justificación y el docente la rechaza; verificar estadísticas y auditoría.

**Escenarios de aceptación**:

1. **Dado** una sesión activa o cerrada hace menos de 7 días, **Cuando** el responsable registra
   manualmente a un estudiante autorizado con razón, **Entonces** se crea una asistencia de forma
   MANUAL, se elimina su ausencia si existía y queda auditado quién, cuándo y por qué.
2. **Dado** un estudiante ya registrado en la sesión, **Cuando** se intenta registrarlo a mano,
   **Entonces** se rechaza como duplicado.
3. **Dado** una sesión cerrada hace más de 7 días, **Cuando** se intenta registrar o justificar,
   **Entonces** se rechaza con "El plazo de corrección de la sesión venció".
4. **Dado** una ausencia, **Cuando** el responsable la justifica con razón, **Entonces** la
   ausencia queda JUSTIFICADA, auditada, y cuenta como justificada en las estadísticas.
5. **Dado** un estudiante con una ausencia, **Cuando** envía una solicitud con motivo (1 a 2 000
   caracteres) dentro de 7 días del cierre, **Entonces** queda PENDIENTE y visible para el
   responsable; solo una solicitud pendiente por ausencia.
6. **Dado** una solicitud pendiente, **Cuando** el responsable la aprueba o rechaza con razón,
   **Entonces** la decisión queda registrada e inmutable y el estudiante la ve en su historial.
7. **Dado** un usuario que no es responsable de la actividad, **Cuando** intenta registrar,
   justificar o decidir, **Entonces** se rechaza.
8. **Dado** una asistencia manual, **Cuando** se consulta el listado o el CSV de la sesión,
   **Entonces** aparece con forma MANUAL y razón.

---

### Historia de usuario 3 — Historial del participante (Prioridad: P2)

El participante autenticado consulta en la app sus asistencias, ausencias y justificaciones, y
su porcentaje por clase.

**Por qué esta prioridad**: transparencia para el estudiante; no bloquea al docente.

**Prueba independiente**: un estudiante con dos clases consulta su historial y ve lo mismo que
el docente ve de él.

**Escenarios de aceptación**:

1. **Dado** un estudiante autenticado, **Cuando** consulta su historial, **Entonces** ve por
   sesión cerrada: actividad, fecha, resultado (asistió, ausente, justificada, pendiente de
   revisión) y, por clase, su porcentaje y marca bajo umbral; paginado.
2. **Dado** cualquier usuario, **Cuando** consulta el historial, **Entonces** solo obtiene sus
   propios datos; nunca los de otra persona.
3. **Dado** una actividad vencida y anonimizada (001 FR-025), **Cuando** se consulta el
   historial, **Entonces** esa actividad ya no aparece.

---

### Historia de usuario 4 — Ciclo automático de sesiones (Prioridad: P2)

El responsable puede activar el ciclo automático en una actividad: el sistema inicia cada
sesión programada a su hora de inicio y la cierra a su hora de fin. Sin ciclo automático, una
sesión activa olvidada se cierra sola 2 horas después de su hora de fin.

**Por qué esta prioridad**: evita sesiones olvidadas abiertas o sin abrir; el registro funciona
igual a mano.

**Prueba independiente**: con reloj de prueba, activar el ciclo en una actividad y avanzar la
hora; verificar inicio, cierre y ausencias generadas.

**Escenarios de aceptación**:

1. **Dado** una actividad con ciclo automático y una sesión programada, **Cuando** llega su hora
   de inicio, **Entonces** la sesión pasa a activa (con su método de verificación y QR si aplica).
2. **Dado** una sesión activa con ciclo automático, **Cuando** llega su hora de fin, **Entonces**
   se cierra y se generan las ausencias.
3. **Dado** una actividad sin ciclo automático y una sesión activa, **Cuando** pasan 2 horas de su
   hora de fin, **Entonces** se cierra automáticamente.
4. **Dado** una sesión cuyo inicio automático falla (por ejemplo, espacio sin coordenadas con
   método GPS), **Cuando** se intenta iniciar, **Entonces** queda programada y el motivo se registra
   en auditoría; no se reintenta en bucle más de una vez por minuto.
5. **Dado** dos instancias del servidor, **Cuando** ejecutan el ciclo a la vez, **Entonces** cada
   sesión se inicia o cierra una sola vez.
6. **Dado** cualquier inicio o cierre automático, **Cuando** se consulta la auditoría, **Entonces**
   figura como acción del sistema con la hora.

---

### Historia de usuario 5 — Cuenta propia y sesión prolongada (Prioridad: P2)

> **Reemplazada en parte por la feature 004 (SSO, FR-017)**: el cambio de contraseña propio (`PUT /me/password`), el restablecimiento por el administrador (`POST /admin/accounts/{id}/password-reset`) y el cambio obligatorio fueron retirados; la renovación de sesión se mantiene.

El usuario cambia su contraseña, el administrador puede restablecerla y la app mantiene la
sesión renovando el acceso sin pedir credenciales cada 8 horas, hasta un máximo de 30 días.

**Por qué esta prioridad**: necesario para operar con usuarios reales y la app móvil.

**Prueba independiente**: cambiar contraseña, comprobar que las demás sesiones se cierran;
renovar la sesión hasta el límite de 30 días.

**Escenarios de aceptación**:

1. **Dado** un usuario autenticado, **Cuando** cambia su contraseña dando la actual y una nueva de
   al menos 12 caracteres distinta de la actual, **Entonces** se actualiza, se cierran sus demás
   sesiones y la actual sigue válida.
2. **Dado** una contraseña actual incorrecta, **Cuando** intenta cambiarla, **Entonces** se
   rechaza y cuenta para el límite de intentos de login de 001.
3. **Dado** un administrador, **Cuando** restablece la contraseña de una cuenta, **Entonces** se
   genera una contraseña temporal que se muestra una sola vez, se cierran las sesiones de esa
   cuenta, se audita y la cuenta debe cambiarla en el siguiente ingreso.
4. **Dado** una cuenta que debe cambiar la contraseña, **Cuando** ingresa, **Entonces** solo puede
   usar el cambio de contraseña y cerrar sesión hasta cambiarla.
5. **Dado** una sesión vigente, **Cuando** la app pide renovarla, **Entonces** recibe un acceso
   nuevo por 8 horas y el anterior deja de servir.
6. **Dado** una sesión iniciada hace más de 30 días o revocada, **Cuando** se intenta renovar,
   **Entonces** se rechaza y se piden credenciales.
7. **Dado** el usuario autenticado, **Cuando** consulta su perfil, **Entonces** ve nombres, correo,
   roles y carreras/paralelos asociados, nunca su contraseña ni secretos.

### Edge Cases

- Cierre manual y automático a la vez → la sesión se cierra una vez; las ausencias no se duplican.
- Asistencia manual sobre un estudiante con intento pendiente de revisión → se registra la manual y
  el pendiente queda resuelto como rechazado por "registro manual" con auditoría.
- Asistencia manual y regla de dispositivo (002) → no aplica: no hay dispositivo.
- Cambio de horario o de lista tras cerrar sesiones → no recalcula ausencias pasadas.
- Umbral de asistencia → configurable por actividad, por defecto 70 %.
- Actividad sin sesiones cerradas → porcentaje "sin datos", no 0 %.

## Requirements *(obligatorio)*

### Requisitos funcionales

- **FR-001**: Al cerrar una sesión de CLASE (manual, automático o por cierre de actividad), el sistema
  DEBE registrar una ausencia por cada estudiante autorizado vigente sin asistencia aceptada, una sola
  vez, en la misma transacción del cierre.
- **FR-002**: La aprobación posterior de una revisión o una asistencia manual DEBE eliminar la ausencia
  correspondiente.
- **FR-003**: El sistema DEBE calcular por estudiante y actividad: sesiones computables (cerradas, no
  canceladas, con el estudiante autorizado al cierre), asistencias, ausencias, ausencias justificadas,
  porcentaje = asistencias / computables y `bajo_umbral` respecto al umbral de la actividad (defecto 70 %).
- **FR-004**: Las estadísticas DEBEN ofrecerse por actividad, por paralelo y por carrera en un periodo,
  respetando el ámbito de consulta de 001 (FR-021) y sin conceder exportación a directores.
- **FR-005**: El responsable DEBE poder registrar asistencia MANUAL de un estudiante autorizado con razón
  obligatoria (1–2 000) mientras la sesión esté activa o hasta 7 días después de su cierre.
- **FR-006**: El responsable DEBE poder justificar una ausencia con razón en el mismo plazo.
- **FR-007**: El estudiante DEBE poder solicitar la justificación de su ausencia con motivo dentro de 7
  días del cierre; una solicitud pendiente por ausencia; el responsable la decide con razón; la
  decisión es inmutable.
- **FR-008**: Toda asistencia manual, justificación y decisión DEBE auditarse con responsable, fecha,
  razón, valor anterior y nuevo (Principio VIII).
- **FR-009**: El participante DEBE poder consultar solo su propio historial paginado y sus porcentajes.
- **FR-010**: Una actividad DEBE poder activar `ciclo_automatico`; el sistema DEBE iniciar y cerrar sus
  sesiones según su ventana, y DEBE cerrar cualquier sesión activa 2 horas después de su hora de fin.
- **FR-011**: El ciclo automático DEBE ser seguro ante varias instancias y reintentos (una transición
  por sesión) y auditarse como acción del sistema.
- **FR-012**: El usuario DEBE poder cambiar su contraseña (mínimo 12 caracteres, distinta de la actual),
  cerrando sus demás sesiones.
- **FR-013**: El administrador DEBE poder restablecer contraseñas generando una temporal de un solo uso
  visible una vez y forzando el cambio en el siguiente ingreso.
- **FR-014**: El sistema DEBE permitir renovar una sesión vigente por 8 horas con rotación del secreto,
  hasta 30 días desde el login original.
- **FR-015**: El sistema DEBE ofrecer `GET /me` con el perfil propio sin secretos.
- **FR-016**: Ausencias, justificaciones y asistencias manuales DEBEN anonimizarse con la política de
  cinco años de 001.

### Privacidad

Datos nuevos: ausencias, motivos de justificación (pueden contener datos de salud: se muestran solo
al estudiante, al responsable y a directores según ámbito, y no se exportan en CSV; el CSV exporta
solo el estado JUSTIFICADA). Conservación y eliminación: cinco años desde el cierre de la actividad
(001). Base legal LOPDP pendiente de validación institucional.

### Entidades clave

- **Ausencia**: sesión, persona, estado (AUSENTE / JUSTIFICADA), creada al cerrar.
- **Solicitud de justificación**: ausencia, motivo, estado (PENDIENTE / APROBADA / RECHAZADA),
  decisor, razón, fechas.
- **Asistencia**: añade forma MANUAL con razón.
- **Actividad**: añade `ciclo_automatico` y `umbral_asistencia`.
- **Sesión de acceso**: añade `login_original_en` y la marca de cambio de contraseña obligatorio en la cuenta.

## Success Criteria *(obligatorio)*

- **SC-001**: En las pruebas, el número de ausencias de una sesión cerrada es exactamente
  autorizados vigentes − asistencias aceptadas, también con cierres simultáneos.
- **SC-002**: Los porcentajes de un caso de prueba de 3 estudiantes × 10 sesiones coinciden al 100 %
  con el cálculo manual esperado.
- **SC-003**: Las estadísticas de una carrera con 1 000 estudiantes y un semestre de sesiones se
  obtienen en menos de 3 segundos.
- **SC-004**: El 100 % de asistencias manuales, justificaciones y decisiones tienen registro de
  auditoría con razón.
- **SC-005**: Con reloj de prueba, el 100 % de las sesiones con ciclo automático se inician y cierran
  en el minuto correcto, y ninguna se transiciona dos veces con dos ejecutores concurrentes.
- **SC-006**: En pruebas con reloj simulado, una sesión renovada antes de cada vencimiento de 8 horas
  sigue válida hasta el día 30 y la renovación se rechaza a partir de ese límite.
- **SC-007**: Ninguna prueba de autorización permite ver historial o estadísticas fuera del ámbito.

## Assumptions

- Plazo de corrección y de justificación: 7 días desde el cierre (valor por defecto configurable).
- Umbral de asistencia por defecto 70 %, configurable por actividad.
- El cierre automático de seguridad (2 horas tras la hora de fin) aplica a todas las sesiones.
- Las justificaciones son solo texto; no se adjuntan archivos en este incremento.
- La renovación de sesión reutiliza el modelo de secretos opacos de 001 (no se adoptan JWT).
- Restablecer por correo electrónico queda fuera: no hay servicio de correo.

### Fuera del alcance

Adjuntos de justificación, notificaciones, restablecimiento por correo, CAS, reportes gráficos.
