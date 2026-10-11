# Especificación de funcionalidad: Registro de asistencia con QR dinámico y validación geográfica

**Rama de la funcionalidad**: `feature/001-asistencia-qr-dinamico`

**Creado**: 2026-10-10

**Estado**: Borrador — clarificado (ver sección *Clarifications*)

**Entrada**: Descripción del equipo: "Un docente activa una sesión de clase que muestra un
código QR que cambia periódicamente; el estudiante lo escanea desde la app móvil o la web y
el sistema verifica que el código esté vigente, que el estudiante esté autenticado, que no
haya registrado antes y que se encuentre cerca del aula autorizada."

## Problema que resuelve

La firma manual consume tiempo de clase y puede suplantarse; un QR estático se comparte por
foto a quien no está presente. El docente necesita un registro de asistencia rápido y
**auditable**, y el estudiante necesita saber de inmediato si su asistencia quedó registrada
y, si no, por qué. Esta funcionalidad es el núcleo mínimo (MVP) del sistema.

## User Scenarios & Testing *(obligatorio)*

### Historia de usuario 1 — Docente abre una sesión con QR dinámico (Prioridad: P1)

El docente, autenticado, selecciona una sesión de una actividad ya creada con su espacio
(aula) asignado y la inicia. El panel web muestra un QR que se renueva automáticamente
mientras la sesión esté abierta. Al terminar la clase, el docente cierra la sesión.

**Por qué esta prioridad**: sin una sesión activa y un QR vigente no existe ningún otro flujo.

**Prueba independiente**: con datos semilla (un docente, un espacio, una sesión programada),
iniciar la sesión, observar que el QR cambia en el intervalo configurado y cerrarla.

**Escenarios de aceptación**:

1. **Dado** un docente autenticado y una sesión en estado *programada*, **Cuando** pulsa
   "Iniciar sesión", **Entonces** la sesión pasa a *activa* y se muestra un QR.
2. **Dado** una sesión activa, **Cuando** transcurre el intervalo de rotación (30 s),
   **Entonces** se muestra un QR nuevo y el anterior deja de aceptarse tras su caducidad.
3. **Dado** una sesión activa, **Cuando** el docente la cierra, **Entonces** la sesión pasa a
   *cerrada* y ningún token emitido para ella vuelve a aceptarse, aunque no haya caducado.
4. **Dado** un usuario sin rol de docente u organizador de esa actividad, **Cuando** solicita
   el QR de la sesión, **Entonces** el sistema lo rechaza.

---

### Historia de usuario 2 — Estudiante registra su asistencia escaneando el QR (Prioridad: P1)

El estudiante inicia sesión en la app móvil (o en la web), escanea el QR proyectado y concede
permiso de ubicación. El sistema valida y le muestra el resultado con un motivo comprensible.

**Por qué esta prioridad**: es el valor principal del sistema para docentes y estudiantes.

**Prueba independiente**: con una sesión activa, enviar una solicitud de asistencia con un
token vigente y una ubicación dentro del radio y verificar que se registra una sola vez.

**Escenarios de aceptación**:

1. **Dado** un estudiante autenticado, una sesión activa, un token vigente y una ubicación
   dentro de la zona con precisión suficiente, **Cuando** envía su asistencia, **Entonces**
   queda registrada como *aceptada* y se le muestra "Asistencia registrada".
2. **Dado** un token caducado, **Cuando** el estudiante lo envía, **Entonces** el intento se
   rechaza con el motivo "Código QR vencido, escanee el código actual".
3. **Dado** un estudiante con asistencia ya aceptada en la sesión, **Cuando** vuelve a
   enviar, **Entonces** no se crea un segundo registro y se le informa que ya estaba registrado.
4. **Dado** una ubicación reportada fuera de la zona autorizada, **Cuando** envía,
   **Entonces** el intento se rechaza con el motivo "Fuera de la zona de la sesión".
5. **Dado** una sesión cerrada, **Cuando** el estudiante envía cualquier token,
   **Entonces** el intento se rechaza con el motivo "La sesión ya no admite registros".

---

### Historia de usuario 3 — Manejo de ubicación imprecisa (Prioridad: P2)

Cuando la precisión reportada por el dispositivo es insuficiente o la ubicación es antigua,
el sistema no acepta automáticamente: pide repetir la medición y, si persiste, deja el
intento *pendiente de revisión* para el docente.

**Por qué esta prioridad**: evita falsos positivos y falsos negativos en interiores, el
riesgo más alto identificado, pero el MVP puede demostrarse sin ella.

**Prueba independiente**: enviar una solicitud con precisión de 80 m en un aula de radio 20 m
y verificar que el resultado es *repetir medición* y luego *pendiente de revisión*.

**Escenarios de aceptación**:

1. **Dado** una ubicación cuya precisión supera el umbral de la sesión, **Cuando** el
   estudiante envía, **Entonces** se le pide repetir la medición y no se registra asistencia.
2. **Dado** que el círculo de incertidumbre (ubicación ± precisión) cruza el borde de la zona,
   **Cuando** el estudiante envía por segunda vez con el mismo resultado, **Entonces** el
   intento queda *pendiente de revisión* y se le informa.
3. **Dado** un intento pendiente, **Cuando** el docente lo aprueba o rechaza indicando una
   razón, **Entonces** se registra la decisión con responsable, fecha y razón.

---

### Historia de usuario 4 — Docente consulta asistencias en tiempo real (Prioridad: P3)

Durante la sesión, el docente ve la lista de asistencias aceptadas, rechazadas y pendientes
con su motivo, y al final puede exportarla.

**Por qué esta prioridad**: aporta control y trazabilidad, pero no bloquea el registro.

**Prueba independiente**: con varias asistencias de distintos estados, consultar el listado
de la sesión y verificar conteos y motivos.

**Escenarios de aceptación**:

1. **Dado** una sesión con intentos registrados, **Cuando** el docente abre el listado,
   **Entonces** ve cada participante con estado, hora y motivo.
2. **Dado** una sesión cerrada, **Cuando** el docente exporta, **Entonces** obtiene un
   archivo CSV con las asistencias de esa sesión.

---

### Casos límite

- Dos solicitudes simultáneas de la misma persona en la misma sesión → un único registro.
- Pérdida de red durante el envío → el reintento con la misma clave de idempotencia no
  duplica el registro.
- El token caduca mientras el estudiante concede permisos de ubicación → se acepta si el
  token era vigente en el momento del escaneo dentro de un margen de gracia (ver Clarifications).
- El estudiante niega el permiso de cámara o ubicación → se le explica el motivo y la
  alternativa (usar la web o acudir al docente).
- Muchos estudiantes escanean el mismo token → el token **no** se invalida tras el primer uso.
- Reloj del dispositivo desajustado → irrelevante, la caducidad usa la hora del servidor.

## Requirements *(obligatorio)*

### Requisitos funcionales

- **FR-001**: El sistema DEBE permitir a un docente u organizador iniciar y cerrar una
  sesión de la que es responsable.
- **FR-002**: El sistema DEBE emitir, mientras la sesión esté activa, un token QR aleatorio
  criptográficamente seguro asociado a la sesión, con emisión y caducidad, renovado cada
  intervalo configurado (por defecto 30 s).
- **FR-003**: El QR NO DEBE contener datos personales ni secretos; solo una referencia
  opaca al token.
- **FR-004**: El sistema DEBE mostrar el QR únicamente a usuarios autorizados de la sesión.
- **FR-005**: El sistema DEBE recibir intentos de asistencia con token, coordenadas,
  precisión estimada y marca de tiempo de la medición.
- **FR-006**: El sistema DEBE validar en servidor, en este orden: identidad autenticada,
  sesión activa y dentro de la ventana, vigencia del token, ausencia de asistencia previa
  y proximidad geográfica.
- **FR-007**: El sistema DEBE evaluar la proximidad con PostGIS contra el punto y radio de
  la sesión (heredado del espacio si la sesión no lo define).
- **FR-008**: El sistema NO DEBE aceptar automáticamente ubicaciones con precisión peor que
  el umbral o con antigüedad mayor a la permitida (RN-05).
- **FR-009**: El sistema DEBE garantizar como máximo una asistencia aceptada por persona y
  sesión, incluso ante solicitudes concurrentes (RN-01).
- **FR-010**: El sistema DEBE registrar cada intento con su resultado (aceptado, rechazado,
  repetir medición, pendiente de revisión) y un motivo legible (RN-12).
- **FR-011**: El sistema DEBE dejar de aceptar registros al cerrar la sesión (RN-10).
- **FR-012**: El sistema DEBE permitir al docente resolver intentos pendientes registrando
  responsable, fecha y razón (RN-09).
- **FR-013**: El sistema DEBE permitir al docente consultar y exportar a CSV las asistencias
  de una sesión.
- **FR-014**: El sistema DEBE aplicar límite de peticiones por usuario al endpoint de intentos.

### Entidades clave

- **Espacio**: lugar físico reutilizable; nombre, tipo, punto central, radio por defecto.
- **Sesión**: ventana concreta de control; actividad, espacio, inicio, fin, estado
  (programada/activa/cerrada), radio, umbral de precisión, intervalo de rotación.
- **Token QR**: referencia temporal a una sesión; valor (almacenado como hash), emitido, expira.
- **Asistencia**: resultado confirmado por persona y sesión; estado, hora, forma de validación.
- **Intento de asistencia**: cada solicitud; persona, sesión, resultado, motivo, precisión,
  distancia calculada, marca de tiempo (sin almacenar coordenadas exactas en rechazos).
- **Decisión de revisión**: resolución manual de un intento; responsable, fecha, razón.

## Success Criteria *(obligatorio)*

### Resultados medibles

- **SC-001**: Un estudiante con buena señal completa el registro (escaneo → resultado) en
  menos de 10 segundos.
- **SC-002**: La validación en servidor responde en p95 < 2 s, sin contar la adquisición de GPS.
- **SC-003**: En 100 pruebas de solicitudes concurrentes de la misma persona, se crea
  exactamente 1 asistencia en el 100 % de los casos.
- **SC-004**: El 100 % de los tokens caducados o de sesiones cerradas son rechazados.
- **SC-005**: El 100 % de los resultados mostrados al usuario incluyen estado y motivo.
- **SC-006**: Se documentan las tasas de falsos positivos y falsos negativos medidas en al
  menos un aula y un espacio abierto reales.

## Assumptions

- Los estudiantes, docentes, espacios y actividades existen como datos semilla; su gestión
  completa corresponde a las especificaciones futuras `002-autenticacion-identidades` y
  `003-espacios-eventos`.
- La autenticación es con correo y contraseña propios del sistema (sin SSO institucional en
  este incremento).
- La zona se representa como punto + radio; los polígonos quedan para una versión posterior.
- El registro de invitados (sin cuenta) se especifica aparte en `004-invitados`.
- El control de permanencia (segundo escaneo) queda fuera de este incremento.

### Fuera del alcance de este incremento

Invitados, polígonos, control de permanencia, integración con el sistema académico real,
reconocimiento facial, detección avanzada de GPS simulado y notificaciones.

## Clarifications

> Resultado de la etapa **clarify**. Respuestas propuestas por el equipo a partir del
> documento de contexto; deben confirmarse en reunión antes de cerrar `plan`.

### Session 2026-10-10

- Q: ¿Cuánto dura cada token QR y hay margen de gracia? → A: Rotación cada 30 s; cada token
  es válido 30 s más un margen de gracia de 10 s para compensar la latencia de red y la
  concesión de permisos. La validación usa la hora del servidor.
- Q: ¿Qué hacer cuando la precisión del GPS es insuficiente? → A: Umbral por defecto 30 m
  (configurable por sesión). Si la precisión es peor que el umbral, o si el círculo de
  incertidumbre cruza el borde de la zona, se pide repetir; al segundo intento igual, el
  resultado es *pendiente de revisión*. Nunca se acepta automáticamente.
- Q: ¿Qué antigüedad máxima se acepta para la ubicación? → A: 60 segundos respecto a la hora
  del servidor al recibir el intento.
- Q: ¿Un token se invalida tras el primer escaneo? → A: No. Todos los participantes pueden
  usar el mismo token durante su vigencia; la unicidad es por persona y sesión.
- Q: ¿Qué radio por defecto usa un aula? → A: 20 m (rango de pruebas 15–25 m), heredado del
  espacio y ajustable por el docente en la sesión. Es un valor experimental, no una garantía.
