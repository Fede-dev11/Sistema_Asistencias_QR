# Fase 0 — Investigación y decisiones: 001-asistencia-qr-dinamico

Cada decisión registra: **Decisión**, **Justificación** y **Alternativas consideradas**.

## R1. Formato y verificación del token QR

- **Decisión**: token aleatorio de 32 bytes (CSPRNG) codificado en base64url; se almacena
  solo su hash SHA-256 en `tokens_qr` con `sesion_id`, `emitido_en`, `expira_en`.
  El QR contiene una URL del tipo `https://<host>/a/<token>`.
- **Justificación**: no expone datos; permite revocar todos los tokens al cerrar la sesión;
  el hash evita que una fuga de la BD permita reutilizar tokens vigentes.
- **Alternativas**: token firmado (JWT/HMAC) sin persistir cada rotación — menos escrituras,
  pero la revocación al cerrar la sesión depende igualmente de consultar el estado de la
  sesión; se puede adoptar más adelante si el volumen lo exige.

## R2. Vigencia y margen de gracia

- **Decisión**: rotación cada 30 s; válido 30 s + 10 s de gracia; reloj del servidor.
- **Justificación**: reduce el reenvío a distancia sin castigar latencia de red ni la
  concesión de permisos (Clarifications de la spec).
- **Alternativas**: 15 s (más seguro, más fallos en redes lentas); 60 s (más tolerante,
  mayor ventana de reenvío).

## R3. Evaluación geoespacial

- **Decisión**: `ST_DWithin(ubicacion::geography, espacio.centro::geography, radio)` y,
  además, evaluar incertidumbre: si `distancia + precision > radio` y
  `distancia - precision <= radio`, el resultado es *incierto* (repetir / revisión).
- **Justificación**: `geography` calcula en metros sobre SRID 4326; el tratamiento de la
  precisión evita aceptar automáticamente mediciones dudosas (RN-05).
- **Alternativas**: cálculo Haversine en el backend — descartado para mantener la lógica
  espacial en PostGIS y preparar el uso futuro de polígonos (`ST_Contains`).

## R4. Unicidad e idempotencia

- **Decisión**: índice `UNIQUE(sesion_id, persona_id)` en `asistencias`; inserción dentro de
  una transacción con `ON CONFLICT DO NOTHING`; cabecera `Idempotency-Key` generada por el
  cliente por cada escaneo.
- **Justificación**: la base de datos garantiza RN-01 incluso con solicitudes simultáneas;
  la clave evita duplicar intentos al reintentar tras una caída de red.
- **Alternativas**: bloqueo en la aplicación — no protege ante varias instancias del backend.

## R5. Privacidad de la ubicación

- **Decisión**: en intentos rechazados se guarda distancia redondeada y precisión, no las
  coordenadas; en asistencias aceptadas se guarda la coordenada con precisión reducida
  (4 decimales ≈ 11 m) para auditoría.
- **Justificación**: principio V de la constitución y RN-08.

## R6. Limitaciones conocidas (se documentan, no se ocultan)

- Un token puede reenviarse en tiempo real a alguien cercano mientras siga vigente.
- La ubicación reportada por el dispositivo puede falsearse (apps de GPS simulado).
- En interiores el GPS no distingue con fiabilidad aulas contiguas ni pisos distintos.

## R7. Backend (pendiente)

- **Estado**: *NEEDS CLARIFICATION* — comparación Spring Boot vs Laravel y decisión
  preliminar en el Paso 5 (`docs/decisiones/`). El contrato de la API y el modelo de datos
  de esta funcionalidad no dependen de esa elección.
