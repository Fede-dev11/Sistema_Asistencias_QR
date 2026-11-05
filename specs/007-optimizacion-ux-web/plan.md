# Implementation Plan: Optimización de la experiencia web

**Branch**: `feature/007-optimizacion-ux-web` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

## Summary

Reorganizar `apps/web` por tareas y no por formularios: inicio por rol ("Hoy"), re-autenticación
silenciosa con el SSO, vista de proyección de la sesión en vivo, lista de actividades separada de un
asistente de creación, fichas con pestañas, administración con tablas y panel lateral, y un sistema
visual con jerarquía (tokens CSS, variantes de botón, etiquetas de estado). Sin cambios de backend ni
dependencias nuevas salvo una fuente de Google Fonts autoalojada con `next/font`.

## Fuentes de diseño consultadas

**UI/UX Pro Max** (`--design-system` con densidad 6, variación 3, movimiento 3, y búsquedas `--domain ux`):

| Recomendación | Decisión |
|---|---|
| Estilo "Minimalism & Swiss": funcional, espacio en blanco, alto contraste, rejilla | **Se adopta** |
| Contraste 4,5:1, foco visible, `prefers-reduced-motion`, objetivos ≥ 44 px, sin emojis como iconos | **Se adopta** (FR-012) |
| Estados vacíos con mensaje y acción; feedback de carga → éxito/error; etiquetas visibles | **Se adopta** |
| Migas de pan solo con 3+ niveles; jerarquía de encabezados h1→h2→h3 | **Se adopta** (fichas de actividad y sesión) |
| Tablas con contenedor desplazable o tarjetas en móvil; acciones masivas | **Se adopta** (administración) |
| Paleta teal + ámbar "educación" | **Se descarta**: se conserva el verde institucional actual |
| Tipografía Baloo 2 / Comic Neue ("infantil") | **Se descarta**: el público son docentes y administradores universitarios |
| Patrón "Feature-Rich Showcase" (landing comercial) | **Se descarta**: es una app de trabajo, no una página de marketing |

**Frontend Design** (dirección visual): el momento memorable de GeoAsist es el **QR proyectado en el aula**;
ahí se concentra la audacia visual. Todo lo demás es sobrio y disciplinado. Se evitan los rasgos de
plantilla: tarjetas idénticas con la misma sombra, etiquetas en mayúsculas sobre cada título, botones
todos iguales, flechas "→" en cada enlace.

## Sistema visual (tokens)

| Token | Valor | Uso |
|---|---|---|
| `--color-brand` | `#146b50` | Acción primaria, foco, elemento activo del menú |
| `--color-ink` | `#18302a` | Texto principal (contraste ≥ 12:1 sobre papel) |
| `--color-paper` | `#f5f6f1` | Fondo (actual) |
| `--color-surface` | `#ffffff` | Superficies de contenido (sin sombra; borde 1 px `--color-line`) |
| `--color-line` | `#d7ded4` | Bordes y divisores |
| `--color-danger` | `#a33726` | Acciones destructivas y errores |
| `--color-warn` | `#8a5a00` | Pendientes y "bajo umbral" (con texto) |
| Tipografía | **Atkinson Hyperlegible** (Google Fonts vía `next/font`), una sola familia | Diseñada para legibilidad a distancia y baja visión: encaja con proyectar el QR y los nombres en el aula |
| Escala | 14 / 16 / 20 / 28 / 40 px; proyección 64 px | Base 16 px, interlineado 1,5 |
| Espaciado | 4 · 8 · 12 · 16 · 24 · 32 · 48 | Densidad estándar |
| Radios | 6 px controles, 10 px paneles; 0 en la vista de proyección | Jerarquía por forma, no un radio para todo |

Variantes de botón: **primario** (relleno verde, 1 por pantalla), **secundario** (borde), **texto**
(enlace) y **destructivo** (texto/borde rojo + confirmación en página). Etiqueta de estado (`StatusTag`):
texto + color + forma, nunca solo color.

## Arquitectura de información (por rol, ≤ 5 entradas)

```text
Docente/Encargado:  Hoy · Actividades · Estadísticas · Reportes
Director:           Resumen · Estadísticas · Reportes
Administrador:      Inicio · Personas · Carreras y paralelos · Espacios · Importar
Estudiante:         (sin web en producción; con bandera: Pruebas › Participante)
Pruebas (bandera):  sección separada al pie del menú: Participante · Invitado
```

### Flujos clave (wireframes)

```text
HOY (docente)                                   PROYECCIÓN /sesiones/[id]/proyectar
┌──────────────────────────────────────────┐    ┌───────────────────────────────────────────┐
│ Hoy, martes 4 nov                         │    │ Programación II · Aula A334      ⛶  Cerrar │
│ ● Activa  Programación II  08:00–10:00    │    │                                           │
│   23 presentes        [Ver en vivo]       │    │              ███████████████              │
│ ○ Programada  Redes  10:00–12:00          │    │              ██  QR grande  ██            │
│   Aula A334      [Iniciar y proyectar QR] │    │              ███████████████              │
│ ✓ Cerrada  Taller  07:00–08:00            │    │  Nuevo código en 18 s     23 presentes    │
│   3 ausencias      [Revisar ausencias]    │    └───────────────────────────────────────────┘
└──────────────────────────────────────────┘

ACTIVIDADES                                     NUEVA ACTIVIDAD (asistente)
┌──────────────────────────────────────────┐    ┌──────────────────────────────────────────┐
│ Actividades           [Nueva actividad]  │    │ 1 Qué  ─  2 Dónde y cuándo  ─  3 Quiénes │
│ [Buscar…]   Tipo ▾   Estado ▾            │    │ Tipo:  (•) Clase  ( ) Evento             │
│ Programación II  Clase  Activa  mar 08:00│    │ Nombre: [                    ]           │
│ Feria científica Evento Programada 12 nov│    │                     [Siguiente]          │
└──────────────────────────────────────────┘    └──────────────────────────────────────────┘
```

## Constitution Check

| Principio | Tratamiento | Estado |
|---|---|---|
| II | Reglas siguen en el backend | Cumple |
| IX | Componentes separados del acceso a datos; estados de carga/éxito/error; WCAG 2.1 AA básico; token solo en memoria (la re-autenticación usa el SSO, no almacenamiento) | Cumple |
| XII | Pruebas de componente y E2E que miden clics y altura (SC-001..SC-006) | Cumple |
| XIII | Este plan registra las fuentes de diseño y lo que se descartó | Cumple |

## Decisiones técnicas

- **Re-autenticación silenciosa**: al cargar sin sesión en memoria y con marca `sessionStorage['geoasist-sso']` (solo un indicador, nunca el token), redirigir a `login_url?service=<callback>&destino=<ruta>`; el callback canjea y vuelve a `destino` (validar que sea ruta interna). Si falla, borrar la marca y mostrar "Ingresar" (sin bucle).
- **Estado en la URL**: pestañas y filtros en `searchParams` para que "Volver" y los enlaces conserven contexto.
- **Fuente**: `next/font/google` (Atkinson Hyperlegible) autoalojada en build; sin peticiones a terceros en tiempo de ejecución.
- **Medición**: E2E `apps/web/e2e/ux-clics.spec.ts` cuenta clics por tarea y altura de páginas; `@axe-core/playwright` si ya está disponible, si no, comprobaciones de contraste/foco manuales documentadas (no se añade dependencia sin necesidad).

## Project Structure

```text
apps/web/src/
├── app/{hoy,inicio,actividades,actividades/nueva,actividades/[id],sesiones/[id],sesiones/[id]/proyectar}/page.tsx
├── features/ui/{Button,StatusTag,Tabs,Drawer,Stepper,Breadcrumbs,EmptyState,ConfirmInline}.tsx   # sistema visual
├── features/home/{TeacherToday,DirectorSummary,AdminHome}.tsx
├── features/sessions/{ProjectionView,SessionSummary}.tsx
├── features/activities/{ActivityList,ActivityWizard,ActivityTabs}.tsx
└── app/globals.css (tokens) · app/layout.tsx (barra lateral, fuente)
```

## Complexity Tracking

Sin entradas.
