# ADR 0006: Servicio de autenticación universitaria simulado

**Fecha**: 2026-10-26. **Estado**: Propuesto. Pendiente de revisión por otro integrante del equipo
antes de integrar a develop. No constituye enmienda constitucional.

## Contexto

GeoAsist valida contraseñas por sí mismo. La UNL tiene un servicio de identidad institucional, pero
no está disponible para este proyecto, así que no se puede probar la frontera real con él. Además,
varios servicios universitarios necesitan single sign-on (SSO) y GeoAsist debe poder integrarse con
ese mecanismo sin reescribir su dominio. Hace falta un sustituto que se comporte como un sistema
externo para probar esa frontera.

## Decisión

Crear un servicio separado y simulado en `services/auth` (Spring Boot, Java 21) con su propia base
lógica `geoasist_auth`. Implementa un subconjunto de CAS 3.0 (`/cas/login`, `/cas/logout` y
`/cas/p3/serviceValidate`), suficiente para los clientes web y móvil. GeoAsist depende solo del
puerto `IdentityProvider`; la implementación CAS es un adaptador que puede cambiarse por otro sin
tocar el dominio.

## Justificación

- **Principio II (modularidad)**: el servicio es una desviación deliberada. Simula un sistema externo
  que GeoAsist no controla y que otros servicios de la universidad también usarán. Dentro del
  monolito, GeoAsist podría leer sus tablas y las pruebas no ejercitarían la frontera HTTP real
  (redirecciones, canje servidor a servidor, caída del servicio).
- **Principio V (identidad)**: el protocolo CAS 3.0 se asume como **hipótesis** hasta que la UNL
  confirme cuál usa. Si difiere, cambia solo el adaptador. La identidad sigue separada en persona y
  cuenta, con auditoría y sin contraseñas de GeoAsist para cuentas CAS.

## Alternativas descartadas

- **Módulo dentro del monolito**: más simple, pero no prueba la frontera ni evita que GeoAsist lea
  datos del proveedor de identidad. Descartado por el Principio II.
- **Apereo CAS en contenedor**: es una implementación completa y probada, pero pesada de configurar
  con atributos y semillas propias, y oculta la frontera que se quiere documentar. Sigue siendo una
  opción si el equipo prefiere no mantener código propio.
- **OIDC**: protocolo moderno, pero descartado por decisión del usuario para este incremento.

## Consecuencias

- Un despliegue más en `infra/docker-compose.yml` y en CI (job `auth`).
- Código propio de autenticación que requiere pruebas de seguridad (redirecciones abiertas, reuso de
  tickets, concurrencia, cabeceras y registro sin secretos).
- Los datos son ficticios. El tratamiento de datos reales queda pendiente de la LOPDP y de
  Gobernanza.
- Si la UNL usa otro protocolo, el cambio se limita a un adaptador y a la configuración `SSO_*`.

## Revisión

Revisión: requiere aprobación de otro integrante antes de integrar a develop; no se declara aprobado.
