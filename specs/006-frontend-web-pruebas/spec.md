# Especificación de funcionalidad: Vistas web básicas para probar todas las funcionalidades

**Rama de la funcionalidad**: `feature/006-frontend-web-pruebas` (desde `feature/005-consultas-faltantes`)

**Creado**: 2026-11-03

**Estado**: Borrador — decisiones del usuario (2026-11-03): vistas funcionales con el estilo actual
de la web; los flujos de participante e invitado se prueban con un simulador web visible solo con
una bandera de pruebas. No cambia el backend ni sus reglas.

**Entrada**: "Ayúdame a planificar la implementación del frontend web, donde necesito vistas
básicas para poder probar las funcionalidades."

## Problema que resuelve

El backend tiene 73 operaciones; 28 no tienen pantalla y algunas pantallas existentes piden pegar
identificadores a mano (por ejemplo, el código de sesión). El equipo no puede probar de punta a
punta las correcciones, las justificaciones, las estadísticas, la administración de paralelos, la
evidencia de GPS ni los flujos de estudiante e invitado sin usar herramientas externas.

## User Scenarios & Testing *(obligatorio)*

### Historia de usuario 1 — Entrar y llegar a lo que mi rol puede hacer (Prioridad: P1)

Tras ingresar con la cuenta universitaria, la persona ve un menú con solo las secciones de sus
roles y su nombre; la sesión se mantiene sin volver a ingresar mientras la página esté abierta.

**Prueba independiente**: ingresar con docente, director, administrador y estudiante y comprobar
que cada uno ve solo sus secciones.

1. **Dado** un usuario autenticado, **Cuando** abre la web, **Entonces** ve su nombre, sus roles y los
   enlaces de sus secciones; los de otros roles no aparecen.
2. **Dado** una sesión abierta cerca de vencer, **Cuando** sigue usando la web, **Entonces** la sesión se
   renueva sola; si la renovación falla (cuenta desactivada, SSO caído) se le indica y vuelve a ingresar.
3. **Dado** una ruta de otro rol, **Cuando** la abre directamente, **Entonces** ve "No tienes acceso a esta
   sección" en lugar de una pantalla rota.

### Historia de usuario 2 — Docente opera una sesión de principio a fin (Prioridad: P1)

El docente elige la actividad y la sesión de una lista (sin pegar códigos), la inicia, proyecta el QR,
ve las asistencias en vivo, registra asistencias manuales, revisa las ausencias al cerrar, justifica
faltas, decide solicitudes de justificación, consulta y cambia el horario, y sincroniza estudiantes.

**Prueba independiente**: con una clase sembrada, recorrer iniciar → QR → cerrar → ausencias →
asistencia manual → justificar → decidir solicitud, todo desde la web.

1. **Dado** una actividad, **Cuando** el docente la elige, **Entonces** ve sus sesiones con fecha, estado y
   método, y opera la que elija.
2. **Dado** una sesión activa o cerrada hace menos de 7 días, **Cuando** registra asistencia manual con
   razón, **Entonces** aparece en el listado con forma MANUAL; fuera de plazo ve el motivo del rechazo.
3. **Dado** una sesión cerrada, **Cuando** abre ausencias, **Entonces** ve quién faltó y puede justificar con
   razón.
4. **Dado** solicitudes pendientes, **Cuando** abre la bandeja, **Entonces** las aprueba o rechaza con razón.
5. **Dado** una clase con horario, **Cuando** lo abre, **Entonces** ve el horario actual y puede reemplazarlo
   con vista previa de fechas; también puede sincronizar la lista con los paralelos.
6. **Dado** un evento, **Cuando** lo abre, **Entonces** ve el listado de invitados y su configuración actuales.

### Historia de usuario 3 — Consultar estadísticas (Prioridad: P2)

Docentes y directores consultan porcentajes por clase, por paralelo y por carrera en un periodo.

1. **Dado** una clase con sesiones cerradas, **Cuando** el docente abre estadísticas, **Entonces** ve por
   estudiante asistencias, ausencias, justificadas, porcentaje y "bajo umbral" resaltado.
2. **Dado** un director, **Cuando** elige periodo y carrera, **Entonces** ve las estadísticas de paralelos y
   carreras de su ámbito.

### Historia de usuario 4 — Administrar paralelos y evidencias de GPS (Prioridad: P2)

1. **Dado** un paralelo, **Cuando** el administrador lo abre, **Entonces** ve y reemplaza matriculados (por
   cédula) y docentes (por correo), con informe de rechazados.
2. **Dado** un espacio con coordenadas, **Cuando** el administrador registra una evidencia de GPS,
   **Entonces** aparece en el historial con su versión; docentes ven ese historial al elegir el espacio.

### Historia de usuario 5 — Simulador de participante e invitado (Prioridad: P1)

Con la bandera de pruebas activa, una página permite actuar como estudiante (sesiones activas,
registrar asistencia con QR pegado o por sesión, ubicación manual, identificador de dispositivo de
prueba editable, historial, porcentajes, solicitar justificación) y otra como invitado (datos públicos
de un evento abierto, abrir formulario, enviar cédula y, en eventos abiertos, nombres).

**Prueba independiente**: registrar dos estudiantes con el mismo identificador de dispositivo en la
misma sesión y ver el rechazo `DISPOSITIVO_YA_REGISTRADO`.

1. **Dado** la bandera desactivada, **Cuando** se abre la ruta del simulador, **Entonces** no existe (404).
2. **Dado** un estudiante autenticado, **Cuando** abre el simulador, **Entonces** ve sus sesiones activas y
   registra asistencia; la respuesta muestra resultado y motivo tal cual los da la API.
3. **Dado** el identificador de dispositivo, **Cuando** lo cambia o lo reutiliza, **Entonces** puede probar la
   regla de un dispositivo por sesión.
4. **Dado** un invitado, **Cuando** usa el simulador de invitado, **Entonces** completa el flujo de evento
   cerrado (cédula del listado) y de evento abierto (cédula y nombres).
5. **Dado** el estudiante, **Cuando** abre su historial, **Entonces** ve resultados por sesión y porcentajes, y
   puede solicitar justificación de una ausencia.

### Edge Cases

- Sin bandera de pruebas en producción → el simulador no se publica en la navegación ni responde.
- Errores de la API → se muestra el `code` y el mensaje del Problem Details, nunca una pantalla en blanco.
- Listas largas → paginación con "Anterior/Siguiente".

## Requirements *(obligatorio)*

- **FR-001**: La web DEBE mostrar navegación según los roles del usuario y bloquear con mensaje las secciones ajenas.
- **FR-002**: La web DEBE renovar la sesión antes de vencer y manejar el rechazo de la renovación.
- **FR-003**: El docente DEBE poder elegir sesiones de una lista y operar el ciclo completo de la sesión.
- **FR-004**: El docente DEBE poder registrar asistencia manual, ver ausencias, justificarlas y decidir solicitudes.
- **FR-005**: El docente DEBE poder ver y reemplazar el horario y sincronizar la lista autorizada.
- **FR-006**: La web DEBE mostrar el estado actual (invitados, configuración de invitados, configuración GPS) antes de editarlo.
- **FR-007**: Docentes y directores DEBEN poder consultar estadísticas por clase, paralelo y carrera.
- **FR-008**: El administrador DEBE poder gestionar matriculados y docentes de un paralelo y registrar evidencias de GPS.
- **FR-009**: Con la bandera de pruebas, la web DEBE ofrecer simuladores de participante e invitado; sin ella NO DEBEN existir.
- **FR-010**: Toda pantalla DEBE mostrar estados de carga, éxito y error comprensibles y ser accesible (etiquetas, foco, teclado) (Principio IX).
- **FR-011**: El token NO DEBE guardarse fuera de memoria (Principio IX); el identificador de dispositivo de prueba puede guardarse en `sessionStorage` solo dentro del simulador.

## Success Criteria *(obligatorio)*

- **SC-001**: Las 73 operaciones del contrato se pueden ejecutar desde la web (con bandera de pruebas), comprobado con una lista de verificación por operación.
- **SC-002**: Un recorrido E2E por rol (docente, director, administrador, estudiante simulado, invitado simulado) pasa en el CI.
- **SC-003**: Ninguna pantalla nueva necesita pegar un identificador interno para llegar a su dato.

## Assumptions

- Estilo y componentes actuales de `apps/web` (sin librería de UI nueva).
- La regla "registro solo desde la app" (002 FR-026) se mantiene en producción; el simulador es una herramienta de pruebas.
- Los textos de la API (Problem Details) se muestran tal cual; no se traducen códigos.

### Fuera del alcance

Diseño visual nuevo, app móvil, nuevos endpoints o cambios del backend.
