# Auditoría base de la experiencia web (007)

Fecha de la auditoría: 2026-11-04. Referencia: `specs/007-optimizacion-ux-web/spec.md`, sección "Problema que resuelve".

## Método

- Recorrido real de la web en `http://localhost:3000` con inicio de sesión SSO, primero como docente y luego como administrador.
- La navegación se hizo siempre con el menú de la aplicación, sin escribir URLs a mano.
- Métricas por página:
  - alto de la página en píxeles;
  - número de botones dentro de `main`;
  - número de campos (entradas de formulario) dentro de `main`.
- Los clics se contaron desde que el usuario ya está dentro de la aplicación.

## Línea base

### Hallazgos

| Hallazgo | Evidencia | Impacto |
|---|---|---|
| Recargar o abrir un enlace pierde la sesión | El token vive solo en memoria; cada recarga lleva a "Ingresar" | Clics repetidos y enlaces que no sirven |
| Tras ingresar se queda en "Sesión iniciada" | Página intermedia con botones que repiten el menú | Un paso inútil en cada ingreso |
| "Mis actividades" sobrecargada | 3 401 px de alto, 24 botones, 22 campos: lista + formulario de clase + formulario de evento | Difícil de leer; crear algo exige recorrer todo |
| Abrir la sesión de hoy y proyectar el QR | Menú → Sesiones → elegir actividad → elegir sesión → Iniciar → Mostrar QR (≈ 6 clics) | La tarea más frecuente es la más lenta |
| Sin jerarquía visual | Menú, acción principal y acción destructiva ("Cancelar clase") tienen el mismo botón verde relleno | Riesgo de error y lectura lenta |
| Herramientas de prueba en el menú principal | "Simulador de invitado" junto a las tareas del docente | Ruido en la navegación |

### Valores medidos

| Pantalla | Alto | Botones | Campos |
|---|---|---|---|
| Mis actividades (docente) | 3 401 px | 24 | 22 |
| Sesiones (docente) | 356 px | 0 | 1 (solo el selector de actividad) |
| Estadísticas (docente) | 888 px | 1 | 4 |
| Reportes (docente) | 825 px | 2 | 4 |
| Personas (administrador) | 1 932 px | 13 | 6 |
| Carreras y paralelos (administrador) | 737 px | 1 | 3 |
| Espacios (administrador) | 481 px | 1 | 1 |
| Importar (administrador) | 636 px | 1 | 2 |

Medido el 2026-11-04 con Playwright (Chromium, ventana 1366×850), navegando con el menú tras ingresar
por el SSO; "Botones" y "Campos" cuentan solo elementos dentro de `main`. Las capturas se tomaron
localmente y no se versionan.

## Metas

| Tarea | Hoy | Meta |
|---|---|---|
| SC-001 Docente: abrir la sesión de hoy y proyectar el QR (desde que ya está dentro) | ≈ 6 clics | **1 clic** |
| SC-002 Recargar la página con la sesión universitaria vigente | Ingresar de nuevo (3+ clics y contraseña) | **0 clics** |
| SC-003 Docente: tras cerrar, justificar una ausencia | Ir a otra página y elegir sesión | **2 clics** |
| SC-004 Director: ver los paralelos bajo umbral | Menú → formulario de periodo → consultar | **0 clics** (en su inicio) |
| SC-005 Altura de "Mis actividades" con 10 actividades | 3 401 px | **≤ 1 pantalla (≈ 900 px)** |
| SC-006 Botones en una pantalla sin abrir paneles | hasta 24 | **≤ 8** |

SC-007: las metas anteriores se miden con pruebas E2E que cuentan clics y altura; ninguna regresión de accesibilidad (axe sin violaciones serias en las pantallas principales).

## Resultado después de 007

_Pendiente. Esta sección se completará con T023 (capturas antes/después) y T024 (revisión de verificación en `docs/validation/revision-incremento-007.md`)._

## Accesibilidad (T019)

Contraste calculado con `node tools/contracts/contrast.mjs` (fórmula WCAG 2.x; mínimo 4,5:1 para texto). Ningún par quedó por debajo, por lo que no se cambió ningún token.

| Par (texto sobre fondo) | Ratio | Resultado |
|---|---|---|
| ink `#18302a` / paper `#f5f6f1` | 12,93 | OK |
| ink / surface `#fff` | 14,05 | OK |
| brand `#146b50` / paper | 5,95 | OK |
| brand / surface | 6,46 | OK |
| blanco / brand (botón primario) | 6,46 | OK |
| danger `#a33726` / paper · surface | 6,17 · 6,71 | OK |
| warn `#8a5a00` / paper · surface | 5,46 · 5,93 | OK |
| muted `#4f665b` / paper · surface | 5,71 · 6,21 | OK |
| info `#1c5ca3` / surface | 6,75 | OK |
| StatusTag: brand/ok-soft · warn/warn-soft · danger/danger-soft · info/info-soft | 5,73 · 5,28 · 6,08 · 5,78 | OK |
| Métodos HTTP de /documentacion (get, put, patch, delete) / blanco | 5,76 · 5,78 · 7,08 · 6,36 | OK |

Los botones deshabilitados (opacidad .55) quedan exentos por WCAG (componentes inactivos).

| Comprobación | Estado | Cómo |
|---|---|---|
| Foco visible | OK | `:focus-visible` global con contorno de 3 px `--color-brand` y desfase de 3 px |
| Objetivos ≥ 44 px | OK | `.ui-btn`, `button`, `nav a`, pestañas, migas, chips de método, índice de documentación, resúmenes y casillas con `min-height:44px` |
| Sin scroll horizontal a 375 px | OK por CSS (sin medición en navegador) | todas las tablas dentro de `.table-wrap` (`overflow-x:auto`); `main{min-width:0}` |
| Etiquetas visibles | OK | todos los campos usan `<label>` visible; el buscador de /documentacion usa `aria-label` con marcador de posición |
| `prefers-reduced-motion` | OK | regla global que anula transiciones y animaciones |
| Color no es el único indicador | OK | `StatusTag` lleva icono y texto |

No se ejecutó axe ni Playwright en este incremento: lo anterior es verificación por CSS y por cálculo.
