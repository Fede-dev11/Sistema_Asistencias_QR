# Plan: asistencia con QR, invitados y geolocalización opcional

**Fecha**: 2026-10-19. **Rama detectada**: `001-asistencia-qr-dinamico`.
**Rama declarada en spec**: `feature/001-asistencia-qr-dinamico`; no se cambia la rama del usuario.
**Entrada**: [spec.md](spec.md), constitución 2.0.0 y decisiones posteriores de planificación.

**Estado**: diseño implementado con validación técnica y pendientes externos registrados en
[revisión del incremento](../../docs/validation/revision-incremento.md). Se concretan investigación y diseño;
**el control constitucional no se declara aprobado**: la desviación de identidad de invitados
requiere revisión de otro integrante. Este borrador no habilita integración ni ejecución
indiscriminada de las tareas. No se modifica la constitución ni se atribuye una
aprobación humana inexistente. Las decisiones funcionales antes ambiguas ya están respondidas.

## Resumen

Monolito modular Java/Spring Boot y PostgreSQL con contrato REST común a Next.js y Expo.
Clases: cuenta autenticada más lista autorizada. Eventos: credenciales para cualquier persona
con cuenta; para invitados sin cuenta, solo cédula cotejada contra listado precargado por
el encargado. No se añaden códigos personales, verificación presencial ni autorregistro.

El QR rota cada 30 s y vence a los 40 s según hora de recepción en servidor. Un QR vigente
puede abrir un formulario invitado con una capacidad temporal opaca y un plazo copiado de
`tolerancia_invitado_segundos`, entero >40 configurado por evento. Ese plazo **solo** sirve
al formulario invitado, no amplía QR ni acceso con cuenta. Al enviar, el servidor vuelve a
comprobar cuenta/listado/sesión y consume el inicio dentro de la transacción de asistencia.

GPS está apagado por defecto: no hay permisos ni coordenadas en ese flujo. Habilitarlo requiere
capacidad geográfica; el rechazo automático exige evidencia de campo. Consultar se limita
por responsabilidad o carrera, con lectura global del director institucional. Los roles de
dirección no añaden modificación ni exportación; el responsable exporta solo sus actividades.

Cierre definitivo de actividad inicia cinco años calendario de conservación. Al vencer,
se bloquea consulta nominal y se anonimiza el detalle, auditoría incluida, conservando solo
agregados no identificables. Respaldos y restauraciones forman parte del tratamiento;
una cuenta global o una actividad vigente diferente no se destruyen por ese vencimiento.

## Contexto técnico

| Componente | Propuesta concreta | Control |
|------------|--------------------|---------|
| Backend | Java 21, Spring Boot 4.1.1 MVC/Security/Data JPA, Maven Wrapper 3.9.11 | Dependencias mediante BOM; migraciones y pruebas PostgreSQL |
| Persistencia | PostgreSQL 17.7 y Flyway | Transacciones y unicidad real; no H2 para concurrencia |
| Web | Next.js 16.4.2, React 19.2.3, TypeScript estricto | Sesión de cuenta en memoria; inicio invitado temporal según R5; linter/tipos explícitos |
| Móvil | Expo SDK 57, React Native 0.86, React 19.2.3 | Matriz Expo, SecureStore, Android e iOS |
| Node | 22.20.0 | Versión y lockfiles fijados |
| API | OpenAPI 3.0.3, `/api/v1`, Problem Details | YAML fuente de verdad y validación de contrato en CI |
| Pruebas | JUnit Jupiter 6, Testcontainers 2 administrados por Boot; pruebas de clientes y recorrido web | Probar contra PostgreSQL real y contrato versionado |
| Geografía | Adaptador y migración PostGIS opcionales | Arranque base sin extensión; OFF/REVIEW_ONLY/AUTO |
| Entorno | Docker Compose de desarrollo, perfiles separados, `.env.example` sin secretos | Semillas ficticias; credenciales por configuración local |

Fuentes y alternativas verificadas están en [research.md](research.md), R1-R3.
El [ADR 0001](../../docs/adr/0001-arquitectura-y-contrato.md) propone la confirmación del stack;
no lo etiqueta como aprobado por el equipo. No se adopta Laravel ni un stack duplicado.

**Rendimiento**: SC-001 <10 s escaneo-respuesta y SC-002 p95 <2 s sin adquisición GPS.
Banco inicial técnico: 200 intentos/minuto, diez minutos, cincuenta personas, cinco sesiones,
mezcla definida de resultados y GPS apagado. Es escenario de laboratorio reproducible,
no una capacidad institucional prometida. SC-003: cien ejecuciones de cien solicitudes
simultáneas para la misma persona/sesión, una sola asistencia por ejecución. No se reportan
resultados medidos durante planificación. Si la serialización limita p95, medir antes de
introducir bloqueos más finos, cachés o componentes distribuidos.

**Límites iniciales de laboratorio**: intentos 30/minuto por persona/sesión; apertura invitada
300/minuto por IP/sesión; envío seis/minuto por inicio y 300/minuto por IP. Login cinco/minuto
por identidad y 300/minuto por IP. Cifras configurables para medición, con 429 y Retry-After;
no son políticas institucionales aprobadas. Listado máximo 1000 entradas por petición en el
contrato inicial; cambiarlo exige actualizar el contrato y sus validaciones. Paginación máxima 100.

**Dependencias delimitadas**: autenticación local ficticia para desarrollo; CAS real no se
presume disponible ni se finge mediante semillas. El ámbito académico se siembra con carreras,
paralelos y vínculos explícitos, sin integración automática al sistema académico. Los plazos
legales/base institucional y la operación de copias con datos reales requieren validación
institucional antes de puesta en uso. Son condiciones de entrega, no incógnitas técnicas
que se sustituyan por credenciales o servicios inventados.

## Constitution Check

**Antes del diseño**: decisiones de invitados y retención aclaradas por el usuario. Se
registra una desviación concreta y justificada en Complexity Tracking y ADR 0002. El control
formal sigue condicionado por su aprobación de equipo; el diseño se ofrece como propuesta
reviewable, no como superación automática de ese control.

**Después del diseño**: se documentan las medidas de los diecisiete principios y su evidencia
prevista. No se confunde una medida planificada con código o pruebas ya ejecutados.

| Principio | Tratamiento en el borrador | Estado de diseño |
|-----------|----------------------------|------------------|
| I. SDD | FR/SC mapeados a áreas y tareas; revisión humana e IA declarada al integrar | Desglose y evidencia actualizados en tasks.md; revisión humana pendiente |
| II. Modularidad | Dominios y capas separados; reglas críticas en servidor, una API común | Cubierto en diseño |
| III. SOLID/calidad | Casos de uso con una responsabilidad, puertos solo para proveedores/adaptadores reales; linter/tipos | Cubierto en diseño |
| IV. API | YAML OpenAPI, versionamiento, DTOs, Problem Details, paginación e idempotencia | Dieciocho operaciones implementadas y tipos sincronizados |
| V. Identidad/autorización | Cuenta/persona separadas; ámbitos por actividad/carrera y credenciales locales de desarrollo | Invitados sujetos a desviación V/VI |
| VI. Registro | QR temporal, reloj servidor, cierre transaccional; titularidad de invitado no acreditada por cédula | **Desviación pendiente de revisión/aprobación** |
| VII. GPS | OFF por defecto, sin coordenadas en ese modo; piloto con revisión; AUTO solo con evidencia real | Ensayo de campo previo a AUTO |
| VIII. Integridad | Único sesión/persona, transacciones y bloqueos, migraciones, auditoría antes/nuevo, restore ensayado | Pruebas PostgreSQL y restore aislado ejecutados con fixtures |
| IX. Web | TypeScript estricto, estados y accesibilidad, sin secretos en localStorage | Cubierto en diseño |
| X. Móvil | Android/iOS, permisos en uso, SecureStore y reintentos idempotentes | Cubierto en diseño |
| XI. Privacidad | Cinco años desde cierre definitivo, anonimización de detalle/auditoría y copias; acceso por ámbito | Política de producto resuelta; validación institucional pendiente |
| XII. Pruebas | Escenarios por FR/SC, contrato, concurrencia, rol/ámbito, errores y conectividad | Suites ejecutadas; dispositivo y campo siguen pendientes |
| XIII. Documentación | Research, modelo, contrato, guía y tres ADR propuestos coherentes | Validación documental al final |
| XIV. Colaboración | No cambio automático de rama, sin commits directos; PR y revisión del equipo | Condición de integración |
| XV. DevOps | Versiones fijadas y lockfiles al implementar; Compose y perfiles, configuración sin secretos | Estructura prevista |
| XVI. Observabilidad | Correlación, motivos, métricas y contadores sin secretos/PII; benchmark documentado | Cubierto en diseño |
| XVII. Evolución | Contrato único, incrementos backend/web/móvil y regresión; sin cronograma supuesto | Cubierto en diseño |

No se marca aprobado el Principio VI: el usuario volvió a confirmar el flujo de solo listado,
lo que resuelve su intención, no demuestra titularidad ni aprobación de otro integrante.
La etapa de aprobación del plan/integración conserva esa restricción según Gobernanza.

## Diseño y responsabilidades

| Módulo | Responsabilidad | Dependencias permitidas |
|--------|----------------|--------------------------|
| compartido | Errores, reloj, secretos, bloqueos, idempotencia y regla común de conservación | Infraestructura; sin dependencia de los dominios |
| identidades | Persona, cuenta, credenciales, roles y carrera dirigida | Puerto de proveedor; persistencia; compartido |
| actividades | Modalidad, responsable, carrera/paralelo, estudiantes y lista invitada, cierre definitivo | Identidades por referencias/casos de uso |
| sesiones | Ciclo programada/activa/cerrada y ventana | Actividad |
| qr | Emisión, hash, caducidad y revocación efectiva por estado de sesión | Sesión y reloj servidor |
| asistencia | Intentos ordinarios/invitados, inicio temporal, unicidad e idempotencia | Sesiones, identidades, actividades y puerto geográfico |
| reportes | Consultas paginadas y CSV según ámbito | Lectura autorizada de actividad/asistencia |
| conservación | Vencimiento, bloqueo nominal, agregación segura, eliminación de enlaces, catálogo de copias | Actividad y repositorios; operación de copias |

Los controladores traducen DTOs; los casos de uso coordinan transacciones. El dominio no
importa web/JPA. Los adaptadores PostgreSQL y geográfico implementan puertos concretos;
no se crea una interfaz por clase sin necesidad real. CAS no se implementa en este incremento.

El protocolo de concurrencia se explica en [data-model.md](data-model.md): actividad,
sesión, persona, inicio invitado. Cierre, sustitución de listado, alta de cuenta y envío no
comprueban estados fuera de la transacción. Alta de cuenta no puede competir sin coordinar
la persona. Aprobar revisión que crea asistencia exige sesión activa. Reintentar una clave
anterior no permite leer datos ya vencidos ni crear otro registro después del cierre.

## Trazabilidad para la siguiente etapa

Estas áreas organizan el desglose de [tasks.md](tasks.md), que incluye la correspondencia
concreta de tareas para cada FR/SC y operación del contrato. La trazabilidad no acredita implementación.

| Área de trabajo | Requisitos y criterios de origen |
|-----------------|---------------------------------|
| Identidades, ámbito y elegibilidad | FR-004, FR-006, FR-016, FR-017, FR-019, FR-021; SC-008, SC-011 |
| Actividad/sesión y cierre definitivo | FR-001, FR-011, FR-024; SC-004 |
| QR temporal | FR-002, FR-003; SC-004 |
| Precarga y formulario exclusivo invitado | FR-005, FR-018, FR-019, FR-020; SC-009, SC-010 |
| Registro, unicidad, resultados y límites | FR-005, FR-006, FR-009, FR-010, FR-014; SC-001, SC-002, SC-003, SC-005 |
| Geografía condicional y ensayo | FR-007, FR-008, FR-015; SC-006, SC-007 |
| Revisión y auditoría | FR-012, FR-023; SC-012 |
| Consulta y exportación | FR-013, FR-021; SC-011 |
| Retención y anonimización | FR-022, FR-025; SC-013 |
| Clientes, contrato y entorno | Todos los recorridos anteriores; principios IV, IX, X, XII, XV |

## Estructura del proyecto

### Documentación de esta funcionalidad

```text
specs/001-asistencia-qr-dinamico/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   ├── api.md
│   └── openapi.yaml
├── quickstart.md
├── checklists/acceso-privacidad.md
└── tasks.md                  # desglose actualizado; integración sujeta a revisión

docs/adr/
├── 0001-arquitectura-y-contrato.md
├── 0002-identificacion-invitados.md
└── 0003-conservacion-y-anonimizacion.md
```

### Código fuente previsto, aún no creado

```text
services/api/
├── pom.xml, mvnw, .mvn/
├── src/main/java/ec/edu/unl/geoasist/
│   ├── compartido/           # kernel común sin ciclos entre dominios
│   ├── identidades/
│   ├── actividades/
│   ├── sesiones/
│   ├── qr/
│   ├── asistencia/
│   ├── reportes/
│   └── conservacion/
│       # cada dominio: presentacion, aplicacion, dominio y persistencia
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/          # básicas; geografía en ubicación/perfil separado
└── src/test/                 # reglas, contrato, integración y concurrencia
apps/web/                     # componentes, casos de uso de UI y cliente de API separados
apps/mobile/                  # funcionalidades, pantallas y servicios separados
infra/                        # Compose, perfiles y procedimiento de respaldo/restauración
```

La API será el primer incremento, seguida de web, móvil y validación integrada. Es una
secuencia técnica de dependencias, no un cronograma de equipo aprobado. Las tareas se
han desglosado para revisión; integrar el incremento exige tratar la excepción constitucional
y registrar la aprobación real del equipo según la gobernanza.

## Complexity Tracking

| Desviación | Motivo y alcance | Alternativa descartada | Revisión y autorización |
|------------|------------------|------------------------|--------------------------|
| Principios V/VI: invitado sin prueba de titularidad | Usuario exige registro automático por cédula y listado, solo invitados precargados sin cuenta, sin verificación presencial | Verificación presencial y código personal fueron expresamente descartados | ADR 0002 propuesto; revisión antes de integrar; seguimiento propuesto 2026-11-19; **aprobación de otro integrante no registrada** |

El cotejo no prueba identidad; el riesgo de usar una cédula ajena continúa. La desviación
se hace concreta y revisable, no se oculta cambiando la definición de identidad ni se añade
un control descartado. No hay nueva complejidad distribuida cuya introducción requiera otra
justificación. La fase técnica redactada no sustituye la aprobación del gate constitucional.

## Resultado de esta revisión

Decisiones del usuario incorporadas a la spec: solo listado para invitados y anonimización
al cumplir cinco años desde cierre definitivo. Investigación técnica y documentos de diseño
redactados como **borrador condicionado**. Contrato y enlaces se validan documentalmente;
no hay implementación, pruebas de integración, ensayos de campo o medidas de rendimiento
ejecutados durante esta etapa. La checklist mantiene sus marcas de revisión sin cambios.

**Comprobaciones realizadas**: YAML sin claves duplicadas, 18 operaciones con identificadores
únicos, 191 referencias locales resueltas, 129 ejemplos de respuesta válidos contra esquemas,
enlaces locales de nueve documentos existentes y referencia desde el plan a los 38 FR/SC.
`git diff --check` sin errores. No hay validador integral OAS instalado: estas comprobaciones
estructurales y de ejemplos no sustituyen esa validación ni pruebas de implementación.

## Ajuste durante implementación

Una prueba de arquitectura detectó ciclos al ubicar utilidades transversales en los módulos
de identidad, QR, sesiones y asistencia. Se trasladan al kernel `compartido`, sin depender de
los dominios. La guardia de conservación recibe solo el vencimiento, sin importar entidades
de actividad. Las reglas y las 18 operaciones del contrato se conservan. El chequeo de
ciclos y de independencia del dominio respecto a web/JPA forma parte de las pruebas.
