---

description: "Lista de tareas para 001-asistencia-qr-dinamico"
---

# Tareas: Registro de asistencia con QR dinámico y validación geográfica

**Entrada**: documentos de diseño en `/specs/001-asistencia-qr-dinamico/`

**Prerrequisitos**: plan.md, spec.md, research.md, data-model.md, contracts/api.md

**Pruebas**: obligatorias según el principio VI de la constitución.

## Formato: `[ID] [P?] [Historia] Descripción`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias)
- **[US#]**: historia de usuario de la spec a la que pertenece
- Las rutas de backend usan `services/api/`; el framework se fija en el Paso 5.

---

## Fase 1: Preparación (infraestructura compartida)

- [ ] T001 Crear estructura del monorepo: `services/api/`, `apps/web/`, `apps/mobile/`, `infra/`, `docs/`
- [ ] T002 Crear `infra/docker-compose.yml` con PostgreSQL 16 + PostGIS 3.4
- [ ] T003 [P] Crear `.env.example` (sin secretos) y `.gitignore` raíz
- [ ] T004 [P] Inicializar proyecto backend en `services/api/` con el framework elegido en el Paso 5
- [ ] T005 [P] Inicializar Next.js + TypeScript en `apps/web/`
- [ ] T006 [P] Inicializar Expo + TypeScript en `apps/mobile/`
- [ ] T007 [P] Configurar linters/formateadores en los tres proyectos
- [ ] T008 Actualizar README con instrucciones de arranque local reproducibles

---

## Fase 2: Base (bloquea todas las historias)

- [ ] T009 Migración: habilitar extensión `postgis` en `services/api/db/migrations/`
- [ ] T010 Migración: `personas`, `usuarios`, `roles`, `usuarios_roles`
- [ ] T011 Migración: `espacios` con índice GiST y `sesiones`
- [ ] T012 Migración: `tokens_qr`, `intentos_asistencia`, `asistencias` con `UNIQUE(sesion_id, persona_id)`, `revisiones`
- [ ] T013 [P] Autenticación mínima (login, JWT) y autorización por rol en `services/api/src/identidades/`
- [ ] T014 [P] Manejo uniforme de errores y motivos legibles (tabla de data-model.md)
- [ ] T015 [P] Datos semilla: docente, estudiante, espacio "Aula 101", sesión programada
- [ ] T016 Configurar pruebas de integración con PostGIS real (Testcontainers o contenedor de CI)

**Checkpoint**: base lista; las historias pueden avanzar en paralelo.

---

## Fase 3: Historia 1 — Docente abre una sesión con QR dinámico (P1) 🎯 MVP

**Objetivo**: iniciar, rotar QR y cerrar sesión.
**Prueba independiente**: iniciar sesión semilla, observar rotación cada 30 s, cerrar.

### Pruebas (escribir primero, deben fallar)

- [ ] T017 [P] [US1] Prueba de contrato `POST /sessions/{id}/start|close` y `GET /sessions/{id}/qr`
- [ ] T018 [P] [US1] Prueba de integración: token cerrado no se acepta aunque no haya caducado (RN-10)
- [ ] T019 [P] [US1] Prueba unitaria: emisión de token CSPRNG, hash SHA-256, expiración 30 s + 10 s

### Implementación

- [ ] T020 [US1] Servicio de ciclo de vida de sesión en `services/api/src/sesiones/`
- [ ] T021 [US1] Servicio de emisión/verificación de tokens en `services/api/src/qr/`
- [ ] T022 [US1] Endpoints start/close/qr con autorización de responsable
- [ ] T023 [US1] Página del panel docente con QR auto-renovado en `apps/web/`

**Checkpoint**: Historia 1 funcional y demostrable.

---

## Fase 4: Historia 2 — Estudiante registra asistencia (P1) 🎯 MVP

**Objetivo**: registrar asistencia con validación completa en servidor.
**Prueba independiente**: token vigente + ubicación dentro del radio → una sola asistencia.

### Pruebas

- [ ] T024 [P] [US2] Prueba de contrato `POST /attendance/attempts`
- [ ] T025 [P] [US2] Integración: token vencido, sesión cerrada, fuera de zona, duplicado
- [ ] T026 [P] [US2] Concurrencia: 100 solicitudes simultáneas de la misma persona → 1 asistencia (SC-003)
- [ ] T027 [P] [US2] Integración PostGIS: `ST_DWithin` con puntos a 10 m y 50 m de un aula de 20 m

### Implementación

- [ ] T028 [US2] Validador en orden FR-006 en `services/api/src/asistencia/`
- [ ] T029 [US2] Consulta geoespacial con `geography` en `services/api/src/espacios/`
- [ ] T030 [US2] Registro transaccional con `ON CONFLICT DO NOTHING` e `Idempotency-Key`
- [ ] T031 [US2] Rate limiting por usuario en el endpoint de intentos (FR-014)
- [ ] T032 [P] [US2] Pantalla de escaneo + ubicación + resultado en `apps/mobile/`
- [ ] T033 [P] [US2] Página de registro web con cámara y geolocalización en `apps/web/`

**Checkpoint**: MVP completo (Historias 1 y 2).

---

## Fase 5: Historia 3 — Ubicación imprecisa (P2)

- [ ] T034 [P] [US3] Pruebas: precisión > umbral → `repetir`; segunda vez → `pendiente_revision`; ubicación > 60 s → `UBICACION_ANTIGUA`
- [ ] T035 [US3] Lógica de incertidumbre (R3 de research.md) en el validador
- [ ] T036 [US3] Endpoint `POST /attempts/{id}/review` con razón obligatoria y registro inmutable
- [ ] T037 [US3] Vista de intentos pendientes y botones aprobar/rechazar en `apps/web/`

---

## Fase 6: Historia 4 — Consulta y exportación (P3)

- [ ] T038 [P] [US4] Prueba de contrato `GET /sessions/{id}/attendances` (JSON y CSV)
- [ ] T039 [US4] Servicio de reporte y exportación CSV en `services/api/src/reportes/`
- [ ] T040 [US4] Tabla en tiempo real (polling cada 5 s) en el panel docente

---

## Fase 7: Pulido y evaluación

- [ ] T041 [P] Medir p95 de validación con 200 intentos/minuto (SC-002)
- [ ] T042 [P] Pruebas de campo en un aula y un espacio abierto; registrar falsos positivos/negativos (SC-006)
- [ ] T043 [P] Documentar limitaciones conocidas (R6) en `docs/`
- [ ] T044 Ejecutar `quickstart.md` completo y corregir desviaciones

## Dependencias

- Fase 1 → Fase 2 → (Fase 3 y Fase 4 en paralelo; Fase 4 necesita T021 para verificar tokens)
- Fase 5 depende de Fase 4; Fase 6 depende de Fase 4; Fase 7 al final.
- Dentro de cada historia: pruebas → modelos/servicios → endpoints → interfaz.
