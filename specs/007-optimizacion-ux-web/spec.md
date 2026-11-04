# Especificación de funcionalidad: Optimización de la experiencia web

**Rama de la funcionalidad**: `feature/007-optimizacion-ux-web` (desde `feature/006-frontend-web-pruebas`)

**Creado**: 2026-11-04

**Estado**: Borrador — pedido del usuario: "optimizar las vistas web: intuitivas, sin sobrecargar todo en
una página, con la menor cantidad de clics", usando las guías de UI/UX Pro Max y Frontend Design.
No cambia el backend ni sus reglas.

## Problema que resuelve (auditoría del 2026-11-04)

Recorrido real con SSO como docente y administrador (capturas en la auditoría del plan):

| Hallazgo | Evidencia | Impacto |
|---|---|---|
| Recargar o abrir un enlace pierde la sesión | El token vive solo en memoria; cada recarga lleva a "Ingresar" | Clics repetidos y enlaces que no sirven |
| Tras ingresar se queda en "Sesión iniciada" | Página intermedia con botones que repiten el menú | Un paso inútil en cada ingreso |
| "Mis actividades" sobrecargada | 3 401 px de alto, 24 botones, 22 campos: lista + formulario de clase + formulario de evento | Difícil de leer; crear algo exige recorrer todo |
| Abrir la sesión de hoy y proyectar el QR | Menú → Sesiones → elegir actividad → elegir sesión → Iniciar → Mostrar QR (≈ 6 clics) | La tarea más frecuente es la más lenta |
| Sin jerarquía visual | Menú, acción principal y acción destructiva ("Cancelar clase") tienen el mismo botón verde relleno | Riesgo de error y lectura lenta |
| Herramientas de prueba en el menú principal | "Simulador de invitado" junto a las tareas del docente | Ruido en la navegación |

## User Scenarios & Testing *(obligatorio)*

### Historia de usuario 1 — Llegar directo a mi trabajo (Prioridad: P1)

Tras ingresar, cada rol llega a un inicio con lo que necesita hoy; recargar o abrir un enlace no
obliga a volver a escribir la contraseña mientras la sesión universitaria siga vigente.

1. **Dado** un docente, **Cuando** ingresa, **Entonces** llega a "Hoy": sus sesiones de hoy (programadas,
   activa, cerradas) con una acción principal por sesión ("Iniciar y proyectar QR", "Ver en vivo",
   "Revisar ausencias").
2. **Dado** un director, **Cuando** ingresa, **Entonces** llega a un resumen de su ámbito (porcentaje global y
   paralelos bajo umbral) con enlace al detalle.
3. **Dado** un administrador, **Cuando** ingresa, **Entonces** llega a un inicio con sus tareas (personas,
   carreras y paralelos, espacios, importar) y contadores.
4. **Dado** una sesión universitaria vigente, **Cuando** el usuario recarga o abre un enlace de GeoAsist,
   **Entonces** vuelve autenticado a la misma página sin escribir credenciales (0 clics); si la sesión
   universitaria venció, ve el inicio de sesión una sola vez y vuelve a la página que pidió.

### Historia de usuario 2 — Abrir la sesión de hoy en un clic y proyectar el QR (Prioridad: P1)

1. **Dado** una sesión programada para hoy, **Cuando** el docente pulsa "Iniciar y proyectar QR" en "Hoy",
   **Entonces** la sesión se inicia y se abre la vista de proyección (1 clic).
2. **Dado** la vista de proyección, **Cuando** está activa, **Entonces** el QR ocupa la pantalla con alto
   contraste, el nombre de la clase, el tiempo restante del QR y el contador de asistencias en vivo;
   un botón permite pantalla completa y otro "Cerrar sesión" con confirmación en la misma vista.
3. **Dado** que se cierra la sesión, **Cuando** termina, **Entonces** la vista pasa al resumen: asistencias,
   ausencias y pendientes de revisión, con acciones directas (asistencia manual, justificar, aprobar).

### Historia de usuario 3 — Actividades sin sobrecarga (Prioridad: P1)

1. **Dado** "Mis actividades", **Cuando** se abre, **Entonces** muestra solo la lista (nombre, tipo, estado,
   próxima sesión) con búsqueda y un botón "Nueva actividad"; los formularios no están en esa página.
2. **Dado** "Nueva actividad", **Cuando** se abre, **Entonces** un asistente de 3 pasos pide: tipo y nombre →
   lugar y horario (o fecha del evento) → participantes (paralelos o tipo de acceso), con valores por
   defecto (último espacio usado, método por defecto) y resumen antes de crear.
3. **Dado** una actividad, **Cuando** se abre su ficha, **Entonces** el contenido se divide en pestañas
   (Sesiones, Participantes, Horario o Invitados, Justificaciones, Auditoría); acciones destructivas
   (cancelar actividad) quedan en un menú secundario con confirmación en la página.

### Historia de usuario 4 — Navegación y jerarquía visual coherentes (Prioridad: P2)

1. **Dado** cualquier página, **Cuando** se mira, **Entonces** hay una sola acción principal visible (botón
   primario), las secundarias son discretas y las destructivas se distinguen por estilo y texto.
2. **Dado** la navegación, **Cuando** se usa, **Entonces** es una barra lateral (escritorio) o un menú
   compacto (teléfono) con 5 o menos entradas por rol; las herramientas de prueba van en una sección
   aparte "Pruebas" solo con la bandera activa.
3. **Dado** una página de detalle, **Cuando** se abre, **Entonces** muestra migas de pan (por ejemplo
   Actividades › Programación II › Sesión 12 nov) y "Volver" lleva a la lista conservando filtros.
4. **Dado** estados de sesiones y asistencias, **Cuando** se muestran, **Entonces** usan una etiqueta con texto
   y color (Programada, Activa, Cerrada, Cancelada; Aceptada, Rechazada, Pendiente), nunca solo color.

### Historia de usuario 5 — Administración en listas con panel lateral (Prioridad: P2)

1. **Dado** Personas, Carreras y paralelos o Espacios, **Cuando** se abren, **Entonces** muestran una tabla con
   búsqueda y paginación; crear o editar se hace en un panel lateral que no saca al usuario de la lista.
2. **Dado** un paralelo, **Cuando** se abre, **Entonces** sus matriculados y docentes están en pestañas del
   panel, con reemplazo masivo pegando una lista.

### Edge Cases

- Sin sesiones hoy → "Hoy" muestra "No tienes sesiones hoy" y el acceso a la próxima sesión.
- Varias sesiones activas → "Hoy" las lista primero.
- Proyección en pantalla pequeña → el QR conserva tamaño mínimo legible y el resto se oculta.
- Re-autenticación silenciosa que falla (SSO caído) → mensaje claro y botón "Ingresar"; nunca un bucle de redirecciones.
- Teclado y lector de pantalla: pestañas, panel lateral y asistente operables sin ratón.

## Requirements *(obligatorio)*

- **FR-001**: Tras ingresar, la web DEBE llevar a cada rol a su inicio; NO DEBE existir la página intermedia "Sesión iniciada".
- **FR-002**: Con sesión universitaria vigente, recargar o abrir un enlace DEBE restaurar la sesión sin pedir credenciales y volver a la página pedida, sin guardar el token fuera de memoria (Principio IX) y sin bucles.
- **FR-003**: El docente DEBE poder iniciar la sesión de hoy y ver el QR proyectado con 1 clic desde el inicio.
- **FR-004**: La vista de proyección DEBE mostrar QR grande, tiempo del QR, contador en vivo, pantalla completa y cierre con confirmación en la misma vista; tras cerrar DEBE mostrar el resumen con acciones de corrección.
- **FR-005**: "Mis actividades" DEBE mostrar solo la lista; crear una actividad DEBE hacerse en un asistente de 3 pasos con valores por defecto y resumen.
- **FR-006**: La ficha de actividad DEBE organizarse en pestañas y las acciones destructivas DEBEN estar en un menú secundario con confirmación en la página.
- **FR-007**: Cada pantalla DEBE tener una sola acción primaria; los botones DEBEN tener variantes primaria, secundaria, de texto y destructiva.
- **FR-008**: La navegación DEBE tener ≤ 5 entradas por rol; las herramientas de prueba en una sección separada solo con la bandera.
- **FR-009**: Las páginas de detalle DEBEN mostrar migas de pan y conservar filtros al volver.
- **FR-010**: Los estados DEBEN mostrarse con etiqueta de texto y color.
- **FR-011**: La administración DEBE usar tablas con búsqueda y panel lateral para crear y editar.
- **FR-012**: Toda la web DEBE cumplir: contraste 4,5:1, foco visible, objetivos táctiles ≥ 44 px, sin desplazamiento horizontal desde 375 px, `prefers-reduced-motion` respetado, etiquetas visibles en formularios y errores junto al campo.

## Success Criteria *(obligatorio)*

| Tarea | Hoy | Meta |
|---|---|---|
| SC-001 Docente: abrir la sesión de hoy y proyectar el QR (desde que ya está dentro) | ≈ 6 clics | **1 clic** |
| SC-002 Recargar la página con la sesión universitaria vigente | Ingresar de nuevo (3+ clics y contraseña) | **0 clics** |
| SC-003 Docente: tras cerrar, justificar una ausencia | Ir a otra página y elegir sesión | **2 clics** |
| SC-004 Director: ver los paralelos bajo umbral | Menú → formulario de periodo → consultar | **0 clics** (en su inicio) |
| SC-005 Altura de "Mis actividades" con 10 actividades | 3 401 px | **≤ 1 pantalla (≈ 900 px)** |
| SC-006 Botones en una pantalla sin abrir paneles | hasta 24 | **≤ 8** |

SC-007: las metas anteriores se miden con pruebas E2E que cuentan clics y altura; ninguna regresión de accesibilidad (axe sin violaciones serias en las pantallas principales).

## Assumptions

- Se conserva la marca actual (verde institucional) y el estilo sobrio; no se adopta la paleta ni la tipografía infantil que sugirió la búsqueda automática (ver plan).
- La re-autenticación silenciosa usa el SSO existente: si la cookie universitaria es válida, el servicio devuelve un ticket sin pedir credenciales (004).
- No hay cambios de API; si un dato del inicio exige muchas llamadas, se acepta como límite medido y se anota.

### Fuera del alcance

App móvil, modo oscuro completo, nuevas funcionalidades de negocio, cambios del backend.
