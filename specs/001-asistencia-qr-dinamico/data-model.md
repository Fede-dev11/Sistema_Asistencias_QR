# Fase 1 — Modelo de datos: 001-asistencia-qr-dinamico

Modelo lógico (independiente del framework). Solo incluye lo necesario para este incremento.

## Entidades

### personas
| Campo | Tipo | Notas |
|-------|------|-------|
| id | UUID PK | |
| tipo | enum(`estudiante`, `docente`, `invitado`) | invitados en spec 004 |
| nombre | texto | |
| correo | texto único | |

### usuarios / roles
- `usuarios(id, persona_id FK, hash_password, estado)`
- `roles(id, nombre)` con valores `admin`, `docente`, `estudiante`
- `usuarios_roles(usuario_id, rol_id)`

### espacios
| Campo | Tipo | Notas |
|-------|------|-------|
| id | UUID PK | |
| nombre | texto | |
| tipo | enum(`aula`, `laboratorio`, `auditorio`, `coliseo`, `estadio`, `abierto`) | |
| centro | `geometry(Point, 4326)` | índice GiST |
| radio_m | entero | por defecto 20 en aulas |

### sesiones
| Campo | Tipo | Notas |
|-------|------|-------|
| id | UUID PK | |
| evento_id | UUID FK | evento simplificado en este incremento |
| espacio_id | UUID FK | |
| responsable_id | UUID FK → usuarios | |
| inicio, fin | timestamptz | ventana permitida |
| estado | enum(`programada`, `activa`, `cerrada`) | |
| radio_m | entero nullable | si es nulo, hereda del espacio (RN-04) |
| umbral_precision_m | entero | por defecto 30 |
| rotacion_s | entero | por defecto 30 |

### tokens_qr
| Campo | Tipo | Notas |
|-------|------|-------|
| id | UUID PK | |
| sesion_id | UUID FK | |
| hash_token | bytea único | SHA-256 del token |
| emitido_en | timestamptz | hora del servidor |
| expira_en | timestamptz | emitido + rotación + gracia |

Índice: `(sesion_id, expira_en)`.

### intentos_asistencia
| Campo | Tipo | Notas |
|-------|------|-------|
| id | UUID PK | |
| sesion_id | UUID FK | |
| persona_id | UUID FK | |
| idempotency_key | texto | único por persona |
| resultado | enum(`aceptado`, `rechazado`, `repetir`, `pendiente_revision`) | |
| motivo | enum + texto legible | ver tabla de motivos |
| distancia_m | entero nullable | redondeada |
| precision_m | entero nullable | |
| medido_en | timestamptz | hora de la medición del dispositivo |
| recibido_en | timestamptz | hora del servidor |

### asistencias
| Campo | Tipo | Notas |
|-------|------|-------|
| id | UUID PK | |
| sesion_id, persona_id | UUID FK | **UNIQUE(sesion_id, persona_id)** (RN-01) |
| intento_id | UUID FK | intento que la originó |
| estado | enum(`confirmada`, `rechazada_por_revision`) | |
| ubicacion | `geometry(Point, 4326)` | precisión reducida (R5) |
| registrado_en | timestamptz | |

### revisiones
`revisiones(id, intento_id FK, revisor_id FK, decision enum(aprobar, rechazar), razon texto, decidido_en)`
— toda decisión manual es inmutable (RN-09).

## Transiciones de estado

```text
Sesión:   programada ──iniciar──▶ activa ──cerrar──▶ cerrada   (sin retorno)

Intento:  recibido ─▶ rechazado            (token vencido, sesión cerrada, fuera de zona, duplicado)
                  ├─▶ aceptado ─▶ crea asistencia confirmada
                  ├─▶ repetir  (precisión insuficiente, 1.ª vez)
                  └─▶ pendiente_revision ─▶ aprobado (crea asistencia) | rechazado
```

## Motivos legibles (RN-12)

| Código | Mensaje al usuario |
|--------|--------------------|
| `OK` | Asistencia registrada |
| `TOKEN_VENCIDO` | Código QR vencido, escanee el código actual |
| `TOKEN_INVALIDO` | Código QR no reconocido |
| `SESION_INACTIVA` | La sesión ya no admite registros |
| `YA_REGISTRADO` | Ya tenía una asistencia registrada en esta sesión |
| `FUERA_DE_ZONA` | Fuera de la zona de la sesión |
| `PRECISION_INSUFICIENTE` | Ubicación imprecisa, intente de nuevo al aire libre o junto a una ventana |
| `UBICACION_ANTIGUA` | Ubicación desactualizada, vuelva a intentarlo |
| `EN_REVISION` | Su registro quedó pendiente de revisión por el docente |
