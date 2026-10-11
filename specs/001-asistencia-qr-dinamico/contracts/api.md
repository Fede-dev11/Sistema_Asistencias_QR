# Contrato preliminar de API: 001-asistencia-qr-dinamico

> Propuesta, no contrato definitivo. Se formalizará en OpenAPI cuando se elija el backend.
> Todas las rutas requieren HTTPS y `Authorization: Bearer <token>` salvo `/auth/login`.

## POST /auth/login
Request: `{ "correo": "...", "password": "..." }` → 200 `{ "accessToken": "...", "roles": ["estudiante"] }`

## POST /sessions/{id}/start
Rol: responsable de la sesión. 200 `{ "id": "...", "estado": "activa" }` · 403 si no es responsable · 409 si no está *programada*.

## POST /sessions/{id}/close
Rol: responsable. 200 `{ "estado": "cerrada" }`. Invalida todos los tokens de la sesión (RN-10).

## GET /sessions/{id}/qr
Rol: responsable. 200 `{ "url": "https://<host>/a/<token>", "expiraEn": "2026-10-10T15:00:30Z", "rotacionS": 30 }`
· 409 si la sesión no está activa. El panel la consulta antes de cada rotación.

## POST /attendance/attempts
Rol: estudiante. Cabecera `Idempotency-Key: <uuid>`.

```json
{
  "token": "base64url...",
  "ubicacion": { "lat": -3.99313, "lon": -79.20422, "precisionM": 12 },
  "medidoEn": "2026-10-10T15:00:21Z",
  "canal": "movil"
}
```

Respuesta 200 (siempre que la solicitud sea válida sintácticamente, incluso si se rechaza):

```json
{ "resultado": "aceptado", "motivo": "OK", "mensaje": "Asistencia registrada", "intentoId": "..." }
```

`resultado` ∈ `aceptado | rechazado | repetir | pendiente_revision`; `motivo` según la tabla de
[data-model.md](../data-model.md). 400 datos mal formados · 401 no autenticado · 429 límite de peticiones.

## GET /sessions/{id}/attendances
Rol: responsable. Query `?estado=` opcional; `Accept: text/csv` para exportar (FR-013).

## POST /attempts/{id}/review
Rol: responsable. `{ "decision": "aprobar" | "rechazar", "razon": "texto obligatorio" }` → 201.
