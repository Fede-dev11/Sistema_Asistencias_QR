# ADR 0001: Monolito modular y contrato común

**Fecha**: 2026-10-19. **Estado**: Propuesto para revisión del equipo.

## Contexto

UNL GeoAsist comparte reglas entre web y móvil. El alcance incluye clases, eventos,
invitados precargados, permisos por carrera y GPS opcional. La constitución fija un stack
preliminar; el plan anterior mantenía una comparación con Laravel ya retirada de ese contexto.

## Decisión propuesta

Java 21 + Spring Boot 4.1.x (MVC, Security y JPA mediante BOM), Maven Wrapper,
PostgreSQL 17 y Flyway. Next.js 16/React 19/TypeScript estricto en web; Expo SDK 57 y su
matriz compatible en móvil. Monorepo `services/api`, `apps/web`, `apps/mobile`, `infra`.
Capas por dominio, DTOs y OpenAPI 3.0.3 versionado `/api/v1` común. Autenticación local
ficticia mediante adaptador; CAS institucional no se simula como integración real.
PostGIS es adaptador/migración opcional, no condición de arranque sin GPS.

## Alternativas y consecuencias

Microservicios, Redis y colas añaden operación sin medición que la justifique. JWT sin
estado no ofrece revocación simple para este monolito. Boot 3.5 necesita resolver su soporte
OSS finalizado. Spring Boot 4 requiere usar dependencias compatibles y no arrastrar versiones
JUnit/springdoc de Boot 3. Familias de versiones deben convertirse en parches fijados y
lockfiles al implementar; no se aprueba una actualización automática perpetua.

## Fuentes y validación

Ver decisiones R1-R3 y R8 en [research.md](../../specs/001-asistencia-qr-dinamico/research.md).
Validar compilación, contrato y suites al implementar. Este ADR no acredita esas ejecuciones
ni la aprobación humana requerida por Gobernanza.

## Registro de ejecución

Se inicia preparación técnica tras autorización del usuario de continuar con la checklist
pendiente. La revisión humana del stack y la integración conserva estado propuesto; no
se atribuye una aprobación de equipo inexistente. Versiones fijadas en los manifiestos y
lockfiles se verificaron mediante compilación y suites.

La implementación fija Spring Boot 4.1.1, Maven 3.9.11, PostgreSQL 17.7, Next.js 16.4.2,
React 19.2.3, Expo 57.0.30 y React Native 0.86.3. La prueba de arquitectura comprueba que
los módulos no forman ciclos. El contrato contiene dieciocho operaciones y 129 ejemplos
validados, con tipos web/móvil sincronizados. La conservación comparte el procedimiento
versionado de PostgreSQL entre el job y la restauración aislada. Los resultados finales y
sus límites constan en [revisión del incremento](../validation/revision-incremento.md).
El estado del ADR sigue propuesto: los resultados técnicos no constituyen aprobación de equipo.
