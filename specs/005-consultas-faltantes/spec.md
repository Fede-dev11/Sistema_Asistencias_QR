# Especificación de funcionalidad: Consultas faltantes, contrato completo y evidencia de GPS

**Rama de la funcionalidad**: `feature/005-consultas-faltantes` (desde `feature/004-autenticacion-sso`)

**Creado**: 2026-10-28

**Estado**: Borrador — correcciones detectadas al revisar si el backend estaba listo para el frontend
(2026-10-26). No cambia reglas de negocio de 001–004; completa lo que el frontend necesita leer.

**Entrada**: "Genera un plan para realizar las correspondientes correcciones" sobre las brechas
señaladas: recursos que se escriben pero no se leen, esquema `Activity` incompleto en el contrato
y falta de registro de la evidencia de GPS en campo.

## Problema que resuelve

Varias pantallas no pueden mostrar el estado actual antes de editarlo: el listado de invitados de
un evento, su configuración de invitados, el horario de una clase, la configuración GPS de una
sesión y los matriculados y docentes de un paralelo. El contrato no describe campos que la API ya
devuelve, así que los tipos generados para web y móvil no los tienen. Y la validación geográfica
automática (modo AUTO de 001) nunca se activa porque no hay forma de registrar la evidencia del
ensayo en campo salvo con SQL.

## User Scenarios & Testing *(obligatorio)*

### Historia de usuario 1 — Ver el estado actual antes de editar (Prioridad: P1)

El responsable abre un evento, una clase o una sesión y ve lo que está configurado hoy; el
administrador abre un paralelo y ve quiénes están matriculados y qué docentes lo dictan.

**Prueba independiente**: guardar un listado de invitados, un horario, una configuración GPS y
una matrícula, y leerlos de vuelta con los mismos valores.

**Escenarios de aceptación**:

1. **Dado** un evento cerrado con listado, **Cuando** su responsable lo consulta, **Entonces** ve
   cédula, nombres y estado activo de cada invitado, paginado.
2. **Dado** un evento, **Cuando** su responsable consulta la configuración de invitados,
   **Entonces** ve si están habilitados y la tolerancia en segundos.
3. **Dado** una clase con horario, **Cuando** su responsable lo consulta, **Entonces** ve días,
   horas, rango de fechas y espacio; una clase sin horario responde que no tiene.
4. **Dado** una sesión, **Cuando** su responsable consulta la configuración GPS, **Entonces** ve si
   está habilitada, radio, precisión máxima, antigüedad máxima, modo (OFF, REVIEW_ONLY, AUTO) y si
   hay evidencia de campo vigente.
5. **Dado** un paralelo, **Cuando** el administrador lo consulta, **Entonces** ve la lista paginada
   de matriculados (cédula, nombres, vigente) y de docentes (correo, nombres, vigente).
6. **Dado** un usuario que no es responsable de la actividad (o no es administrador para los
   paralelos), **Cuando** consulta, **Entonces** se rechaza; los directores leen dentro de su
   ámbito igual que en 001 y no ven cédulas de invitados fuera de él.

### Historia de usuario 2 — Contrato que coincide con lo que responde la API (Prioridad: P1)

Los clientes web y móvil usan los campos reales de una actividad (`acceso`, `espacio_id`,
`metodo_verificacion` y los que la API ya devuelva) con tipos generados.

**Prueba independiente**: una prueba de contrato valida la respuesta real de `GET /activities/{id}`
de una clase y de un evento contra el esquema, que no admite campos no declarados.

**Escenarios de aceptación**:

1. **Dado** una actividad, **Cuando** se consulta, **Entonces** cada campo de la respuesta está
   declarado en el contrato con su tipo y nulabilidad.
2. **Dado** el contrato, **Cuando** se regeneran los tipos, **Entonces** web y móvil compilan.

### Historia de usuario 3 — Registrar la evidencia del ensayo de GPS en campo (Prioridad: P2)

El administrador registra para un espacio el resultado de un ensayo real: referencia del
documento, fecha, responsable y tasas de falsos positivos y negativos. Desde entonces, las
sesiones de ese espacio con GPS habilitado usan el modo AUTO.

**Prueba independiente**: sin evidencia, habilitar GPS deja la sesión en REVIEW_ONLY; tras
registrar evidencia, una nueva configuración queda en AUTO y lo informa.

**Escenarios de aceptación**:

1. **Dado** un administrador, **Cuando** registra una evidencia válida para un espacio, **Entonces**
   queda con versión siguiente, auditada, y aparece en la consulta del espacio.
2. **Dado** tasas fuera de 0–1, referencia vacía o fecha futura, **Cuando** se envían,
   **Entonces** se rechazan con un mensaje claro.
3. **Dado** un espacio con evidencia, **Cuando** el responsable habilita GPS en una sesión
   programada de ese espacio, **Entonces** el modo es AUTO; sin evidencia, REVIEW_ONLY (001).
4. **Dado** un usuario que no es administrador, **Cuando** intenta registrar evidencia,
   **Entonces** se rechaza.
5. **Dado** una evidencia registrada, **Cuando** se quiere modificar, **Entonces** no se permite:
   se registra una nueva versión (historial inmutable).

### Edge Cases

- Listado de invitados vacío → página vacía, no error.
- Evento abierto → la consulta del listado responde vacío (no tiene listado).
- Sesión activa o cerrada → su configuración GPS se puede leer aunque ya no se pueda editar.
- Paralelo sin matriculados → página vacía.
- Evidencia de un espacio sin coordenadas → se rechaza (no hay zona que evaluar).

## Requirements *(obligatorio)*

- **FR-001**: El responsable DEBE poder leer el listado de invitados (paginado) y la configuración de invitados de su evento.
- **FR-002**: El responsable DEBE poder leer el horario de su clase.
- **FR-003**: El responsable DEBE poder leer la configuración GPS de cada sesión de su actividad, incluido el modo efectivo y si hay evidencia vigente.
- **FR-004**: El administrador DEBE poder leer los matriculados y docentes de un paralelo, paginados.
- **FR-005**: Las lecturas DEBEN respetar el ámbito de 001 (responsable; directores solo lectura dentro de su ámbito; administrador para datos académicos).
- **FR-006**: El esquema `Activity` del contrato DEBE declarar todos los campos que la API devuelve y una prueba DEBE verificarlo con respuestas reales.
- **FR-007**: El administrador DEBE poder registrar evidencias de GPS por espacio (referencia, fecha, tasas de falsos positivos y negativos entre 0 y 1), inmutables y versionadas, y consultarlas.
- **FR-008**: La configuración GPS de una sesión DEBE usar AUTO solo si el espacio tiene evidencia registrada; las reglas de repetición y revisión de 001 no cambian.
- **FR-009**: Toda alta de evidencia DEBE auditarse.

## Success Criteria *(obligatorio)*

- **SC-001**: El 100 % de los recursos que la API permite modificar tienen una forma de leer su estado actual (comprobado listando operaciones de escritura sin lectura en el contrato: resultado cero).
- **SC-002**: La prueba de contrato de `Activity` pasa con clases y eventos reales.
- **SC-003**: En las pruebas, una sesión con GPS habilitado queda en AUTO con evidencia y en REVIEW_ONLY sin ella en el 100 % de los casos.

## Assumptions

- Quién registra evidencias: ADMINISTRADOR (dato institucional, no del docente).
- La evidencia no se edita ni se borra; una corrección es una versión nueva.
- La cédula de invitados se muestra solo al responsable y a directores dentro de su ámbito (como el resto de datos nominales de 001).

### Fuera del alcance

Pantallas de frontend que usan estas lecturas (las hace el equipo de frontend), nuevos reportes, cambios de reglas de asistencia.
