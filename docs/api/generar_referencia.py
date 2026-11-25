#!/usr/bin/env python3
"""Genera la referencia Quarto del API desde el contrato OpenAPI.

El contrato `specs/001-asistencia-qr-dinamico/contracts/openapi.yaml` es la
fuente única. Este script se ejecuta como `pre-render` de Quarto y produce:

- `referencia/<tag>.qmd`: una página por funcionalidad con cada operación,
  parámetros, cuerpo, respuestas, ejemplos JSON y una llamada `curl`.
- `referencia/index.qmd`: índice de funcionalidades.
- `esquemas.qmd`: todos los esquemas de `components.schemas`.
- `openapi.yaml`: copia del contrato para la página Swagger UI publicada.

Solo usa la biblioteca estándar y PyYAML (`pip install pyyaml`).
"""

from __future__ import annotations

import json
import re
import shutil
from pathlib import Path
from urllib.parse import quote

import yaml

AQUI = Path(__file__).resolve().parent
RAIZ = AQUI.parent.parent
CONTRATO = RAIZ / "specs" / "001-asistencia-qr-dinamico" / "contracts" / "openapi.yaml"
REFERENCIA = AQUI / "referencia"
METODOS = ("get", "post", "put", "patch", "delete")

# Etiquetas estables y su descripción. El contrato declara las mismas.
TAGS = [
    ("auth", "Autenticación y cuenta"),
    ("activities", "Clases y eventos"),
    ("sessions", "Sesiones y QR"),
    ("attendance", "Registro de asistencia"),
    ("guest-registrations", "Invitados y eventos abiertos"),
    ("attempts", "Revisión de intentos"),
    ("corrections", "Correcciones y justificaciones"),
    ("admin", "Administración académica"),
    ("spaces", "Espacios"),
    ("stats", "Estadísticas e historial"),
]

ROLES = {
    "pub": "Público (sin autenticación)",
    "inv": "Invitado sin cuenta",
    "cta": "Cualquier cuenta",
    "est": "Estudiante / participante",
    "doc": "Docente",
    "enc": "Encargado",
    "dir": "Director de carrera o institucional",
    "adm": "Administrador",
}

RESP = ["doc", "enc"]
READ = ["doc", "enc", "dir"]


def cargar_contrato() -> dict:
    with CONTRATO.open(encoding="utf-8") as archivo:
        return yaml.safe_load(archivo)


def resolver(obj, spec: dict):
    """Resuelve cadenas de `$ref` locales."""
    visto = 0
    while isinstance(obj, dict) and "$ref" in obj:
        ref = obj["$ref"]
        if not ref.startswith("#/"):
            return obj
        actual = spec
        for parte in ref[2:].split("/"):
            actual = actual[parte.replace("~1", "/").replace("~0", "~")]
        obj = actual
        visto += 1
        if visto > 30:
            break
    return obj


def nombre_ref(raw) -> str | None:
    if isinstance(raw, dict) and "$ref" in raw:
        return raw["$ref"].split("/")[-1]
    return None


def tipo(raw, spec: dict) -> str:
    ref = nombre_ref(raw)
    if ref:
        return ref
    s = resolver(raw, spec) or {}
    t = s.get("type")
    if t == "array":
        return f"lista de {tipo(s.get('items', {}), spec)}"
    if t == "object" or "properties" in s:
        return "objeto"
    if not t and s.get("enum"):
        t = "string"
    base = t or "objeto"
    if s.get("format"):
        base += f" ({s['format']})"
    if s.get("nullable"):
        base += " | null"
    return base


def reglas(raw, spec: dict) -> str:
    s = resolver(raw, spec) or {}
    partes: list[str] = []
    if s.get("enum"):
        partes.append("valores: " + " | ".join(str(v) for v in s["enum"]))
    if s.get("format"):
        partes.append(f"formato {s['format']}")
    if s.get("pattern"):
        partes.append(f"patrón {s['pattern']}")
    for clave, etiqueta in (
        ("minLength", "mín. longitud"),
        ("maxLength", "máx. longitud"),
        ("minimum", "mín."),
        ("maximum", "máx."),
        ("minItems", "mín. elementos"),
        ("maxItems", "máx. elementos"),
    ):
        if s.get(clave) is not None:
            partes.append(f"{etiqueta} {s[clave]}")
    if s.get("default") is not None:
        partes.append(f"por defecto {s['default']}")
    if s.get("nullable"):
        partes.append("puede ser null")
    return "; ".join(partes)


def campos(raw, spec: dict, prefijo: str = "", profundidad: int = 0) -> list[dict]:
    """Aplana propiedades (incluye un nivel de objetos anidados)."""
    s = resolver(raw, spec) or {}
    if "allOf" in s:
        filas: list[dict] = []
        for parte in s["allOf"]:
            filas.extend(campos(parte, spec, prefijo, profundidad))
        return filas
    requeridos = set(s.get("required", []))
    filas = []
    for nombre, prop in (s.get("properties") or {}).items():
        p = resolver(prop, spec) or {}
        fila = {
            "name": prefijo + nombre,
            "type": tipo(prop, spec),
            "required": nombre in requeridos,
            "desc": (p.get("description") or "").strip(),
            "rules": reglas(prop, spec),
        }
        filas.append(fila)
        if profundidad < 1:
            if p.get("properties"):
                filas.extend(campos(prop, spec, f"{prefijo}{nombre}.", profundidad + 1))
            elif p.get("type") == "array" and (resolver(p.get("items", {}), spec) or {}).get("properties"):
                filas.extend(campos(p.get("items", {}), spec, f"{prefijo}{nombre}[].", profundidad + 1))
    return filas


def ejemplo(raw, spec: dict, profundidad: int = 0):
    """Genera un ejemplo razonable cuando el contrato no trae uno explícito."""
    if profundidad > 4:
        return "..."
    s = resolver(raw, spec) or {}
    if "example" in s:
        return s["example"]
    if "examples" in s and s["examples"]:
        primero = next(iter(s["examples"].values()))
        if isinstance(primero, dict) and "value" in primero:
            return primero["value"]
        return primero
    if s.get("default") is not None:
        return s["default"]
    if s.get("enum"):
        return next((v for v in s["enum"] if v is not None), None)
    if "allOf" in s:
        salida = {}
        for parte in s["allOf"]:
            valor = ejemplo(parte, spec, profundidad + 1)
            if isinstance(valor, dict):
                salida.update(valor)
        return salida
    t = s.get("type")
    if t == "array":
        return [ejemplo(s.get("items", {}), spec, profundidad + 1)]
    if t == "object" or "properties" in s:
        return {n: ejemplo(p, spec, profundidad + 1) for n, p in (s.get("properties") or {}).items()}
    if t == "integer":
        return s.get("minimum", 1) or 1
    if t == "number":
        return s.get("minimum", 1.0) or 1.0
    if t == "boolean":
        return True
    formato = s.get("format")
    if formato == "uuid":
        return "00000000-0000-4000-8000-000000000000"
    if formato == "date-time":
        return "2026-11-07T13:00:00Z"
    if formato == "date":
        return "2026-11-07"
    if formato == "email":
        return "persona@unl.edu.ec"
    if formato == "uri":
        return "https://ejemplo.unl.edu.ec"
    if formato == "binary":
        return "<archivo>"
    patron = s.get("pattern") or ""
    if patron == "^[0-9]{10}$":
        return "1100000000"
    return "texto"


def ejemplo_media(media: dict, spec: dict):
    if "example" in media:
        return media["example"]
    if media.get("examples"):
        primero = next(iter(media["examples"].values()))
        if isinstance(primero, dict) and "value" in primero:
            return primero["value"]
        return primero
    return ejemplo(media.get("schema", {}), spec)


def json_bloque(valor) -> str:
    return "```json\n" + json.dumps(valor, ensure_ascii=False, indent=2) + "\n```"


def escapar(texto: str) -> str:
    return (texto or "").replace("|", "\\|").replace("\n", " ").strip()


def roles_operacion(metodo: str, ruta: str) -> tuple[list[str], str | None]:
    """Misma clasificación por rol que usa la web (`tools/contracts/docs.mjs`)."""
    if ruta in ("/auth/login", "/auth/cas", "/auth/sso-config"):
        return ["pub"], None
    if ruta in ("/auth/logout", "/auth/renew", "/me"):
        return ["cta"], None
    if ruta.startswith("/admin/"):
        return ["adm"], None
    if ruta in ("/guest-registrations", "/guest-registrations/submit") or ruta.startswith("/events/open/"):
        return ["inv"], None
    if ruta in ("/attendance/attempts", "/me/active-sessions", "/me/attendances", "/me/stats") or ruta.startswith("/me/absences/"):
        return ["est"], None
    if ruta == "/me/parallels":
        return RESP, None
    if ruta == "/spaces/{spaceId}/geo-evidence":
        return (["adm"] if metodo == "POST" else ["doc", "enc", "adm"]), (
            "La evidencia es inmutable; una corrección crea una versión nueva." if metodo == "POST" else None
        )
    if ruta.startswith("/spaces"):
        return ["doc", "enc", "adm"], (
            "Solo el creador del espacio o un administrador puede editarlo." if metodo == "PATCH" else None
        )
    if ruta.startswith("/stats/") or re.search(r"/(stats|absences|audit)$", ruta):
        return READ, "Directores: solo lectura dentro de su ámbito."
    if metodo == "GET" and (ruta in ("/activities", "/activities/{activityId}") or ruta.endswith("/attendances")):
        return READ, "Directores: solo lectura dentro de su ámbito."
    if metodo == "GET" and re.search(r"/(guest-list|guest-settings|schedule|geolocation)$", ruta):
        return READ, "Directores: solo lectura dentro de su ámbito."
    if ruta.endswith("/schedule") or "/students" in ruta:
        return ["doc"], "Solo clases; el responsable es el docente que la creó."
    if ruta == "/activities" and metodo == "POST":
        return RESP, "CLASE: solo DOCENTE. EVENTO: DOCENTE o ENCARGADO."
    return RESP, "Solo el responsable de la actividad."


def valor_parametro(param: dict, spec: dict):
    s = resolver(param.get("schema", {}), spec) or {}
    if "example" in param:
        return param["example"]
    if "example" in s:
        return s["example"]
    if s.get("default") is not None:
        return s["default"]
    if s.get("enum"):
        return s["enum"][0]
    return ejemplo(s, spec)


def curl(metodo: str, ruta: str, op: dict, parametros: list[dict], spec: dict, cuerpo: dict | None) -> str:
    url = "https://api.example.com/api/v1" + ruta
    consulta = []
    for param in parametros:
        if param.get("in") == "path":
            url = url.replace("{" + param["name"] + "}", quote(str(valor_parametro(param, spec))))
        elif param.get("in") == "query":
            consulta.append(f"{quote(param['name'])}={quote(str(valor_parametro(param, spec)))}")
    if consulta:
        url += "?" + "&".join(consulta)

    lineas = [f"curl -X {metodo} '{url}'"]
    seguridad = op.get("security", spec.get("security", []))
    if seguridad:
        lineas.append("  -H 'Authorization: Bearer <token>'")
    for param in parametros:
        if param.get("in") == "header" and param.get("name", "").lower() == "idempotency-key":
            lineas.append("  -H 'Idempotency-Key: 00000000-0000-4000-8000-000000000000'")

    if cuerpo:
        tipo_contenido, media = cuerpo
        if tipo_contenido == "multipart/form-data":
            lineas.append("  -F 'file=@estudiantes.csv'")
        else:
            lineas.append("  -H 'Content-Type: application/json'")
            lineas.append("  --data '" + json.dumps(ejemplo_media(media, spec), ensure_ascii=False) + "'")
    return "```bash\n" + " \\\n".join(lineas) + "\n```"


def tabla_parametros(parametros: list[dict], spec: dict) -> str:
    if not parametros:
        return ""
    filas = [
        "| Nombre | Ubicación | Tipo | Obligatorio | Descripción |",
        "|---|---|---|---|---|",
    ]
    for param in parametros:
        p = resolver(param, spec) or {}
        desc = escapar(p.get("description"))
        reglas_txt = reglas(p.get("schema", {}), spec)
        detalle = desc + (f" ({reglas_txt})" if reglas_txt else "")
        filas.append(
            f"| `{p.get('name', '')}` | {p.get('in', '')} | {escapar(tipo(p.get('schema', {}), spec))} | "
            f"{'sí' if p.get('required') else 'no'} | {detalle} |"
        )
    return "\n".join(filas)


def tabla_campos(filas: list[dict]) -> str:
    if not filas:
        return ""
    salida = [
        "| Campo | Tipo | Obligatorio | Descripción / reglas |",
        "|---|---|---|---|",
    ]
    for fila in filas:
        detalle = escapar(fila["desc"])
        if fila["rules"]:
            detalle += (" — " if detalle else "") + escapar(fila["rules"])
        salida.append(
            f"| `{fila['name']}` | {escapar(fila['type'])} | {'sí' if fila['required'] else 'no'} | {detalle} |"
        )
    return "\n".join(salida)


def render_operacion(metodo: str, ruta: str, op: dict, spec: dict) -> str:
    operation_id = op.get("operationId") or f"{metodo.lower()}-{ruta}"
    ancla = re.sub(r"[^a-zA-Z0-9_-]+", "-", operation_id).strip("-").lower()
    parametros = [resolver(p, spec) for p in (op.get("parameters") or [])]
    roles, nota = roles_operacion(metodo, ruta)
    etiquetas_roles = ", ".join(ROLES[r] for r in roles)

    salida = [f"### `{metodo}` `{ruta}` {{#op-{ancla}}}", ""]
    if op.get("summary"):
        salida += [f"**{op['summary']}**", ""]
    if op.get("description"):
        salida += [op["description"].strip(), ""]
    salida += [f"**Roles:** {etiquetas_roles}."]
    if nota:
        salida += [f"**Nota de ámbito:** {nota}"]
    seguridad = op.get("security", spec.get("security", []))
    salida += [f"**Seguridad:** {'sesión `SessionBearer`' if seguridad else 'pública (sin autenticación)'}.", ""]

    if parametros:
        salida += ["#### Parámetros", "", tabla_parametros(parametros, spec), ""]

    cuerpo = None
    if op.get("requestBody"):
        cuerpo_op = resolver(op["requestBody"], spec) or {}
        contenidos = cuerpo_op.get("content") or {}
        if contenidos:
            tipo_contenido, media = next(iter(contenidos.items()))
            cuerpo = (tipo_contenido, media)
            salida += ["#### Cuerpo de la solicitud", ""]
            if cuerpo_op.get("description"):
                salida += [cuerpo_op["description"].strip(), ""]
            salida += [f"Tipo de contenido: `{tipo_contenido}`.", ""]
            filas = campos(media.get("schema", {}), spec)
            if filas:
                salida += [tabla_campos(filas), ""]
            salida += ["**Ejemplo de solicitud**", "", json_bloque(ejemplo_media(media, spec)), ""]

    respuestas = op.get("responses") or {}
    if respuestas:
        salida += ["#### Respuestas", "", "| Código | Descripción | Esquema |", "|---|---|---|"]
        for codigo, respuesta in respuestas.items():
            r = resolver(respuesta, spec) or {}
            media = None
            if r.get("content"):
                media = next(iter(r["content"].values()))
            esquema = nombre_ref(media.get("schema", {})) if media else None
            if not esquema and media:
                esquema = tipo(media.get("schema", {}), spec)
            salida.append(f"| `{codigo}` | {escapar(r.get('description', ''))} | {escapar(esquema or '—')} |")
        salida.append("")

        for codigo, respuesta in respuestas.items():
            if not str(codigo).startswith("2"):
                continue
            r = resolver(respuesta, spec) or {}
            if not r.get("content"):
                continue
            tipo_contenido, media = next(iter(r["content"].items()))
            if tipo_contenido.startswith("application/json"):
                salida += [f"**Ejemplo de respuesta `{codigo}`**", "", json_bloque(ejemplo_media(media, spec)), ""]

    salida += ["#### Ejemplo con `curl`", "", curl(metodo, ruta, op, parametros, spec, cuerpo), ""]
    return "\n".join(salida)


def render_tag(tag: str, etiqueta: str, operaciones: list[tuple[str, str, dict]], spec: dict) -> str:
    lineas = [
        "---",
        f'title: "{etiqueta}"',
        "---",
        "",
        f"# {etiqueta}",
        "",
        f"Operaciones del grupo `{tag}` del contrato OpenAPI. "
        f"Total: **{len(operaciones)}**.",
        "",
    ]
    for metodo, ruta, op in operaciones:
        lineas += [render_operacion(metodo, ruta, op, spec), ""]
    return "\n".join(lineas)


def render_esquemas(spec: dict) -> str:
    esquemas = spec.get("components", {}).get("schemas", {})
    lineas = [
        "---",
        'title: "Esquemas"',
        "---",
        "",
        "# Esquemas del contrato",
        "",
        f"Modelos reutilizables del contrato OpenAPI (**{len(esquemas)}**). "
        "Cada tabla muestra los campos, su tipo, si son obligatorios y sus reglas de validación.",
        "",
    ]
    for nombre in sorted(esquemas):
        esquema = esquemas[nombre]
        lineas += [f"## `{nombre}`", ""]
        if esquema.get("description"):
            lineas += [esquema["description"].strip(), ""]
        filas = campos(esquema, spec)
        if filas:
            lineas += [tabla_campos(filas), ""]
        lineas += ["**Ejemplo**", "", json_bloque(ejemplo(esquema, spec)), ""]
    return "\n".join(lineas)


def main() -> None:
    spec = cargar_contrato()
    REFERENCIA.mkdir(parents=True, exist_ok=True)

    operaciones: dict[str, list[tuple[str, str, dict]]] = {tag: [] for tag, _ in TAGS}
    desconocidas: list[tuple[str, str, dict]] = []
    for ruta, item in spec.get("paths", {}).items():
        for metodo in METODOS:
            op = item.get(metodo)
            if not op:
                continue
            tag = (op.get("tags") or ["otros"])[0]
            destino = operaciones.setdefault(tag, [])
            destino.append((metodo.upper(), ruta, op))
            if tag not in dict(TAGS):
                desconocidas.append((metodo.upper(), ruta, op))

    for tag, etiqueta in TAGS:
        ops = operaciones.get(tag, [])
        (REFERENCIA / f"{tag}.qmd").write_text(render_tag(tag, etiqueta, ops, spec), encoding="utf-8")

    if desconocidas:
        (REFERENCIA / "otros.qmd").write_text(render_tag("otros", "Otras operaciones", desconocidas, spec), encoding="utf-8")

    indice = [
        "---",
        'title: "Referencia por funcionalidad"',
        "---",
        "",
        "# Referencia por funcionalidad",
        "",
        "Cada página documenta las operaciones de un grupo: qué hacen, qué roles las usan, "
        "sus parámetros, el cuerpo de la solicitud, las respuestas y ejemplos listos para ejecutar.",
        "",
        "| Funcionalidad | Operaciones |",
        "|---|---:|",
    ]
    for tag, etiqueta in TAGS:
        indice.append(f"| [{etiqueta}]({tag}.qmd) | {len(operaciones.get(tag, []))} |")
    if desconocidas:
        indice.append(f"| [Otras operaciones](otros.qmd) | {len(desconocidas)} |")
    total = sum(len(v) for v in operaciones.values())
    indice += ["", f"**Total: {total} operaciones.**", ""]
    (REFERENCIA / "index.qmd").write_text("\n".join(indice), encoding="utf-8")

    (AQUI / "esquemas.qmd").write_text(render_esquemas(spec), encoding="utf-8")
    shutil.copyfile(CONTRATO, AQUI / "openapi.yaml")
    print(f"Referencia generada: {total} operaciones en {len(TAGS)} funcionalidades.")


if __name__ == "__main__":
    main()
