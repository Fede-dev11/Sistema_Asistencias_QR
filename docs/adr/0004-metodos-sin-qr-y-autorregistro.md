# ADR 0004: Métodos de verificación sin QR y autorregistro en eventos abiertos

**Fecha**: 2026-10-25. **Estado**: Propuesto; pendiente de revisión de otro integrante según
Gobernanza. No constituye enmienda constitucional ni declara aprobación.

## Contexto

La feature 002 permite configurar por sesión el método de verificación (`QR_Y_GPS`, `SOLO_QR`,
`SOLO_GPS`, `NINGUNO`) y eventos abiertos sin listado previo, donde cualquier persona se
autorregistra con su cédula. Ambas decisiones fueron pedidas por el usuario (2026-10-25).

## Desviación

- **Principio VI**: `SOLO_GPS` y `NINGUNO` no usan el QR temporal que 001 exige como prueba de
  presencia en la sesión.
- **Principios V y VI**: el autorregistro por cédula no prueba la titularidad de quien la
  escribe (misma limitación que el ADR 0002 de 001).

## Mitigaciones

Unicidad por dispositivo (una asistencia por sesión y dispositivo, HMAC del identificador),
cédula validada con módulo 10, límites de peticiones, GPS con revisión cuando se usa, auditoría
y minimización de datos de externos. Ninguna acredita identidad ni evita todo fraude.

## Limitaciones

El identificador de dispositivo puede reinstalarse o falsificarse; un tercero que conozca una
cédula puede registrarla en un evento abierto; `NINGUNO` solo deja constancia declarativa. No se
comunica garantía antifraude. No usar con datos reales hasta cerrar la revisión de Gobernanza y
el tratamiento LOPDP institucional.

## Alternativas descartadas

QR siempre obligatorio (rechazado por el usuario); eventos abiertos solo con cuenta UNL
(rechazado por el usuario); OTP o verificación presencial (ya descartados en el ADR 0002).
