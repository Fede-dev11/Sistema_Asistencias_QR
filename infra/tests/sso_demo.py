#!/usr/bin/env python3
"""Demostración de SSO: con una sesión CASTGC se obtiene un ticket para un segundo servicio
('ejemplo', http://localhost:8082/) sin pedir credenciales, y se valida.

Uso:
  python infra/tests/sso_demo.py --tgc <valor de la cookie CASTGC>
  SSO_DEMO_PASSWORD=... python infra/tests/sso_demo.py --correo docente@example.test
(con --correo se hace primero un ingreso para obtener CASTGC). Solo biblioteca estándar."""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


OPENER = urllib.request.build_opener(NoRedirect)


def request(url, headers=None, data=None):
    req = urllib.request.Request(url, headers=headers or {}, data=data)
    try:
        return OPENER.open(req, timeout=10)
    except urllib.error.HTTPError as e:  # 3xx/4xx llegan como excepción
        return e


def cookies(response):
    found = {}
    for header in response.headers.get_all("Set-Cookie") or []:
        name, _, rest = header.partition("=")
        found[name] = rest.split(";", 1)[0]
    return found


def login(base, correo, password, service):
    first = request(f"{base}/cas/login?service={urllib.parse.quote(service, safe='')}")
    cookie = cookies(first).get("XSRF-TOKEN")
    # Spring enmascara el token en el formulario; la cookie lleva el valor sin enmascarar.
    found = re.search(r'name="_csrf" value="([^"]+)"', first.read().decode())
    if not (cookie and found):
        sys.exit("No se obtuvo el token CSRF del servicio.")
    csrf = found.group(1)
    body = urllib.parse.urlencode(
        {"correo": correo, "password": password, "service": service, "_csrf": csrf}
    ).encode()
    res = request(
        f"{base}/cas/login",
        {"Cookie": f"XSRF-TOKEN={cookie}", "Content-Type": "application/x-www-form-urlencoded"},
        body,
    )
    tgc = cookies(res).get("CASTGC")
    if not tgc:
        sys.exit(f"Ingreso rechazado (HTTP {res.status}).")
    return tgc


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    ap.add_argument("--base", default=os.environ.get("SSO_BASE", "http://localhost:8081"))
    ap.add_argument("--service", default="http://localhost:8082/")
    ap.add_argument("--tgc", default=os.environ.get("SSO_DEMO_TGC"))
    ap.add_argument("--correo")
    args = ap.parse_args()

    tgc = args.tgc
    if not tgc:
        password = os.environ.get("SSO_DEMO_PASSWORD")
        if not (args.correo and password):
            sys.exit("Indica --tgc, o --correo con SSO_DEMO_PASSWORD.")
        tgc = login(args.base, args.correo, password, os.environ.get("SSO_DEMO_LOGIN_SERVICE", "http://localhost:3000/auth/callback"))

    # Sin credenciales: solo la cookie CASTGC.
    res = request(
        f"{args.base}/cas/login?service={urllib.parse.quote(args.service, safe='')}",
        {"Cookie": f"CASTGC={tgc}"},
    )
    location = res.headers.get("Location", "")
    if res.status != 302 or "ticket=" not in location:
        sys.exit(f"No hubo SSO (HTTP {res.status}); la sesión no es válida o expiró.")
    ticket = urllib.parse.parse_qs(urllib.parse.urlparse(location).query)["ticket"][0]

    query = urllib.parse.urlencode({"service": args.service, "ticket": ticket, "format": "JSON"})
    with request(f"{args.base}/cas/p3/serviceValidate?{query}") as ok:
        data = json.load(ok)["serviceResponse"]
    if "authenticationSuccess" not in data:
        sys.exit(f"Validación fallida: {data['authenticationFailure']['code']}")
    success = data["authenticationSuccess"]
    print(f"Usuario: {success['user']}")
    print(f"Roles: {', '.join(success['attributes'].get('roles', []))}")
    print(f"SSO correcto hacia {args.service} sin pedir credenciales.")


if __name__ == "__main__":
    main()
