# Plan de implementación: Registro de asistencia con QR dinámico y validación geográfica

**Rama**: `feature/001-asistencia-qr-dinamico` | **Fecha**: 2026-10-10 | **Spec**: [spec.md](./spec.md)

**Entrada**: Especificación de `/specs/001-asistencia-qr-dinamico/spec.md`

## Resumen

El docente inicia una sesión y el backend emite tokens aleatorios de 30 s (+10 s de gracia)
que el panel web muestra como QR. El estudiante, autenticado en la app móvil o la web, envía
token + ubicación + precisión. Una API única valida en servidor la identidad, el estado de la
sesión, la vigencia del token, la unicidad (restricción `UNIQUE` + transacción + clave de
idempotencia) y la proximidad con `ST_DWithin` sobre `geography` en PostGIS. Cada intento se
registra con resultado y motivo legible.

## Contexto técnico

**Lenguaje/Versión**: Backend — *pendiente de la decisión técnica (Paso 5)*: Spring Boot 3
(Java 21) o Laravel 11 (PHP 8.3). Web — TypeScript 5 + Next.js. Móvil — TypeScript +
React Native / Expo.

**Dependencias principales**: librería QR en el cliente web (p. ej. `qrcode`), `expo-camera`
y `expo-location` en móvil, cliente PostGIS/ORM del framework backend elegido.

**Almacenamiento**: PostgreSQL 16 + PostGIS 3.4 (geometrías SRID 4326, distancias con
`geography`). Redis opcional, **no** incluido en este incremento (ver Complexity Tracking).

**Pruebas**: unitarias e integración del framework backend (JUnit 5 + Testcontainers o
PHPUnit/Pest) con una base PostGIS real; Jest para web y móvil.

**Plataforma objetivo**: servidor Linux en contenedor Docker; Android/iOS vía Expo Go;
navegadores modernos con cámara y geolocalización (requieren HTTPS).

**Tipo de proyecto**: monorepo con API + web + móvil.

**Metas de rendimiento**: validación p95 < 2 s sin contar GPS (SC-002); 200 intentos en
el primer minuto de una clase sin degradación.

**Restricciones**: hora del servidor para caducidad; sin datos personales en el QR;
coordenadas exactas no persistidas en intentos rechazados; rate limiting por usuario.

**Escala/Alcance**: piloto académico — decenas de sesiones simultáneas, ~50 participantes
por sesión.

## Constitution Check

*GATE: debe aprobarse antes de la Fase 0 y revisarse tras la Fase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. SDD | Spec aprobada y clarificada antes de este plan | ✅ |
| II. Servidor fuente de verdad | Todas las validaciones (FR-006) en la API; clientes solo capturan | ✅ |
| III. Seguridad y honestidad | Token CSPRNG, hash en BD, HTTPS, rate limit; limitaciones listadas en research.md | ✅ |
| IV. Integridad y trazabilidad | `UNIQUE(sesion_id, persona_id)`, transacción, idempotencia, tabla de intentos y revisiones | ✅ |
| V. Privacidad | Ubicación puntual, sin coordenadas en rechazos, solo distancia y precisión | ✅ |
| VI. Pruebas | Pruebas de RN, concurrencia y PostGIS en tasks.md | ✅ |
| VII. Simplicidad | Punto + radio, sin Redis ni polígonos en este incremento | ✅ |

**Revisión posterior al diseño**: sin violaciones nuevas. Backend pendiente no bloquea el
diseño porque contratos y modelo de datos son independientes del framework.

## Estructura del proyecto

### Documentación (esta funcionalidad)

```text
specs/001-asistencia-qr-dinamico/
├── spec.md          # specify + clarify
├── plan.md          # este archivo (plan)
├── research.md      # Fase 0: decisiones y alternativas
├── data-model.md    # Fase 1: entidades, estados y reglas
├── contracts/
│   └── api.md       # Fase 1: contrato preliminar de la API
├── quickstart.md    # Fase 1: cómo validar la funcionalidad de punta a punta
└── tasks.md         # Fase 2: tareas (tasks)
```

### Código fuente (raíz del repositorio)

```text
services/api/              # Backend (framework según Paso 5)
├── src/
│   ├── identidades/       # autenticación y roles (mínimo para este incremento)
│   ├── espacios/          # espacios y geometrías
│   ├── sesiones/          # ciclo de vida de la sesión
│   ├── qr/                # emisión y verificación de tokens
│   ├── asistencia/        # intentos, validación, asistencias, revisiones
│   └── reportes/          # listado y exportación CSV
├── db/migrations/
└── tests/
    ├── unit/
    ├── integration/       # con PostGIS real
    └── contract/

apps/web/                  # Next.js: panel docente, QR, registro web
apps/mobile/               # Expo: escaneo, ubicación, resultado
infra/docker-compose.yml   # PostgreSQL + PostGIS (+ API)
docs/                      # documentación del proyecto y decisiones
```

**Decisión de estructura**: monorepo con un único backend modular por dominios y dos
clientes que consumen la misma API, según el documento de contexto (sección 12 y 15).

## Complexity Tracking

| Elemento | Por qué se necesita | Alternativa más simple descartada porque |
|----------|---------------------|------------------------------------------|
| Dos clientes (web + móvil) | La móvil es el canal primario y la web es alternativa y panel docente | Solo web: peor acceso a cámara/GPS en algunos dispositivos |
| Redis (aplazado) | Solo si el rate limiting o la verificación de tokens en BD no alcanzan SC-002 | Se empieza con PostgreSQL y límite en memoria del backend |
