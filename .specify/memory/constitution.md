<!--
Sync Impact Report (borrador para revisión humana; retirar antes de fusionar a develop)
- Versión: 1.0.0 → 2.0.0 (MAJOR)
  Motivo: se redefinen principios existentes de forma incompatible:
  * La geolocalización deja de ser obligatoria y pasa a ser condicional (Principio VII).
  * Se retiran de la constitución valores concretos (QR de 30 s, radios, ST_DWithin, SRID):
    pasan a especificaciones y planes.
  * Se fija Spring Boot (Java 21) como backend preliminar; se retira Laravel como opción.
  * Se reorganizan 7 principios en 17 y se añade la clasificación de obligaciones.
- Principios modificados (v1 → v2):
  I. Especificación antes que código → I. Desarrollo guiado por especificaciones
  II. El servidor es la única fuente de verdad → absorbido en II. Arquitectura modular y VI
  III. Seguridad por diseño y honestidad sobre los límites → V. Identidad... y VI. Seguridad del registro
  IV. Integridad y trazabilidad → VIII. Persistencia e integridad (+ IV, VI)
  V. Privacidad y minimización → XI. Participantes externos y privacidad (+ VII)
  VI. Pruebas como criterio de terminado → XII. Pruebas y aseguramiento de calidad
  VII. Simplicidad y arquitectura modular → II. Arquitectura modular + III. SOLID y calidad
- Principios añadidos: III, IV, IX, X, XIII, XIV, XV, XVI, XVII.
- Secciones añadidas: "Clasificación de decisiones"; "Stack preliminar y restricciones";
  "Fuentes y limitaciones de contexto".
- Secciones eliminadas: ninguna (contenido redistribuido).
- Conservado de v1: rama fix/*, declaración de uso de IA, .env.example sin secretos, motivo
  legible en resultados, revisión de intentos inciertos, auditoría de cambios manuales.
- Plantillas: plan-template.md ✅ (su "Constitution Check" se alimenta de esta constitución);
  spec-template.md ✅; tasks-template.md ⚠ indica pruebas "OPTIONAL", el Principio XII las
  exige para reglas críticas — se aplica la constitución (no se modifica la plantilla).
- Artefactos dependientes que requieren revisión (no modificados en esta tarea):
  ⚠ specs/001-asistencia-qr-dinamico/spec.md — trata la geolocalización como requisito P1 y
    fija 30 s; debe re-clarificarse.
  ⚠ specs/001-asistencia-qr-dinamico/plan.md y research.md — mencionan Laravel como opción;
    el contrato no tiene versión /api/v1.
- TODO diferidos: ver sección "Decisiones pendientes de clarificación".
-->

# Constitución de UNL GeoAsist

Sistema de control de asistencia académica e institucional de la Universidad Nacional de Loja
(UNL) mediante códigos QR dinámicos, con plataforma web, aplicación móvil y backend común.
Esta constitución rige por igual al backend, la web y la aplicación móvil.

## Clasificación de decisiones

Toda norma de este documento pertenece a una de tres categorías:

- **Obligación (DEBE / NO DEBE)**: no negociable. Su incumplimiento bloquea la integración
  salvo excepción aprobada según la sección *Gobernanza*.
- **Recomendación (SE RECOMIENDA)**: criterio por defecto. Apartarse de ella exige una
  justificación breve en el `plan.md` o en el Pull Request.
- **Pendiente (PENDIENTE)**: decisión no tomada. NO DEBE tratarse como requisito hasta que se
  resuelva en `clarify` o mediante un ADR.

## Principios fundamentales

### I. Desarrollo guiado por especificaciones (SDD)

- Toda funcionalidad DEBE originarse en una especificación en `specs/NNN-nombre/` y seguir el
  flujo Spec Kit: constitution → specify → clarify → plan → tasks → implement.
- Los requisitos DEBEN expresar necesidades y comportamientos verificables (escenarios
  Dado/Cuando/Entonces y criterios de éxito medibles); NO DEBEN prescribir tecnologías.
- Las ambigüedades marcadas como críticas DEBEN resolverse en `clarify` antes de `plan`.
- DEBE existir trazabilidad requisito (FR/SC) → tarea (T###) → código → prueba.
- Los cambios de alcance DEBEN registrarse en la especificación afectada.
- Todo contenido generado con IA DEBE ser revisado por un integrante humano antes de
  integrarse, y su uso DEBE declararse en el Pull Request.

**Justificación**: evita implementar soluciones a problemas no acordados y permite evaluar
el trabajo contra criterios explícitos.
**Verificación**: cada PR de funcionalidad enlaza su spec y sus tareas; `/speckit-analyze`
no reporta requisitos sin tareas.

### II. Arquitectura modular

- El backend DEBE separar las capas de presentación (controladores REST), aplicación (casos
  de uso), dominio (reglas de negocio) y persistencia (repositorios).
- Las reglas centrales de asistencia DEBEN implementarse y decidirse en el backend; la web y
  la app móvil NO DEBEN ser la única barrera de ninguna regla.
- La lógica de dominio NO DEBE depender de componentes visuales ni de detalles de framework
  web.
- Las dependencias entre módulos DEBEN ser explícitas y no cíclicas.
- NO DEBEN introducirse microservicios, colas, cachés distribuidas u otros componentes sin
  una justificación registrada en *Complexity Tracking* del `plan.md` o en un ADR.
- SE RECOMIENDA un monolito modular organizado por dominios como punto de partida, sujeto a
  validación en la planificación.

**Justificación**: un equipo académico pequeño necesita límites claros sin el costo
operativo de una arquitectura distribuida.
**Verificación**: revisión de PR; las clases de dominio no importan paquetes web ni de JPA
de controladores; diagrama de módulos actualizado.

### III. Principios SOLID y calidad del código

- Cada clase o módulo DEBE tener una responsabilidad identificable (SRP).
- Una regla de negocio NO DEBE implementarse en más de un lugar.
- Los errores DEBEN manejarse explícitamente; NO DEBEN silenciarse excepciones.
- NO DEBEN crearse interfaces, abstracciones o patrones sin un segundo uso real o una
  necesidad de prueba concreta.
- SE RECOMIENDA aplicar OCP, LSP, ISP y DIP cuando reduzcan acoplamiento demostrable.
- El código DEBE pasar el linter y el formateador configurados para su componente.

**Justificación**: el código será mantenido por varios integrantes y por varias cohortes.
**Verificación**: linters en CI; revisión de PR con lista de comprobación.

### IV. Diseño de API REST

- La API DEBE modelar recursos con métodos y códigos HTTP semánticamente correctos.
- Los contratos DEBEN documentarse en OpenAPI y mantenerse sincronizados con la implementación.
- La API DEBE tener una estrategia de versionamiento explícita (SE RECOMIENDA prefijo de
  ruta `/api/v1`).
- Las respuestas de error DEBEN usar un formato único (SE RECOMIENDA RFC 9457 *Problem
  Details*) con un código de motivo legible por máquina y un mensaje legible por personas.
- Toda entrada DEBE validarse en el servidor.
- Las colecciones potencialmente grandes DEBEN paginarse.
- Las operaciones de registro de asistencia DEBEN ser idempotentes: repetir una solicitud
  NO DEBE crear registros adicionales.
- Web y móvil DEBEN consumir el mismo contrato; NO DEBEN existir endpoints exclusivos que
  dupliquen reglas de negocio.

**Justificación**: dos clientes distintos dependen de un contrato estable y predecible.
**Verificación**: pruebas de contrato; la especificación OpenAPI se genera o valida en CI.

### V. Identidad, autenticación y autorización

- Toda operación protegida DEBE verificar identidad y permisos en el servidor.
- Los permisos DEBEN asignarse por rol con mínimo privilegio.
- La identidad de una persona DEBE modelarse separada de su cuenta de acceso, para admitir
  personas sin cuenta (invitados) y varias formas de autenticación.
- Un usuario NO DEBE poder consultar ni modificar datos de otra persona sin un permiso
  explícito.
- Contraseñas DEBEN almacenarse con un algoritmo de hash adaptativo (p. ej. BCrypt o
  Argon2); tokens y credenciales NO DEBEN registrarse en logs ni versionarse.
- Toda comunicación con datos personales o credenciales DEBE usar HTTPS fuera del entorno
  local.
- Las acciones administrativas DEBEN quedar auditadas (quién, qué, cuándo).
- La integración con CAS (servicio institucional de autenticación de la UNL) DEBE hacerse
  detrás de una abstracción de proveedor de identidad, de modo que el dominio no dependa de
  CAS. NO DEBE asumirse protocolo, API ni entorno de pruebas de CAS hasta su confirmación
  institucional (PENDIENTE).

**Justificación**: la asistencia tiene valor académico; un acceso indebido compromete su
validez y la privacidad de los participantes.
**Verificación**: pruebas de autorización por rol (acceso permitido y denegado) por endpoint.

### VI. Seguridad del registro de asistencia

- Los códigos QR DEBEN tener vigencia limitada y validarse en el servidor con la hora del
  servidor. La duración DEBE ser configurable o justificarse en la especificación con
  criterios verificables.
- Los tokens QR DEBEN generarse con un generador criptográficamente seguro y NO DEBEN
  contener datos personales ni secretos.
- La identidad del participante DEBE verificarse de forma independiente del QR.
- El backend DEBE comprobar que la sesión está activa y que el participante está autorizado.
- El cierre de una sesión DEBE impedir nuevos registros aunque existan tokens no caducados.
- Los intentos rechazados DEBEN registrarse con un motivo y comunicarse al usuario de forma
  comprensible.
- La documentación y la interfaz NO DEBEN afirmar prevención absoluta del fraude: el QR
  dinámico por sí solo no demuestra presencia física. Cada control DEBE declarar su
  limitación conocida.

**Justificación**: el valor del sistema depende de que sus garantías sean reales y
comprobables, no de promesas.
**Verificación**: pruebas de token caducado, sesión cerrada, token reutilizado por otra
persona y solicitudes concurrentes; sección de limitaciones en cada `research.md` pertinente.

### VII. Geolocalización y verificación de presencia (condicional)

La geolocalización es una **propuesta del equipo** (PENDIENTE de aprobación). Las reglas
siguientes aplican solo si una especificación clarificada la incorpora:

- La ubicación reportada DEBE validarse en el servidor, considerando la precisión estimada y
  la antigüedad de la medición.
- Las mediciones imprecisas, antiguas o no disponibles NO DEBEN aceptarse automáticamente;
  DEBE existir una ruta de repetición o revisión humana ante posibles falsos rechazos.
- La zona autorizada DEBE definirse por tipo de espacio con criterios documentados y
  validados con mediciones reales; la constitución no fija radios ni algoritmos.
- NO DEBE afirmarse que el GPS garantiza presencia dentro de un aula; las ubicaciones
  simuladas DEBEN figurar en el análisis de riesgos.
- La ubicación DEBE tomarse de forma puntual en el registro, nunca de forma continua, y NO
  DEBE conservarse más información geográfica que la necesaria para auditoría.

**Justificación**: la precisión en interiores es limitada y los datos de ubicación son
sensibles; imponerlos sin evidencia genera falsos rechazos y riesgos de privacidad.
**Verificación**: medición documentada de falsos positivos y falsos negativos en espacios
reales antes de habilitar el rechazo automático.

### VIII. Persistencia e integridad

- Las operaciones que modifican varios registros relacionados DEBEN ejecutarse en una
  transacción.
- La base de datos DEBE imponer integridad referencial y una restricción de unicidad que
  impida más de una asistencia confirmada por persona y sesión.
- Las solicitudes simultáneas DEBEN producir un resultado consistente (un solo registro).
- El esquema DEBE evolucionar solo mediante migraciones versionadas (SE RECOMIENDA Flyway).
- Las entidades JPA NO DEBEN exponerse como contrato público; se usan DTOs.
- Los índices DEBEN justificarse por consultas reales o planes de ejecución.
- Toda modificación manual de una asistencia DEBE registrar responsable, fecha, razón y
  valores anterior y nuevo.
- DEBE documentarse un procedimiento de respaldo y restauración probado antes de un
  despliegue con datos reales.

**Justificación**: la integridad de la asistencia no puede depender solo del código de la
aplicación.
**Verificación**: pruebas de integración con PostgreSQL real (no bases en memoria) para
concurrencia y unicidad.

### IX. Frontend web

- La web (Next.js + React + TypeScript) DEBE separar componentes visuales del acceso a datos
  y de la lógica de aplicación.
- TypeScript DEBE usarse en modo estricto (`strict: true`).
- La validación de formularios en el navegador NO DEBE sustituir la del backend, y las
  reglas críticas de asistencia NO DEBEN duplicarse en el cliente.
- Cada operación remota DEBE mostrar estados de carga, éxito y error comprensibles.
- La interfaz DEBE cumplir criterios básicos de accesibilidad (SE RECOMIENDA WCAG 2.1 nivel
  AA como referencia: contraste, etiquetas, navegación por teclado).
- NO DEBE almacenarse información sensible en `localStorage` u otros almacenamientos
  accesibles por scripts sin justificación.

**Justificación**: el panel docente es crítico durante la clase y debe ser claro y fiable.
**Verificación**: `tsc --noEmit` y linter en CI; revisión de accesibilidad en PR de UI.

### X. Aplicación móvil

- La app (React Native + Expo + TypeScript estricto) DEBE organizarse por funcionalidad, con
  servicios de API separados de las pantallas.
- Los permisos de cámara y ubicación DEBEN solicitarse en el momento de uso, con explicación
  del propósito, y la app DEBE ofrecer una salida clara si se deniegan.
- La app DEBE tolerar conectividad intermitente sin generar registros duplicados (reintentos
  idempotentes).
- La app DEBE mostrar resultados diferenciados: aceptado, rechazado (con motivo) o pendiente.
- Credenciales y sesiones DEBEN guardarse en almacenamiento seguro del sistema (p. ej.
  `expo-secure-store`), nunca en almacenamiento plano.
- El comportamiento DEBE ser consistente en Android e iOS.
- Ningún dato del dispositivo (hora, ubicación, identidad) DEBE considerarse confiable sin
  validación del servidor.

**Justificación**: la app móvil es el canal principal y opera en condiciones de red y
permisos no controladas.
**Verificación**: pruebas en al menos un dispositivo Android y uno iOS (o simulador) por
incremento; escenarios de permiso denegado y red caída.

### XI. Participantes externos y privacidad

- La recolección de datos personales DEBE limitarse a lo necesario para cada propósito
  declarado en la especificación.
- El sistema DEBE diferenciar participantes institucionales de externos.
- NO DEBEN crearse cuentas permanentes de invitados sin su consentimiento.
- La vinculación de asistencias históricas a una cuenta DEBE requerir verificación de
  identidad o de titularidad del contacto.
- Cada especificación con datos personales DEBE declarar quién accede, durante cuánto tiempo
  se conservan y cómo se eliminan.
- El diseño DEBE considerar la Ley Orgánica de Protección de Datos Personales del Ecuador
  (LOPDP); su aplicación concreta y la base legal son PENDIENTES de validación institucional.

**Justificación**: el sistema maneja datos académicos y de personas externas a la UNL.
**Verificación**: sección de privacidad en cada spec con datos personales; revisión en PR.

### XII. Pruebas y aseguramiento de calidad

- Las reglas de negocio DEBEN tener pruebas unitarias.
- Los endpoints DEBEN tener pruebas de integración y de contrato.
- DEBEN existir pruebas de: autenticación y autorización, expiración y reutilización de
  tokens QR, solicitudes simultáneas sin duplicados, escenarios de error y de conectividad.
- Si se implementa geolocalización, DEBEN medirse falsos positivos y falsos negativos.
- Todo defecto corregido en una funcionalidad crítica DEBE añadir una prueba de regresión.
- Las metas de rendimiento y de cobertura DEBEN definirse en la especificación o el plan
  como valores medibles; NO DEBEN reportarse resultados no medidos.
- Un PR con pruebas obligatorias fallidas NO DEBE integrarse.

**Justificación**: los errores en asistencia afectan registros académicos de personas.
**Verificación**: CI ejecuta las suites; el PR enlaza la evidencia de pruebas.

### XIII. Documentación y modelado

- El README DEBE permitir clonar, configurar y ejecutar cada componente sin instrucciones
  orales.
- Las decisiones técnicas significativas DEBEN registrarse como ADR en `docs/adr/`.
- Los contratos de API DEBEN publicarse en OpenAPI.
- SE RECOMIENDAN diagramas C4 (contexto y contenedores) y UML (secuencia, clases o estados)
  cuando aclaren arquitectura o comportamiento.
- Modelos, especificaciones e implementación DEBEN mantenerse coherentes: un cambio en uno
  DEBE reflejarse en los demás dentro del mismo PR.
- La documentación técnica DEBE escribirse en Markdown en el repositorio.
- Las fuentes externas consultadas o incorporadas DEBEN citarse.

**Justificación**: la evaluación y la continuidad del proyecto dependen de evidencia escrita.
**Verificación**: revisión de PR; un integrante distinto al autor reproduce el README.

### XIV. Versionamiento y colaboración

- Se aplica GitFlow simplificado: `main` (estable), `develop` (integración), `feature/*`
  (funcionalidades), `fix/*` (correcciones en desarrollo), `release/*` (preparación de
  versiones, cuando corresponda) y `hotfix/*` (urgencias sobre `main`).
- NO DEBEN hacerse commits directos a `main` ni a `develop`; todo cambio DEBE entrar por PR
  con al menos una revisión de otro integrante.
- Los PR de funcionalidad DEBEN apuntar a `develop`; solo `release/*` y `hotfix/*` se
  fusionan a `main`.
- Los commits DEBEN ser descriptivos y atribuibles (nombre y correo de Git configurados);
  SE RECOMIENDA Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).
- La revisión de PR DEBE comprobar los criterios de aceptación de la spec y el cumplimiento
  de esta constitución.

**Justificación**: la entrega es grupal y evalúa contribuciones individuales verificables.
**Verificación**: protección de ramas en GitHub; historial de PRs y commits por integrante.

### XV. DevOps y entornos reproducibles

- Las herramientas y versiones necesarias DEBEN documentarse (JDK, Node.js, gestor de
  paquetes, Docker, PostgreSQL).
- DEBE existir un `.env.example` sin valores reales; los secretos NO DEBEN versionarse.
- Las dependencias DEBEN fijarse mediante archivos de bloqueo o versiones explícitas
  (Maven/Gradle, `package-lock.json` o equivalente).
- La configuración de desarrollo y producción DEBE estar separada (perfiles o variables).
- SE RECOMIENDA Docker Compose para levantar base de datos y servicios en local.
- SE RECOMIENDA GitHub Actions para compilar, analizar y probar en cada PR cuando sea viable.
- La configuración NO DEBE depender de rutas o recursos de una computadora concreta.

**Justificación**: cualquier integrante o evaluador debe poder reproducir el entorno.
**Verificación**: un integrante distinto ejecuta el proyecto desde cero siguiendo el README.

### XVI. Observabilidad, errores y rendimiento

- El backend DEBE emitir logs estructurados con nivel, módulo y, cuando corresponda, un
  identificador de correlación por solicitud.
- Los logs NO DEBEN contener contraseñas, tokens, coordenadas exactas ni datos personales
  innecesarios.
- Los errores DEBEN clasificarse como validación, negocio, autorización o infraestructura, y
  mapearse a códigos HTTP coherentes.
- Las operaciones críticas (registro de asistencia, emisión de QR) DEBEN tener métricas o
  logs suficientes para diagnosticar fallos.
- Las optimizaciones, cachés (p. ej. Redis) y procesamiento asíncrono DEBEN justificarse
  con mediciones; NO DEBEN introducirse de forma preventiva.
- Los indicadores de rendimiento DEBEN definirse en las especificaciones como metas medibles.

**Justificación**: sin diagnóstico no se pueden investigar reclamos de asistencia.
**Verificación**: revisión de logs en pruebas de integración; metas medidas en `quickstart.md`.

### XVII. Evolución del proyecto

- Esta constitución es única y transversal para backend, web y móvil.
- SE RECOMIENDA planificar por incrementos: (1) backend, persistencia, seguridad y API;
  (2) plataforma web; (3) aplicación móvil; (4) integración, validación y pruebas finales.
  Es una propuesta de organización, no un cronograma aprobado.
- Cada incremento DEBE preservar los contratos y las reglas de negocio existentes; un cambio
  incompatible de contrato DEBE versionarse (Principio IV).

**Justificación**: permite entregar valor parcial sin romper lo ya construido.
**Verificación**: pruebas de regresión y de contrato verdes al cerrar cada incremento.

## Stack preliminar y restricciones

Decisión técnica **preliminar** del equipo (DEBE confirmarse mediante ADR en el Paso 5):

| Componente | Tecnología | Categoría |
|------------|-----------|-----------|
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA/Hibernate, API REST + OpenAPI | Preliminar |
| Web | Next.js, React, TypeScript | Preliminar |
| Móvil | React Native, Expo, TypeScript (Android e iOS) | Preliminar |
| Persistencia | PostgreSQL | Preliminar |
| Geoespacial | PostGIS | PENDIENTE (depende del Principio VII) |
| Autenticación institucional | CAS de la UNL | PENDIENTE (protocolo y acceso) |
| Infraestructura | Git y GitHub; Docker/Docker Compose; GitHub Actions | Recomendación |
| Caché / colas | Redis u otros | Solo con justificación medida |

- Las bibliotecas complementarias (generación de QR, migraciones, pruebas, UI) se eligen en
  cada `plan.md`; NO son obligaciones constitucionales.
- Fuera del alcance mínimo salvo nueva especificación: reconocimiento facial, integración
  obligatoria con el sistema académico, despliegue multisede, pagos y certificados.

## Fuentes y limitaciones de contexto

Esta versión se elaboró a partir de: `README.md`, `Contexto_integral_del_proyecto.md`,
`docs/sdd/README.md`, `specs/001-asistencia-qr-dinamico/` y las plantillas de
`.specify/templates/`. No estaban disponibles en el repositorio: `AGENTS.md`, documentos
ACD1 y APE1, mapa de actores, documento de preguntas de clarificación, comparativa
tecnológica ni guías didácticas de arquitectura, seguridad o SDD. Cuando se incorporen,
esta constitución DEBE revisarse contra ellos.

## Gobernanza

- **Autoridad**: esta constitución prevalece sobre especificaciones, planes, tareas y
  prácticas individuales. Ante conflicto, se corrige el artefacto dependiente o se enmienda
  la constitución; no se ignora.
- **Aplicación**: cada `plan.md` DEBE incluir un *Constitution Check* antes del diseño y
  después de él. Cada PR DEBE indicar que cumple la constitución o enlazar la excepción.
- **Excepciones**: una desviación de una obligación DEBE registrarse en *Complexity
  Tracking* del plan o en un ADR, con motivo, alcance, alternativa descartada y fecha de
  revisión, y DEBE aprobarla al menos otro integrante.
- **Enmiendas**: se proponen por PR que modifica solo este archivo, con descripción del
  cambio y su impacto en specs existentes; requieren revisión humana y aprobación de la
  mayoría del equipo.
- **Versionamiento (SemVer)**: MAJOR al eliminar o redefinir de forma incompatible un
  principio; MINOR al añadir un principio o sección o ampliar materialmente una guía; PATCH
  para aclaraciones de redacción.
- **Registro de cambios**: cada enmienda DEBE dejar constancia en el historial de Git y en
  la descripción del PR (versión anterior → nueva, principios afectados).
- **Compatibilidad**: tras una enmienda, las especificaciones existentes DEBEN revisarse;
  las incompatibilidades se registran como tareas o se resuelven con `/speckit-clarify`.

### Decisiones pendientes de clarificación

1. Aprobación, alcance y viabilidad de la geolocalización (Principio VII y PostGIS).
2. Duración de la vigencia del QR y su justificación (Principio VI).
3. Protocolo, acceso y entorno de pruebas de CAS (Principio V).
4. Aplicación concreta de la LOPDP, base legal y períodos de retención (Principio XI).
5. Confirmación del stack preliminar mediante ADR (Paso 5).
6. Metas de rendimiento y cobertura de pruebas (Principios XII y XVI).
7. Rol y permisos concretos de "autoridades universitarias".

**Version**: 2.0.0 | **Ratified**: 2026-10-10 | **Last Amended**: 2026-10-15
