# Implementation Plan: Servicio de autenticación universitaria simulado (SSO)

**Branch**: `feature/004-autenticacion-sso` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-autenticacion-sso/spec.md`

## Summary

Nuevo servicio `services/auth` que simula el servicio institucional con un subconjunto de CAS 3.0:
página de login, sesión SSO por cookie, tickets de servicio de un uso y 10 s, validación servidor a
servidor con atributos (correo, cédula, nombres, roles, carreras, activo), cierre de sesión, consulta
de estado con clave de servicio y administración mínima de usuarios y servicios. GeoAsist canjea el
ticket en `POST /auth/cas`, provisiona la cuenta local por cédula y emite su token opaco de siempre.
Web usa redirección y `/auth/callback`; móvil usa el navegador del sistema con esquema `geoasist://`.
El inicio local por contraseña queda tras una bandera, solo para pruebas. Decisiones en [research.md](research.md).

## Technical Context

**Language/Version**: Java 21 (ambos servicios); TypeScript estricto (web, móvil)

**Primary Dependencies**: Spring Boot 4.1.1 (Web MVC, Security solo para `PasswordEncoder`/cabeceras, JDBC, Flyway,
Thymeleaf en `services/auth`); `RestClient` de Spring en GeoAsist para validar; `expo-web-browser` y
`expo-linking` en móvil (de la matriz Expo SDK 57). Sin dependencias nuevas en web.

**Storage**: PostgreSQL 17.7; base separada `geoasist_auth` para el servicio

**Testing**: JUnit 6 + Testcontainers en ambos servicios; prueba de contrato del adaptador de GeoAsist contra
un servidor HTTP falso (respuestas CAS JSON grabadas); Playwright para el recorrido web completo

**Target Platform**: contenedores Linux; navegador; Android/iOS

**Project Type**: dos servicios web + clientes web y móvil

**Performance Goals**: SC-001 < 30 s de punta a punta; validación de ticket < 500 ms p95 en local

**Constraints**: HTTPS fuera de dev; tickets de un uso y 10 s; sin redirecciones abiertas; sin secretos en logs

**Scale/Scope**: miles de usuarios ficticios; decenas de servicios registrados

## Constitution Check

| Principio | Tratamiento | Estado |
|---|---|---|
| I SDD | Spec aclarado, FR/SC → tareas | Cumple |
| II Modularidad | Servicio separado | **Justificado** (Complexity Tracking, ADR 0006) |
| III SOLID | `IdentityProvider` pasa a tener dos implementaciones reales (CAS y local de pruebas) | Cumple |
| IV API | Contrato único de GeoAsist actualizado; contrato propio del servicio | Cumple |
| V Identidad | Persona/cuenta separadas; BCrypt; auditoría; CAS detrás del puerto. Protocolo asumido como hipótesis | **Pendiente de confirmación UNL** (ADR 0006) |
| VI Registro | Sin cambios en reglas de asistencia | Cumple |
| VIII Integridad | Consumo atómico de tickets; provisión en transacción | Cumple |
| IX Web | Token en memoria; callback sin guardar ticket | Cumple |
| X Móvil | Navegador del sistema, nonce, SecureStore para el token de GeoAsist | Cumple |
| XI Privacidad | Datos ficticios; registro de ingresos 90 días; atributos mínimos | Cumple; LOPDP pendiente |
| XII Pruebas | Tickets (reuso, servicio ajeno, vencido, concurrencia), redirección abierta, provisión, autorización | Cumple |
| XIV Colaboración | Rama `feature/004-…` desde 003; PR a `develop` tras 002/003 | Condición |

## Project Structure

### Documentation (this feature)

```text
specs/004-autenticacion-sso/
├── plan.md, research.md, data-model.md, quickstart.md
├── contracts/api.md            # + auth-openapi.yaml (tarea)
├── checklists/requirements.md
└── tasks.md
```

### Source Code

```text
services/auth/                                   # NUEVO
├── pom.xml, mvnw, Dockerfile
└── src/main/java/ec/edu/unl/sso/
    ├── usuarios/{presentacion,aplicacion,persistencia}
    ├── cas/{presentacion,aplicacion,persistencia}   # login, logout, tickets, validate
    ├── servicios/{aplicacion,persistencia}
    └── compartido/                                  # Cedula, límites, auditoría, secretos
    src/main/resources/{templates,db/migration,application.yml}
services/api/src/main/java/ec/edu/unl/geoasist/identidades/
    ├── aplicacion/CasIdentityProvider.java, AccountProvisioning.java   # NUEVO
    ├── aplicacion/DevelopmentIdentityProvider.java                     # tras bandera local-login
    └── presentacion/AuthController.java                                # /auth/cas, /auth/sso-config
apps/web/src/app/auth/callback/page.tsx, features/auth/*                # botón SSO, callback
apps/mobile/src/features/auth/*                                         # navegador del sistema
infra/docker-compose.yml, infra/db-init/01-auth-db.sql
```

**Structure Decision**: dos servicios independientes; GeoAsist conoce al servicio solo por HTTP y configuración.

## Complexity Tracking

| Desviación | Motivo | Alternativa descartada | Revisión |
|---|---|---|---|
| Principio II: servicio adicional | Simular un sistema externo real y compartido por varios servicios; probar la frontera HTTP | Módulo dentro del monolito (no prueba la frontera; GeoAsist podría leer sus tablas) | ADR 0006 propuesto; revisión del equipo |
| Principio V: protocolo CAS 3.0 asumido | Decisión del usuario mientras la UNL confirma | Esperar confirmación (bloquea el trabajo) | ADR 0006; solo cambia el adaptador si difiere |
