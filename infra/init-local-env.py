"""Crea secretos solo en .env local ignorado por Git, sin imprimirlos."""
from pathlib import Path
import secrets
p = Path(__file__).resolve().parents[1] / ".env"
if p.exists():
    raise SystemExit(".env ya existe; no se reemplazó.")
p.write_text("POSTGRES_USER=geoasist\nPOSTGRES_DB=geoasist\nPOSTGRES_PASSWORD=" + secrets.token_urlsafe(24) + "\nDEV_SEED_PASSWORD=" + secrets.token_urlsafe(24) + "\nGEOASIST_HMAC_KEY=" + secrets.token_urlsafe(48) + "\n")
p.chmod(0o600)
print(".env creado con secretos aleatorios; no compartir ni versionar.")
