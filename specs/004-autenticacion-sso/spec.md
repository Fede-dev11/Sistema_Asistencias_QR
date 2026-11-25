# Especificación de funcionalidad: Servicio de autenticación universitaria simulado (inicio de sesión único)

**Rama de la funcionalidad**: `feature/004-autenticacion-sso`

**Creado**: 2026-10-26

**Estado**: Borrador — decisiones aclaradas con el usuario el 2026-10-26. Introducir un servicio
separado requiere justificación (Principio II) y simular CAS 3.0 es una hipótesis mientras la UNL
no confirme el protocolo (Principio V); ambas quedan registradas para revisión del equipo
(ADR 0006 propuesto). La constitución no se modifica.

**Entrada**: "Necesito un microservicio de autenticación para simular la conexión con el servicio
institucional; que redirija a la aplicación con los roles y datos del usuario. El usuario se
registra con sus credenciales universitarias (correo y contraseña) y esto genera un acceso que
sirve para la app y también para otros servicios de la universidad."

Depende de 001–003. Sustituye el inicio de sesión local por correo y contraseña de GeoAsist para
las cuentas institucionales; las reglas de asistencia, ámbito, auditoría y conservación no cambian.

## Problema que resuelve

GeoAsist guarda hoy contraseñas de cuentas ficticias y las valida él mismo. En producción la
identidad vendrá del servicio institucional de la UNL, que GeoAsist no controla. Sin un servicio
que se comporte como ese sistema externo, el equipo no puede probar el flujo real (salir de la app,
autenticarse en otro sitio y volver con la identidad), ni demostrar que una sola sesión sirve para
varios servicios de la universidad, ni preparar el cambio al servicio real sin reescribir GeoAsist.

## User Scenarios & Testing *(obligatorio)*

### Historia de usuario 1 — Ingresar a GeoAsist web con la cuenta universitaria (Prioridad: P1)

La persona pulsa "Ingresar con cuenta universitaria" en GeoAsist. Es llevada a la página de inicio
de sesión del servicio universitario, escribe su correo institucional y contraseña, y vuelve
automáticamente a GeoAsist ya autenticada, con sus roles y datos.

**Por qué esta prioridad**: es el camino de entrada de todos los usuarios con cuenta.

**Prueba independiente**: con el servicio y GeoAsist levantados, ingresar como docente ficticio y
comprobar que GeoAsist muestra sus actividades sin haber pedido la contraseña en GeoAsist.

**Escenarios de aceptación**:

1. **Dado** un usuario activo del servicio universitario, **Cuando** ingresa correo y contraseña
   correctos, **Entonces** vuelve a GeoAsist autenticado con sus roles y nombres.
2. **Dado** credenciales incorrectas, **Cuando** las envía, **Entonces** el servicio muestra "Correo o
   contraseña incorrectos" sin revelar cuál falló y no vuelve a GeoAsist.
3. **Dado** un usuario desactivado, **Cuando** intenta ingresar, **Entonces** se rechaza con un mensaje
   comprensible.
4. **Dado** demasiados intentos fallidos para el mismo correo en un minuto, **Cuando** intenta otra
   vez, **Entonces** se bloquea temporalmente con indicación de cuándo reintentar.
5. **Dado** un usuario autenticado por primera vez en GeoAsist, **Cuando** vuelve del servicio,
   **Entonces** GeoAsist crea su cuenta local con los roles recibidos y la enlaza a la persona
   existente con la misma cédula (por ejemplo, un estudiante ya matriculado).
6. **Dado** un usuario cuyos roles cambiaron en el servicio, **Cuando** vuelve a ingresar,
   **Entonces** GeoAsist actualiza sus roles; los permisos se aplican con los roles nuevos.
7. **Dado** el regreso a GeoAsist, **Cuando** la persona usa la aplicación, **Entonces** la sesión de
   GeoAsist dura y se renueva como hasta ahora (8 horas, renovable hasta 30 días).

---

### Historia de usuario 2 — Una sola sesión para varios servicios de la universidad (Prioridad: P1)

Tras ingresar una vez, la persona abre otro servicio universitario registrado (u otra pestaña de
GeoAsist) y entra sin volver a escribir su contraseña. Cada servicio recibe su propia prueba de
identidad, de un solo uso, que no sirve para otro servicio.

**Por qué esta prioridad**: es el pedido explícito ("acceso a la app y a otros servicios").

**Prueba independiente**: registrar un segundo servicio de ejemplo, ingresar en GeoAsist y luego
abrir el servicio de ejemplo sin que pida contraseña.

**Escenarios de aceptación**:

1. **Dado** una sesión universitaria vigente, **Cuando** un servicio registrado redirige a la persona
   al servicio universitario, **Entonces** vuelve al servicio de inmediato, autenticada, sin pedir
   credenciales.
2. **Dado** una prueba de identidad emitida para GeoAsist, **Cuando** otro servicio intenta validarla,
   **Entonces** se rechaza.
3. **Dado** una prueba de identidad ya validada una vez, **Cuando** se intenta validar de nuevo,
   **Entonces** se rechaza.
4. **Dado** una prueba de identidad no validada en el tiempo permitido, **Cuando** se intenta
   validar, **Entonces** se rechaza como vencida.
5. **Dado** un servicio o dirección de retorno no registrados, **Cuando** se pide iniciar sesión hacia
   ellos, **Entonces** el servicio universitario no redirige y muestra un error (no hay redirección
   abierta).
6. **Dado** que la persona cierra la sesión universitaria, **Cuando** un servicio la redirige otra vez,
   **Entonces** se le piden credenciales. Las sesiones ya abiertas en cada servicio expiran según
   las reglas de ese servicio (GeoAsist ofrece además cerrar ambas desde su botón de salida).

---

### Historia de usuario 3 — Ingresar desde la app móvil (Prioridad: P1)

En la app, "Ingresar con cuenta universitaria" abre el navegador del sistema con la página del
servicio universitario; al terminar, el navegador devuelve a la app, que queda autenticada.

**Por qué esta prioridad**: el registro de asistencia ocurre en la app (002).

**Prueba independiente**: en un emulador, ingresar como estudiante ficticio y ver sus sesiones
activas.

**Escenarios de aceptación**:

1. **Dado** la app, **Cuando** la persona completa el inicio de sesión en el navegador del sistema,
   **Entonces** vuelve a la app autenticada; la app nunca ve ni guarda la contraseña.
2. **Dado** que la persona cancela o cierra el navegador, **Cuando** vuelve a la app, **Entonces** la
   app sigue sin sesión y lo indica.
3. **Dado** una sesión universitaria vigente en el navegador del sistema, **Cuando** ingresa desde la
   app, **Entonces** no se le pide contraseña.
4. **Dado** un retorno a la app que no corresponde a un inicio pedido por esa misma app, **Cuando**
   llega, **Entonces** se descarta.

---

### Historia de usuario 4 — Administrar los usuarios simulados (Prioridad: P2)

Un administrador del servicio universitario simulado crea, edita, activa o desactiva usuarios,
asigna roles y carreras, restablece contraseñas e importa usuarios por CSV. Los usuarios cambian su
propia contraseña en el servicio universitario.

**Por qué esta prioridad**: sin usuarios no hay inicio de sesión, pero las semillas cubren el inicio.

**Prueba independiente**: importar un CSV de usuarios, ingresar con uno de ellos en GeoAsist y
desactivarlo; el siguiente ingreso se rechaza.

**Escenarios de aceptación**:

1. **Dado** un administrador del servicio, **Cuando** crea un usuario con correo único, cédula válida,
   nombres, roles y carreras, **Entonces** puede ingresar con la contraseña inicial y debe cambiarla.
2. **Dado** una cédula o un correo ya existentes, **Cuando** se intenta crear otro usuario con ellos,
   **Entonces** se rechaza.
3. **Dado** un CSV con filas válidas e inválidas, **Cuando** se importa, **Entonces** se aplican las
   válidas, se informa cada fila rechazada y reimportarlo no duplica.
4. **Dado** un usuario, **Cuando** el administrador restablece su contraseña, **Entonces** se genera una
   temporal que se muestra una sola vez, se cierra su sesión universitaria y debe cambiarla al
   ingresar.
5. **Dado** un usuario autenticado en el servicio, **Cuando** cambia su contraseña, **Entonces** se
   cierran sus otras sesiones universitarias.
6. **Dado** cualquier acción administrativa, **Cuando** se consulta la auditoría del servicio,
   **Entonces** figura quién, qué y cuándo, sin contraseñas.

---

### Historia de usuario 5 — Cambiar al servicio institucional real sin reescribir GeoAsist (Prioridad: P3)

El equipo puede apuntar GeoAsist al servicio institucional de la UNL cuando exista, cambiando solo
configuración, si ese servicio usa el mismo protocolo; si usa otro, solo cambia el adaptador de
identidad de GeoAsist.

**Por qué esta prioridad**: evita retrabajo futuro; no bloquea el uso actual.

**Prueba independiente**: ejecutar las pruebas de asistencia y permisos de GeoAsist con el
adaptador sustituido por un doble de pruebas; ninguna regla de asistencia depende del servicio.

**Escenarios de aceptación**:

1. **Dado** GeoAsist configurado con otra dirección del servicio de identidad, **Cuando** se reinicia,
   **Entonces** usa esa dirección sin cambios de código.
2. **Dado** las reglas de asistencia, ámbito y conservación, **Cuando** se revisan las dependencias,
   **Entonces** ninguna depende del servicio de identidad concreto.

### Edge Cases

- El servicio universitario no responde cuando GeoAsist valida → GeoAsist muestra "No se pudo
  verificar tu identidad, intenta de nuevo" y no crea sesión.
- Atributo de rol desconocido → se ignora ese rol y se registra; si no queda ningún rol válido, la
  persona entra sin permisos de responsable (solo lo que permite "cualquier cuenta").
- Cédula recibida que no cumple el módulo 10 → GeoAsist rechaza el ingreso y lo registra; es un dato
  erróneo del servicio.
- Cédula recibida que pertenece a una persona externa (invitado) en GeoAsist → la persona pasa a
  tener cuenta; desde entonces debe usar credenciales también en eventos (regla de 001).
- Correo recibido distinto del guardado para la misma cédula → GeoAsist actualiza el correo.
- Usuario desactivado en el servicio con sesión vigente en GeoAsist → conserva su sesión de
  GeoAsist hasta que venza o la renueve; la renovación vuelve a consultar el servicio y se rechaza.
- Dos pestañas inician sesión a la vez → cada una recibe su propia prueba de identidad; ambas
  funcionan.
- Reloj desajustado entre servicios → los vencimientos los decide quien emite la prueba.

## Requirements *(obligatorio)*

### Requisitos funcionales

**Servicio universitario simulado**

- **FR-001**: El servicio DEBE autenticar con correo institucional y contraseña, y mantener una
  sesión universitaria única por navegador.
- **FR-002**: El servicio DEBE redirigir solo a servicios registrados y a direcciones de retorno
  permitidas para cada uno; cualquier otra dirección DEBE rechazarse sin redirigir.
- **FR-003**: El servicio DEBE emitir para cada retorno una prueba de identidad de un solo uso, ligada
  al servicio que la pidió, con vida corta configurable (por defecto 10 segundos).
- **FR-004**: El servicio DEBE ofrecer a los servicios registrados la validación de esa prueba de
  servidor a servidor, devolviendo el usuario y sus atributos: correo, cédula, nombres, roles,
  carreras y estado activo.
- **FR-005**: El servicio DEBE permitir cerrar la sesión universitaria y, si se indica, volver a una
  dirección registrada.
- **FR-006**: El servicio DEBE limitar los intentos fallidos por correo y por origen, y auditar
  inicios de sesión exitosos y fallidos sin registrar contraseñas ni pruebas de identidad.
- **FR-007**: El servicio DEBE guardar contraseñas solo con hash adaptativo y exigir al menos 12
  caracteres.
- **FR-008**: Un administrador del servicio DEBE poder crear, editar, activar/desactivar usuarios,
  asignar roles y carreras, restablecer contraseñas e importar CSV; la cédula DEBE validarse con
  módulo 10 y ser única, igual que el correo.
- **FR-009**: Los usuarios DEBEN poder cambiar su contraseña en el servicio; el cambio cierra sus
  demás sesiones universitarias.
- **FR-010**: El servicio DEBE permitir registrar servicios (nombre, direcciones de retorno
  permitidas, activo) por un administrador.

**GeoAsist**

- **FR-011**: GeoAsist web DEBE ofrecer el ingreso con cuenta universitaria y, al volver con una prueba
  de identidad, validarla en el servidor antes de crear su propia sesión.
- **FR-012**: La app móvil DEBE iniciar sesión mediante el navegador del sistema y volver a la app
  con la prueba de identidad; DEBE verificar que el retorno corresponde a un inicio pedido por ella.
- **FR-013**: Al validar, GeoAsist DEBE crear o actualizar la cuenta local con los roles y datos
  recibidos y enlazarla a la persona con la misma cédula; NO DEBE guardar contraseñas de cuentas
  institucionales.
- **FR-014**: GeoAsist DEBE mantener su sesión propia (8 h, renovable hasta 30 días, 003) y, al
  renovar, confirmar con el servicio que la cuenta sigue activa.
- **FR-015**: El cierre de sesión de GeoAsist DEBE cerrar su sesión y ofrecer cerrar también la
  sesión universitaria.
- **FR-016**: La validación DEBE depender del puerto de identidad existente; cambiar de servicio de
  identidad DEBE requerir solo configuración si el protocolo coincide.
- **FR-017**: Las funciones de GeoAsist que gestionaban contraseñas de cuentas institucionales (cambio
  propio y restablecimiento de 003, alta de cuentas con contraseña de 002) DEBEN retirarse o
  delegarse al servicio; la gestión académica (carreras, paralelos, matrículas, docentes) se
  mantiene en GeoAsist enlazada por cédula.
- **FR-018**: Los invitados sin cuenta (002, 001) NO DEBEN verse afectados.

### Privacidad

Datos nuevos en el servicio simulado: correo, cédula, nombres, roles, carreras, hash de contraseña,
registro de inicios de sesión. Solo administradores del servicio los consultan. Son datos ficticios
de desarrollo; el servicio NO DEBE usarse con datos reales. Registro de inicios conservado 90 días
(supuesto de desarrollo). GeoAsist solo recibe los atributos listados en FR-004.

### Entidades clave

- **Usuario universitario**: correo único, cédula única, nombres, roles, carreras, activo, debe cambiar
  contraseña.
- **Sesión universitaria**: sesión única por navegador; vigencia y cierre.
- **Servicio registrado**: nombre, direcciones de retorno permitidas, activo.
- **Prueba de identidad**: de un solo uso, ligada a usuario, servicio y sesión, con vencimiento.
- **Registro de inicio de sesión**: correo, resultado, origen, fecha; sin secretos.
- **Cuenta GeoAsist** (existente): ahora provisionada desde los atributos; sin contraseña.

## Success Criteria *(obligatorio)*

- **SC-001**: Una persona entra a GeoAsist web con su cuenta universitaria en menos de 30 segundos
  desde que pulsa el botón.
- **SC-002**: Con una sesión universitaria vigente, entrar a un segundo servicio registrado no pide
  contraseña en el 100 % de las pruebas.
- **SC-003**: El 100 % de los intentos de reutilizar una prueba de identidad, usarla en otro servicio,
  usarla vencida o pedir una redirección a una dirección no registrada se rechazan.
- **SC-004**: Ninguna contraseña ni prueba de identidad aparece en registros, auditorías ni
  respuestas de GeoAsist en las pruebas de privacidad.
- **SC-005**: Todas las pruebas existentes de asistencia, ámbito y conservación de GeoAsist pasan sin
  cambios en sus reglas tras sustituir el inicio de sesión.
- **SC-006**: Cambiar la dirección del servicio de identidad no requiere modificar código de GeoAsist.

## Assumptions

- El protocolo simulado es CAS 3.0 (decisión del usuario, 2026-10-26); es una hipótesis hasta que la
  UNL confirme el suyo (Principio V). Si difiere, se reemplaza el adaptador de GeoAsist.
- "Acceso a otros servicios" se interpreta como inicio de sesión único: una sesión universitaria y una
  prueba de identidad distinta por servicio. No se emiten tokens que otros servicios acepten
  directamente (eso sería otro protocolo y queda fuera).
- La gestión académica (carreras, paralelos, matrículas, asignación docente) sigue en GeoAsist y se
  enlaza por cédula; el servicio solo conoce las carreras como atributo del usuario.
- Las semillas del servicio reproducen las cuentas de desarrollo actuales (mismos correos y cédulas).
- En desarrollo todo corre en local con contenedores; fuera de desarrollo se exige HTTPS.

### Fuera del alcance

Integración con el servicio real de la UNL, doble factor, federación con otros protocolos, tickets
para servicios intermedios (proxy), registro de usuarios por autoservicio sin administrador,
recuperación de contraseña por correo.

## Clarifications

### Session 2026-10-26

- Q: ¿Qué protocolo simula el servicio? → A: CAS 3.0.
- Q: ¿Dónde viven los usuarios y roles simulados? → A: En el servicio de autenticación, con su propia base.
- Q: ¿Con qué tecnología? → A: La misma del backend.
- Q: ¿Qué pasa con el ingreso local de GeoAsist? → A: Las personas ingresan con sus credenciales
  universitarias (correo y contraseña) en el servicio, que da acceso a la app y a otros servicios de
  la universidad (interpretado como inicio de sesión único; ver Assumptions).
