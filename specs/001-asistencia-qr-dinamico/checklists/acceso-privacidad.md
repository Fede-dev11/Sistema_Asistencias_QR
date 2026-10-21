# Checklist de acceso y privacidad: Registro de asistencia con QR dinámico

**Propósito**: Revisar la completitud, claridad y coherencia de los requisitos de acceso,
invitados, plazos y conservación de datos antes de completar la planificación.
**Creada**: 2026-10-19
**Funcionalidad**: [spec.md](../spec.md)
**Plan de referencia**: [plan.md](../plan.md)
**Profundidad**: Estándar, con prioridad en acceso/autorización y privacidad/conservación.
**Responsable y momento**: Revisor del equipo, durante la revisión de requisitos previa al plan.

**Nota**: Lista personalizada generada con `$speckit-checklist` a partir de las decisiones
del usuario y los documentos actuales de la funcionalidad.
**Propiedad de la revisión**: Las marcas pertenecen al revisor. Solo debe marcar `[x]` cuando
determine que el criterio de calidad de requisitos está satisfecho.
**Significado de las marcas**: `[x]` acredita la revisión del requisito; no significa que
la funcionalidad esté implementada. Todos los elementos se generan sin marcar.

## Completitud de requisitos

- [ ] CHK001 ¿Están definidos por separado los participantes autorizados de una clase y los de un evento, incluida la prohibición de usar la vía de invitado para acceder a una clase? [Completitud, Spec §FR-016, §FR-017]
- [ ] CHK002 ¿Están especificados la precarga exclusiva por el encargado, el uso de los datos del listado y la ausencia de autorregistro libre de invitados? [Completitud, Spec §FR-018, §FR-019]
- [ ] CHK003 ¿La matriz de permisos distingue consulta, exportación, modificación y resolución de pendientes, con el alcance de cada rol expresado explícitamente? [Completitud, Spec §FR-012, §FR-013, §FR-021, §Acceso, conservación y auditoría]
- [ ] CHK004 ¿Se identifica qué datos personales se necesitan para cada flujo y cuáles se incluyen en la conservación de cinco años, separándolos de tokens e inicios temporales? [Completitud, Spec §FR-005, §FR-018, §FR-022, §Entidades clave]

## Claridad de requisitos

- [ ] CHK005 ¿Están diferenciados sin ambigüedad el director con alcance institucional y el director de carrera, así como el significado de actividad propia, carrera y paralelo? [Claridad, Spec §FR-021, §Entidades clave]
- [ ] CHK006 ¿Está definido qué significa que una cédula tenga cuenta, incluido el tratamiento de cuentas inactivas y de una cuenta creada después de precargar al invitado? [Claridad, Gap, Spec §FR-019]
- [ ] CHK007 ¿La expresión «cédula válida» tiene reglas verificables de formato, normalización y coincidencia con el listado, sin confundirlas con prueba de titularidad? [Claridad, Ambiguity, Spec §FR-006, §FR-018, Historia de usuario 2]
- [ ] CHK008 ¿La duración configurable de invitados tiene unidad, límite inferior, tratamiento del límite superior o ausencia de este y regla cuando no se ha configurado un valor? [Claridad, Gap, Spec §FR-020]

## Coherencia de requisitos

- [ ] CHK009 ¿Se expresa de forma consistente que `tolerancia_invitado_segundos` solo corresponde al formulario de invitados sin cuenta y no amplía los 40 s del QR compartido ni el plazo de participantes con cuenta? [Consistencia, Spec §FR-002, §FR-019, §FR-020, §SC-004, §SC-010]
- [ ] CHK010 ¿Los escenarios, requisitos y aclaraciones actuales coinciden en rechazar invitados fuera del listado, dejando explícitamente sustituida la propuesta anterior de registrar cédula y nombres libremente? [Consistencia, Spec §FR-018, §FR-019, §Clarifications]
- [ ] CHK011 ¿Los requisitos y escenarios geográficos se declaran condicionales y son compatibles con no solicitar ni almacenar ubicación cuando el control está desactivado? [Consistencia, Spec §FR-005, §FR-007, §FR-008, §FR-015, §SC-007]
- [ ] CHK012 ¿Se ha resuelto explícitamente la compatibilidad entre la identificación por cédula y listado de invitados y la verificación de identidad exigida por la constitución, conservando la limitación documentada y sin presumir una excepción aprobada? [Consistencia, Conflict, Spec §FR-006, §Assumptions; Constitución §VI; Plan §Constitution Check]

## Calidad de criterios de aceptación

- [ ] CHK013 ¿Los criterios temporales distinguen emisión del QR, recepción del intento e inicio validado del formulario, y especifican el resultado justo antes y al cumplirse cada límite? [Medibilidad, Spec §FR-002, §FR-020, §SC-004, §SC-010]
- [ ] CHK014 ¿SC-003 define sin ambigüedad cuántas solicitudes concurrentes y cuántas repeticiones componen las «100 pruebas», así como el resultado esperado por persona y sesión? [Medibilidad, Ambiguity, Spec §FR-009, §SC-003]
- [ ] CHK015 ¿Los criterios de autorización abarcan accesos permitidos y denegados para cada rol, consultas por listado y por identificador, y exportaciones sin permiso específico? [Cobertura de aceptación, Spec §FR-021, §SC-008, §SC-009, §SC-011]
- [ ] CHK016 ¿El plazo provisional de cinco años tiene un origen de cómputo y una condición de vencimiento definidos de modo que pueda evaluarse objetivamente la conservación de cada registro? [Medibilidad, Gap, Spec §FR-022]

## Cobertura de escenarios

- [ ] CHK017 ¿El flujo principal de invitado precargado especifica que solo introduce la cédula y que los nombres y demás datos proceden del listado del evento? [Cobertura principal, Spec §FR-018, §SC-009, Historia de usuario 2]
- [ ] CHK018 ¿Están documentadas las alternativas ante cédula con cuenta, cédula ausente del listado del evento y cédula registrada únicamente en otro evento, con resultados diferenciados y comprensibles? [Cobertura alternativa, Gap, Spec §FR-018, §FR-019, §FR-010]
- [ ] CHK019 ¿Los requisitos de excepción diferencian QR vencido antes de abrir el formulario, QR vencido después de una apertura válida, vencimiento del plazo de invitado y cierre de sesión? [Cobertura de excepción, Spec §FR-002, §FR-011, §FR-020, §SC-010]
- [ ] CHK020 ¿La recuperación de un envío interrumpido especifica si puede reintentarse dentro del plazo original y cómo se informa una asistencia ya guardada, sin reiniciar el plazo ni duplicar registros? [Cobertura de recuperación, Gap, Spec §FR-009, §FR-020, §Casos límite]

## Cobertura de casos límite

- [ ] CHK021 ¿Está definido el resultado cuando el invitado pasa a tener cuenta o deja de estar autorizado en el listado entre la apertura del formulario y el envío de su cédula? [Casos límite, Spec §FR-019, §FR-020]
- [ ] CHK022 ¿Los requisitos de carga del listado resuelven cédulas duplicadas, nombres discordantes y datos inválidos sin dejar incierta la identidad de la entrada autorizada? [Casos límite, Gap, Spec §FR-018]
- [ ] CHK023 ¿Está delimitado el acceso del director de carrera a actividades sin carrera, compartidas entre carreras o reasignadas, y el efecto de un cambio de carrera del propio director? [Casos límite, Gap, Spec §FR-021, §Acceso, conservación y auditoría]
- [ ] CHK024 ¿Se especifica el efecto de modificar la tolerancia del evento cuando ya existen formularios abiertos, manteniendo claro cuál es el plazo aplicable a cada inicio? [Casos límite, Gap, Spec §FR-020, §SC-010]

## Requisitos no funcionales

- [ ] CHK025 ¿Los requisitos delimitan la información que puede mostrarse antes de autenticar al participante, para orientar sobre cuenta o listado sin revelar nombres ni datos de terceros? [Privacidad, Gap, Spec §FR-010, §FR-019, §FR-021]
- [ ] CHK026 ¿La información necesaria para auditoría está diferenciada de lo que debe excluirse de los registros operativos, como tokens, credenciales y datos personales innecesarios? [Seguridad y privacidad, Spec §FR-023; Constitución §V, §XVI]
- [ ] CHK027 ¿Las metas de tiempo de registro y respuesta identifican población, carga concurrente, intervalo de medición y exclusión de adquisición de ubicación, sin dar por aprobadas cifras del plan anterior? [Rendimiento, Gap, Spec §SC-001, §SC-002; Plan §Contexto técnico]
- [ ] CHK028 ¿Se especifican accesibilidad y estados comprensibles de espera, error, plazo vencido y asistencia confirmada para el formulario de invitados y el acceso con credenciales? [Usabilidad y accesibilidad, Gap, Spec §FR-010, §FR-019, §FR-020; Constitución §IX, §X]

## Dependencias y supuestos

- [ ] CHK029 ¿Se distingue la autenticación con cuentas semilla del acceso institucional real y se explicita qué depende de confirmar el proveedor y la disponibilidad de CAS? [Dependencia, Assumption, Spec §FR-017, §Assumptions]
- [ ] CHK030 ¿Están identificadas las fuentes de las listas autorizadas de clase, los vínculos actividad-carrera-paralelo y las asignaciones de directores, sin asumir una integración académica fuera de alcance? [Dependencia, Assumption, Spec §FR-016, §FR-021, §Assumptions]
- [ ] CHK031 ¿El procedimiento de precarga del encargado define formato, campos mínimos, volumen admitido y tratamiento de errores parciales o correcciones de un listado? [Dependencia, Gap, Spec §FR-018]
- [ ] CHK032 ¿Está identificado qué contenido del plan, investigación, modelo, contrato y tareas debe actualizarse para representar invitados, acceso por carrera y GPS opcional, antes de considerarlo coherente con la spec vigente? [Trazabilidad, Conflict, Spec §FR-015 a §FR-023; Plan §Constitution Check; Tasks §Fases 2, 4 y 6]

## Ambigüedades y conflictos pendientes

- [ ] CHK033 ¿Se ha definido si al finalizar los cinco años se eliminan o anonimizan los datos, qué información queda para auditoría y cómo se aplica la decisión a respaldos y restauraciones? [Ambigüedad, Gap, Spec §FR-022, §Acceso, conservación y auditoría; Constitución §XI]
- [ ] CHK034 ¿Está resuelto si los directores pueden exportar y, en caso afirmativo, con qué alcance, sin equiparar automáticamente consulta con exportación o modificación? [Ambigüedad, Spec §FR-013, §FR-021, §Assumptions]
- [ ] CHK035 ¿Los valores geográficos experimentales y la decisión de habilitar rechazo automático tienen criterios de aprobación definidos, sin presentar propuestas antiguas como requisitos ya validados? [Ambigüedad, Assumption, Spec §FR-007, §FR-008, §SC-006, §Clarifications; Constitución §VII]
- [ ] CHK036 ¿Las referencias RN-01, RN-05, RN-09, RN-10 y RN-12 remiten a reglas accesibles e inequívocas o se han sustituido por requisitos vigentes identificables? [Trazabilidad, Gap, Spec §FR-008 a §FR-012]

## Notas

- Marcar `[x]` solo después de revisar y considerar satisfecho el criterio de calidad de requisitos.
- Dejar sin marcar los elementos que requieran aclaración, corrección o evaluación del revisor.
- `$speckit-implement` utiliza el estado de la checklist como condición de avance y no debe cambiar sus marcas.
- `checklists/requirements.md`, si se genera, tiene un ciclo separado mantenido por `$speckit-specify` y `$speckit-clarify`.
- Esta lista no aprueba excepciones constitucionales ni modifica las decisiones ya tomadas por el usuario.
- Registrar hallazgos y enlaces de evidencia junto al elemento correspondiente; un elemento sin marcar no implica por sí solo que el requisito sea incorrecto.
- Las referencias `Spec` corresponden a [spec.md](../spec.md), `Plan` a [plan.md](../plan.md), `Tasks` a [tasks.md](../tasks.md) y `Constitución` a [constitution.md](../../../.specify/memory/constitution.md).
- Los identificadores CHK son consecutivos dentro de este archivo; futuras ejecuciones deben conservar el contenido existente y continuar la numeración.
