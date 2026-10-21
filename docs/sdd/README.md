# Flujo SDD con GitHub Spec Kit

Este proyecto sigue **Specification-Driven Development (SDD)** con
[GitHub Spec Kit](https://github.com/github/spec-kit) v1.1.3, integrado con Codex y Claude Code.
Codex es la integración predeterminada del proyecto.
Antes de escribir código se define *qué* debe hacer el sistema y *cómo se comprobará*;
después se decide *cómo* construirlo y se divide en tareas.

## Dónde está cada cosa

```text
.specify/
├── memory/constitution.md     # constitution: principios no negociables
├── templates/                 # plantillas de spec, plan, tasks y checklist
└── scripts/bash/              # scripts que crean ramas y carpetas de cada feature
.agents/skills/speckit-*/      # habilidades $speckit-* para Codex
.claude/skills/speckit-*/      # comandos /speckit-* para Claude Code
specs/
└── 001-asistencia-qr-dinamico/
    ├── spec.md                # specify (+ sección Clarifications de clarify)
    ├── plan.md                # plan
    ├── research.md            # plan, Fase 0: decisiones y alternativas
    ├── data-model.md          # plan, Fase 1: entidades y estados
    ├── contracts/api.md       # plan, Fase 1: contrato de API
    ├── quickstart.md          # plan, Fase 1: validación de punta a punta
    └── tasks.md               # tasks
```

## Propósito de cada artefacto

| Etapa | Comando | Artefacto | Pregunta que responde | Qué contiene | Qué NO contiene |
|-------|---------|-----------|----------------------|--------------|-----------------|
| **constitution** | `/speckit-constitution` | `.specify/memory/constitution.md` | ¿Qué reglas rigen *todo* el proyecto? | Principios de seguridad, integridad, privacidad, pruebas, simplicidad y forma de trabajo; gobernanza | Detalles de una funcionalidad concreta |
| **specify** | `/speckit-specify` | `specs/NNN/spec.md` | ¿*Qué* necesita el usuario y cómo sabremos que funciona? | Problema, historias priorizadas, escenarios Dado/Cuando/Entonces, requisitos FR, entidades, criterios de éxito SC, supuestos, fuera de alcance | Tecnologías, frameworks, tablas |
| **clarify** | `/speckit-clarify` | sección *Clarifications* de `spec.md` | ¿Qué quedó ambiguo? | Preguntas concretas y respuestas registradas (p. ej. duración del QR, umbral GPS) | Diseño técnico |
| **plan** | `/speckit-plan` | `plan.md` + `research.md`, `data-model.md`, `contracts/`, `quickstart.md` | ¿*Cómo* lo construiremos? | Contexto técnico, verificación contra la constitución, estructura, decisiones con alternativas, modelo de datos, API | Lista detallada de tareas |
| **tasks** | `/speckit-tasks` | `tasks.md` | ¿En qué orden y en qué pasos pequeños? | Tareas numeradas por fase e historia, con rutas, paralelismo `[P]` y dependencias | Nuevos requisitos |
| implement | `/speckit-implement` | código | — | Ejecución de las tareas | — |

**Diferencia clave**: la *constitution* es global y estable; *specify* y *clarify* hablan
del problema (el qué, sin tecnología); *plan* traduce el problema a decisiones técnicas
(el cómo); *tasks* convierte el plan en trabajo asignable y verificable.

## Uso con Codex

Abre este repositorio en Codex. Escribe `$` para seleccionar una habilidad o invócala
directamente en el chat. Codex descubre las habilidades locales en `.agents/skills/`;
si no aparecen después del cambio, reinicia Codex, según la
[documentación oficial de habilidades](https://learn.chatgpt.com/docs/build-skills).

| Habilidad de Codex | Propósito |
|-------------------|-----------|
| `$speckit-constitution` | Crear o actualizar los principios del proyecto |
| `$speckit-specify` | Crear o actualizar una especificación |
| `$speckit-clarify` | Resolver ambigüedades de la especificación |
| `$speckit-plan` | Preparar el plan y los artefactos de diseño |
| `$speckit-tasks` | Desglosar el plan en tareas |
| `$speckit-analyze` | Revisar consistencia entre especificación, plan y tareas |
| `$speckit-checklist` | Crear una lista de revisión de requisitos |
| `$speckit-implement` | Ejecutar las tareas de implementación |
| `$speckit-converge` | Detectar trabajo pendiente después de implementar y añadir tareas |
| `$speckit-taskstoissues` | Convertir tareas en issues de GitHub; obsoleta en Spec Kit 1.1.3 |

Ejemplo para comenzar una nueva funcionalidad:

```text
$speckit-specify Permitir al docente cerrar una sesión de asistencia y consultar su resumen.
```

Continúa con `$speckit-clarify`, `$speckit-plan`, `$speckit-tasks`,
`$speckit-analyze` y `$speckit-implement`, revisando cada artefacto antes de avanzar.
La constitución vigente está en `.specify/memory/constitution.md`.

Para trabajar sobre la especificación existente, puedes indicar su ruta en el mensaje:

```text
$speckit-clarify Revisa specs/001-asistencia-qr-dinamico/spec.md contra la constitución vigente.
```

Las habilidades usan los scripts y las plantillas compartidos de `.specify/`.
Comprueba la instalación desde la raíz con `specify integration status`.
Para reproducir la configuración en otro checkout que solo tenga Claude instalado:

```bash
specify integration install codex
specify integration use codex
```

## Cómo crear una nueva especificación con Claude Code

1. Desde `develop`, crear la rama: `git switch -c feature/NNN-nombre-corto`.
2. En Claude Code: `/speckit-specify <descripción en lenguaje natural del problema>`.
3. `/speckit-clarify` — responder las preguntas y revisar la sección *Clarifications*.
4. `/speckit-plan <stack y restricciones>` — revisar el *Constitution Check*.
5. `/speckit-tasks` y, opcionalmente, `/speckit-analyze` para revisar consistencia.
6. Abrir Pull Request hacia `develop` para revisión del equipo.

## Especificaciones previstas

| Nº | Nombre | Estado |
|----|--------|--------|
| 001 | asistencia-qr-dinamico (sesión, QR, registro, ubicación) | Borrador clarificado con plan y tareas |
| 002 | autenticacion-identidades | Pendiente |
| 003 | espacios-eventos | Pendiente |
| 004 | invitados | Pendiente |
| 005 | reportes-auditoria | Pendiente |
