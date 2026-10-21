"""Comprueba el JAR por HTTP con una cuenta DEV; no imprime credenciales ni cuerpos."""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from zoneinfo import ZoneInfo


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default="http://127.0.0.1:8080/api/v1")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    password = os.environ.get("DEV_SEED_PASSWORD")
    if not password:
        raise SystemExit("Ejecutar con infra/with-local-env.py y una contraseña DEV configurada.")
    cases = []
    token = None
    revoked = False

    def request(method, path, body=None, bearer=None):
        headers = {"Content-Type": "application/json"}
        if bearer:
            headers["Authorization"] = "Bearer " + bearer
        data = json.dumps(body).encode() if body is not None else None
        started = time.perf_counter()
        try:
            with urlopen(Request(args.url + path, data, headers, method=method), timeout=15) as response:
                status, raw = response.status, response.read()
        except HTTPError as error:
            status, raw = error.code, error.read()
        return status, json.loads(raw) if raw else None, round((time.perf_counter() - started) * 1000, 2)

    def check(name, expected, response):
        status, body, elapsed = response
        cases.append({"caso": name, "esperado": expected, "http": status,
                      "duracion_ms": elapsed, "aprobado": status == expected})
        if status != expected:
            raise RuntimeError("No se cumplió el estado HTTP esperado.")
        return body

    try:
        for attempt in range(60):
            try:
                response = request("GET", "/activities")
                break
            except (URLError, TimeoutError):
                if attempt == 59:
                    raise RuntimeError("API no disponible en el plazo de arranque.") from None
                time.sleep(1)
        check("Actividades sin credencial", 401, response)
        check("Login con contraseña incorrecta", 401, request("POST", "/auth/login", {
            "correo": "docente@example.test", "password": os.urandom(24).hex()}))
        login = check("Login DEV válido", 200, request("POST", "/auth/login", {
            "correo": "docente@example.test", "password": password}))
        token = login["access_token"]
        listing = check("Listado autenticado de actividades", 200,
                        request("GET", "/activities", bearer=token))
        if not listing.get("items"):
            raise RuntimeError("La semilla DEV no contiene actividades visibles para el recorrido.")
        activity = listing["items"][0]["id"]
        check("Detalle de actividad autorizada", 200,
              request("GET", "/activities/" + activity, bearer=token))
        check("Logout", 204, request("POST", "/auth/logout", bearer=token))
        revoked = True
        check("Credencial revocada rechazada", 401, request("GET", "/activities", bearer=token))
    except Exception:
        raise SystemExit("Falló la comprobación HTTP. Revisar los estados redactados del resultado.") from None
    finally:
        if token and not revoked:
            try:
                request("POST", "/auth/logout", bearer=token)
            except Exception:
                pass
        result = {"fecha": datetime.now(ZoneInfo("America/Guayaquil")).isoformat(),
                  "url": args.url, "casos": cases,
                  "aprobado": len(cases) == 7 and all(case["aprobado"] for case in cases)}
        Path(args.output).write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print("HTTP: 7/7 comprobaciones aprobadas; credencial de prueba revocada.")


if __name__ == "__main__":
    main()
