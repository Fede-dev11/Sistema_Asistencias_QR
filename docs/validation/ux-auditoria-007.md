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
| Mis actividades | 3 401 px | 24 | 22 |

La auditoría original no registra en este documento otras pantallas con valores medidos.

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
