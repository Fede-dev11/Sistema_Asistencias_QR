# ADR 0003: Conservación por actividad y estadísticas anonimizadas

**Fecha**: 2026-10-19. **Estado**: Decisión de producto confirmada; implementación técnica ensayada con datos ficticios, validación institucional pendiente.

## Decisión confirmada

Cinco años calendario desde el cierre definitivo de clase/evento; al vencer, anonimizar
para conservar estadísticas. No se amplía la vigencia de QR ni formularios temporales.

## Diseño propuesto

Fecha de cierre definitiva e inmutable y fecha de vencimiento calculada. Al vencer se
bloquea acceso identificado aunque la limpieza requiera reintento. Generar agregados por
periodos y modalidades amplios, eliminar detalle individual, vínculos de persona/cuenta,
textos identificativos de auditoría e idempotencia. No guardar hashes de cédula ni IDs
estables de participante/actividad en los agregados. Suprimir grupos pequeños y resultados
deducibles por diferencia; diez personas es un punto técnico inicial, no garantía de anonimato.

El proceso es idempotente y retira solo los datos vencidos de esa actividad. Se eliminan o
sanean también encabezados de actividad/sesiones, responsable y relaciones de autorización;
el control del tratamiento completado no conserva FK a una actividad nominal ni enlaces
que permitan reconstruirla desde el agregado. No borra
cuentas globales con propósito vigente ni vínculos de otra actividad no vencida. Un invitado
huérfano sin otro propósito puede eliminarse. Respaldos, WAL, exportaciones y restores forman
parte del tratamiento: preparar respaldo saneado, retirar copias recuperables antiguas,
restaurar aislado y aplicar el vencimiento antes de habilitar servicio. No marcar finalizado
si quedan copias recuperables pendientes. El procedimiento debe ensayarse antes de datos reales.

## Alternativas descartadas y límites

V008 implementa `anonimizar_vencidas`, `construir_estadisticas` y
`completar_tratamientos`, compartidas por el job y las herramientas de restauración. El
ensayo aislado preservó una cuenta global y otra actividad vigente y retiró encabezados
vencidos de una copia anterior al vencimiento. El catálogo cubrió BACKUP/WAL/EXPORT y un
fallo mantuvo la finalización bloqueada. Las estadísticas se sellan por año/modalidad
completamente vencidos, con al menos diez personas distintas y dos actividades; sin totales
complementarios ni actualizaciones por diferencia. Los lotes insuficientes o posteriores
se descartan, sin retener staging nominal. Esta pérdida estadística es conservadora y debe
ser revisada por la institución. Ver [procedimiento operativo](../operations/conservacion-y-restauracion.md).

Hash o seudónimo estable no equivale a anonimización; conservar auditoría nominal perpetua
contradice el objetivo. Se evita cifrado por actividad con destrucción de claves por su mayor
complejidad para el piloto. La política operativa de copias requiere ensayo y validación de
la institución; no se afirma que PostgreSQL borre físicamente todas las versiones con un DELETE.
Este ADR no es dictamen legal ni acredita aprobación institucional de la base legal.

## Referencias

[research.md, R9](../../specs/001-asistencia-qr-dinamico/research.md),
[NIST SP 800-188](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-188.pdf),
[PostgreSQL PITR](https://www.postgresql.org/docs/current/continuous-archiving.html).
