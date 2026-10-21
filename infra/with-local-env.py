"""Ejecuta un comando con .env local; no interpreta shell ni imprime secretos."""
from pathlib import Path
import os
import re
import sys
p=Path(__file__).resolve().parents[1]/".env"
environment=dict(os.environ)
for raw in p.read_text().splitlines():
    line=raw.strip()
    if not line or line.startswith("#"):
        continue
    key,sep,value=line.partition("=")
    if not sep or not re.fullmatch(r"[A-Z][A-Z0-9_]*",key):
        raise SystemExit("Formato .env inválido")
    environment[key]=value
arguments=sys.argv[1:]
if arguments and arguments[0]=="--":arguments=arguments[1:]
if not arguments:raise SystemExit("Indica un comando")
os.execvpe(arguments[0],arguments,environment)
