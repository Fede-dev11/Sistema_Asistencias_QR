# Flujo SDD con GitHub Spec Kit

Este proyecto sigue **Specification-Driven Development (SDD)** con
[GitHub Spec Kit](https://github.com/github/spec-kit) v1.1.3, integrado con Claude Code.
Antes de escribir código se define *qué* debe hacer el sistema y *cómo se comprobará*;
después se decide *cómo* construirlo y se divide en tareas.

## Dónde está cada cosa

```text
.specify/
├── memory/constitution.md     # constitution: principios no negociables
├── templates/                 # plantillas de spec, plan, tasks y checklist
└── scripts/bash/              # scripts que crean ramas y carpetas de cada feature
.claude/skills/speckit-*/      # comandos /speckit-* para el asistente de IA
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

## Cómo crear una nueva especificación

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
