<!--
Sync Impact Report
- Versión: plantilla → 1.0.0 (ratificación inicial)
- Principios añadidos: I–VII (todos nuevos)
- Secciones añadidas: Restricciones técnicas y de privacidad; Flujo de trabajo y calidad; Gobernanza
- Plantillas revisadas: plan-template.md ✅ (usa "Constitution Check" genérico), spec-template.md ✅, tasks-template.md ✅
- Pendientes: backend definitivo (Spring Boot o Laravel) se decide en la Fase 0 / Paso 5.
-->

# Constitución del Sistema de Asistencias QR

Sistema híbrido de control de asistencia mediante QR dinámico y validación geoespacial.
Este documento contiene los principios **no negociables** que toda especificación, plan,
tarea y Pull Request del proyecto debe respetar.

## Principios fundamentales

### I. Especificación antes que código (SDD)

Ninguna funcionalidad se implementa sin una especificación aprobada en `specs/NNN-nombre/`.
El flujo obligatorio es: `constitution → specify → clarify → plan → tasks → implement`.
Las especificaciones describen **comportamiento observable y criterios de aceptación
verificables** (Dado / Cuando / Entonces), no soluciones tecnológicas. Las ambigüedades
se marcan como `[NEEDS CLARIFICATION]` y deben resolverse en `clarify` antes de `plan`.

### II. El servidor es la única fuente de verdad (NO NEGOCIABLE)

Toda regla crítica (vigencia del QR, estado de la sesión, identidad, proximidad
geográfica, unicidad de la asistencia) se valida en el backend. Los clientes web y
móvil solo capturan datos y presentan resultados. La hora usada para caducidad es
siempre la del servidor. Nunca se confía en una validación realizada solo en el cliente.

### III. Seguridad por diseño y honestidad sobre los límites

- Los tokens QR se generan con un generador criptográficamente seguro, son de corta
  duración (referencia: 30 s), no contienen datos personales ni secretos y se validan
  en servidor.
- Se exige HTTPS, control de acceso por roles, validación de entradas y rate limiting.
- El sistema **reduce** el fraude; no promete certeza absoluta de presencia física.
  Toda limitación conocida (GPS simulable, interiores imprecisos, QR reenviado en
  tiempo real) se documenta y se comunica, nunca se oculta.

### IV. Integridad y trazabilidad de la asistencia

- Una persona tiene como máximo **una** asistencia confirmada por sesión
  (restricción única en base de datos + operación transaccional e idempotente).
- Todo intento (aceptado, rechazado o en revisión) queda registrado con un motivo.
- Ninguna asistencia se modifica sin bitácora de auditoría que indique responsable,
  fecha, razón y cambios.
- Ante incertidumbre (precisión GPS insuficiente o ubicación desactualizada) el
  resultado es **repetir medición o revisión**, nunca aceptación automática.

### V. Privacidad y minimización de datos

Se recolecta solo la información mínima necesaria. La ubicación se toma de forma
puntual al registrar, no se rastrea de forma continua. Los intentos fallidos guardan
metadatos mínimos. Cada nueva entidad con datos personales debe declarar su propósito
y periodo de conservación. Los invitados no están obligados a crear una cuenta.

### VI. Pruebas como criterio de terminado

Cada historia de usuario tiene pruebas automatizadas que cubren sus escenarios de
aceptación. Son obligatorias las pruebas de: reglas de negocio (RN-01 a RN-12),
concurrencia (solicitudes simultáneas de la misma persona) y consultas geoespaciales.
Un PR que rompe pruebas no se fusiona.

### VII. Simplicidad y arquitectura modular

Se empieza por lo más simple que cumpla la especificación (YAGNI). El backend se
organiza por dominios: identidades, espacios, eventos/sesiones, QR, validación,
asistencia y reportes. Toda complejidad adicional (nuevo servicio, Redis, polígonos,
etc.) debe justificarse en la sección *Complexity Tracking* del `plan.md`.

## Restricciones técnicas y de privacidad

- **Stack propuesto**: app móvil React Native + Expo (canal primario); web Next.js +
  TypeScript (panel y canal alternativo); backend único **Spring Boot (Java 21) o
  Laravel** — decisión preliminar registrada en el Paso 5; PostgreSQL + PostGIS para
  datos y geometrías; Docker Compose para desarrollo reproducible.
- Geometrías en SRID 4326; distancias en metros con `geography` / `ST_DWithin`.
- Ambos clientes consumen **la misma API** y comparten las reglas de validación.
- Fuera del alcance mínimo: integración obligatoria con el sistema académico real,
  reconocimiento facial, ubicación infalible en interiores, despliegue multisede y
  pagos/certificados.
- Ningún secreto se versiona: se usa `.env` local y `.env.example` sin valores reales.

## Flujo de trabajo y calidad

- **Ramas**: `main` estable (sin commits directos); `develop` integración;
  `feature/*` trabajo por funcionalidad; `fix/*` correcciones; `hotfix/*` urgencias
  sobre `main`. Las especificaciones Spec Kit se trabajan en `feature/NNN-nombre`.
- Todo cambio entra por **Pull Request** con al menos una revisión de otro integrante.
- Commits descriptivos (convención sugerida: `feat:`, `fix:`, `docs:`, `test:`,
  `chore:`), con nombre y correo de Git configurados para que sean atribuibles.
- El README debe permitir clonar, instalar y ejecutar el proyecto sin instrucciones
  orales.
- El uso de herramientas de IA se declara en el PR o en la documentación
  correspondiente, y se citan las fuentes consultadas.

## Gobernanza

Esta constitución prevalece sobre cualquier otra práctica del proyecto. Cada `plan.md`
incluye una sección *Constitution Check* que debe aprobarse antes de diseñar y
revisarse después del diseño. Las revisiones de PR verifican el cumplimiento.

Las enmiendas se proponen por PR, se discuten con el equipo y se versionan con
SemVer: **MAJOR** al eliminar o redefinir un principio, **MINOR** al añadir un
principio o sección, **PATCH** para aclaraciones de redacción.

**Version**: 1.0.0 | **Ratified**: 2026-10-10 | **Last Amended**: 2026-10-10
