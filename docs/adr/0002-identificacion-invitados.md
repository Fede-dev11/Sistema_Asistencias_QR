# ADR 0002: Invitados identificados por listado y cédula

**Fecha**: 2026-10-19. **Estado**: Desviación solicitada; pendiente de revisión y aprobación
de otro integrante según Gobernanza. **Revisión prevista**: antes de integrar este incremento
y, como fecha de seguimiento propuesta, 2026-11-19. No constituye enmienda constitucional.

## Decisión expresa del usuario

El encargado precarga cédula y nombres. Un invitado introduce solo su cédula; el servidor
comprueba que no tenga cuenta y que esté autorizado en el listado de ese evento. Si cumple
los demás controles se guarda automáticamente la asistencia. No se añade verificación
presencial, OTP ni un código personal. Si tiene cuenta debe usar credenciales; si no consta
en el listado se rechaza, sin autorregistro ni creación de cuenta permanente.

## Desviación y alcance

Los principios V y VI exigen verificar identidad; el cotejo de cédula y listado no acredita
la titularidad de quien envía. La decisión funcional está resuelta, pero el cumplimiento de
esa obligación no se declara aprobado. La propuesta afecta solo a invitados precargados
sin cuenta en eventos; clases y participantes con cuenta conservan autenticación normal.

## Motivo y alternativas descartadas

Mantener el formulario mínimo y el registro automático con datos controlados previamente
por el encargado, según instrucciones reiteradas del usuario. Se propusieron comprobación
presencial y un código personal adicional; fueron descartados. No se reintroducen bajo
otro mecanismo sin una decisión nueva. La voluntad del usuario no se presenta como prueba
de aprobación de otro integrante del equipo.

## Riesgo y condiciones

Conocer una cédula ajena autorizada y obtener un formulario vigente permite suplantarla.
QR dinámico, GPS opcional, límites de peticiones y auditoría reducen otros riesgos, pero no
resuelven esa titularidad. No se comunica garantía de identidad ni antifraude absoluto.
El plan ofrece un borrador concreto para revisar esta desviación; no autoriza integración
ni uso con datos reales mientras falte su tratamiento conforme a Gobernanza.

## Registro de ejecución

El usuario autorizó expresamente continuar la implementación con los 36 criterios de
`checklists/acceso-privacidad.md` sin marcar. Las marcas permanecen bajo revisión humana.
Se permite preparar código y pruebas con datos ficticios. Esta autorización no se registra
como revisión de otro integrante ni aprueba integración, despliegue o uso de datos reales.
La revisión exigida por Gobernanza permanece pendiente, con responsable de revisión y
fecha de aprobación todavía sin asignar.
