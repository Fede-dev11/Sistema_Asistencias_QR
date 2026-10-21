"""Operaciones de conservación; credenciales solo mediante variables PG*.

PG_CONTAINER permite usar herramientas de la misma versión dentro del contenedor local.
No expone puertos al restaurar y no considera unlink una prueba de borrado físico.
"""
import argparse
import datetime as dt
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import time
import uuid


def sql_string(value):
    return "'" + str(value).replace("'", "''") + "'"


def command(program):
    container = os.environ.get("PG_CONTAINER")
    if container:
        return ["docker", "exec", "-i", container, program,
                "-U", os.environ.get("PGUSER", "geoasist"),
                "-d", os.environ.get("PGDATABASE", "geoasist")]
    return [program]


def query(sql):
    result = subprocess.run(command("psql") + ["-X", "-A", "-t", "-v", "ON_ERROR_STOP=1"],
                            input=sql, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise RuntimeError("Falló la operación SQL; el tratamiento permanece pendiente.")
    return result.stdout.strip()


def wait_for_postgres(container, attempts=60):
    # La imagen oficial inicia primero un servidor temporal que solo escucha por socket.
    # Comprobar TCP dentro del contenedor espera al proceso definitivo sin publicar puertos.
    for _ in range(attempts):
        result = subprocess.run(
            ["docker", "exec", container, "pg_isready", "-h", "127.0.0.1", "-U", "postgres", "-d", "postgres"],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        if result.returncode == 0:
            return
        time.sleep(0.5)
    raise RuntimeError("PostgreSQL aislado no aceptó conexiones TCP dentro del plazo de arranque.")


def timestamp():
    return dt.datetime.now(dt.timezone.utc).isoformat()


def root_path():
    raw = Path(os.environ.get("GEOASIST_BACKUP_ROOT", ""))
    if not raw.is_absolute() or raw.is_symlink():
        raise ValueError("GEOASIST_BACKUP_ROOT debe ser un directorio absoluto sin enlaces simbólicos.")
    raw.mkdir(mode=0o700, parents=True, exist_ok=True)
    if raw.stat().st_mode & 0o077:
        raise ValueError("El directorio de copias debe tener permisos 0700.")
    return raw.resolve()


def managed_file(value):
    path = Path(value)
    if any(character in str(path) for character in "\r\n\t"):
        raise ValueError("La referencia operativa no admite caracteres de control.")
    root = root_path()
    if path.is_symlink() or not path.is_file() or root not in path.resolve().parents:
        raise ValueError("Solo se aceptan archivos regulares dentro del directorio gestionado.")
    return path.resolve()


def sanitize():
    cutoff = timestamp()
    while int(query("SELECT anonimizar_vencidas(" + sql_string(cutoff) + ",100);")):
        pass
    if int(query("SELECT count(*) FROM actividades WHERE anonimizar_en<=" + sql_string(cutoff) + ";")):
        raise RuntimeError("Quedan datos nominales vencidos. No se genera una copia habilitable.")
    return cutoff


def digest(path):
    hasher = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            hasher.update(block)
    return hasher.hexdigest()


def register(path, support, sanitized=None):
    query("INSERT INTO catalogo_copias(id,soporte,referencia,creada_en,saneada_hasta) VALUES ("
          + ",".join([sql_string(uuid.uuid4()), sql_string(support), sql_string(path),
                      sql_string(timestamp()), sql_string(sanitized) if sanitized else "NULL"])
          + ") ON CONFLICT(referencia) DO NOTHING;")


def backup():
    cutoff = sanitize()
    path = root_path() / (str(uuid.uuid4()) + ".dump")
    # Registrar antes de escribir: un archivo parcial sigue siendo recuperable y requiere retiro.
    register(path, "BACKUP")
    with path.open("xb") as output:
        path.chmod(0o600)
        result = subprocess.run(command("pg_dump") + ["-Fc", "--no-owner", "--no-acl"],
                                stdout=output, stderr=subprocess.PIPE)
    if result.returncode:
        raise RuntimeError("Falló el respaldo; la copia parcial permanece inventariada.")
    checksum = digest(path)
    query("UPDATE catalogo_copias SET saneada_hasta=" + sql_string(cutoff)
          + ",verificada_en=CURRENT_TIMESTAMP,evidencia=" + sql_string("sha256:" + checksum)
          + " WHERE referencia=" + sql_string(path) + ";"
          + " UPDATE politica_copias SET respaldo_saneado_en=" + sql_string(cutoff) + " WHERE id=1;")
    print(path)


def purge(evidence):
    # El operador afirma por separado el tratamiento de snapshots, WAL y copias externas.
    root_path()
    records = query("SELECT id::text||E'\\t'||referencia FROM catalogo_copias "
                    "WHERE estado='RECUPERABLE' AND (saneada_hasta IS NULL OR saneada_hasta<"
                    "(SELECT max(procesado_en) FROM tratamientos_conservacion WHERE fase='COPIAS_PENDIENTES'));")
    for line in records.splitlines():
        identity, reference = line.split("\t", 1)
        path = managed_file(reference)  # Fallar ante copia fuera de alcance o ausente, no fingir éxito.
        checksum = digest(path)
        path.unlink()
        if path.exists():
            raise RuntimeError("No se pudo verificar el retiro del archivo.")
        query("UPDATE catalogo_copias SET estado='RETIRADA',verificada_en=CURRENT_TIMESTAMP,evidencia="
              + sql_string(evidence + ":sha256:" + checksum) + " WHERE id=" + sql_string(identity) + ";")
    print("Retiro de archivos gestionados verificado. El operador debe verificar las demás copias.")


def restore(source):
    source = managed_file(source)
    # El origen y sus WAL siguen siendo copias recuperables en el inventario de origen.
    register(source, "BACKUP")
    container = "geoasist-restore-" + uuid.uuid4().hex
    environment = dict(os.environ)
    environment["POSTGRES_PASSWORD"] = uuid.uuid4().hex
    subprocess.run(["docker", "run", "-d", "--name", container, "--network", "none",
                    "--tmpfs", "/var/lib/postgresql/data:rw", "-e", "PGDATA=/var/lib/postgresql/data", "-e", "POSTGRES_PASSWORD",
                    "postgres:17.7"], env=environment, stdout=subprocess.DEVNULL, check=True)
    previous_container = os.environ.get("PG_CONTAINER")
    previous_user = os.environ.get("PGUSER")
    previous_db = os.environ.get("PGDATABASE")
    try:
        wait_for_postgres(container)
        with source.open("rb") as data:
            result = subprocess.run(["docker", "exec", "-i", container, "pg_restore", "-U", "postgres",
                                     "-d", "postgres", "--no-owner", "--no-acl", "--exit-on-error"],
                                    stdin=data, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        if result.returncode:
            raise RuntimeError("La restauración falló; el servicio sigue aislado.")
        os.environ.update(PG_CONTAINER=container, PGUSER="postgres", PGDATABASE="postgres")
        # Nunca habilitar una versión restaurada con una política desconocida o antigua.
        if query("SELECT to_regprocedure('anonimizar_vencidas(timestamp with time zone,integer)') IS NOT NULL;") != "t":
            raise RuntimeError("La copia requiere migración a la política vigente antes de restaurar.")
        query("UPDATE politica_copias SET inventario_verificado_en=NULL,respaldo_saneado_en=NULL,evidencia_retiro=NULL;")
        backup()
        print("Restauración saneada en red aislada; contenedor retirado al finalizar. Habilitación requiere revisión operativa.")
    finally:
        for key, value in (("PG_CONTAINER", previous_container), ("PGUSER", previous_user), ("PGDATABASE", previous_db)):
            if value is None:
                os.environ.pop(key, None)
            else:
                os.environ[key] = value
        subprocess.run(["docker", "rm", "-fv", container], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=["backup", "purge", "restore", "register", "attest"])
    parser.add_argument("--file")
    parser.add_argument("--support", choices=["BACKUP", "WAL", "EXPORT"], default="BACKUP")
    parser.add_argument("--evidence", help="Referencia operativa que acredita inventario/retiro en todos los soportes")
    args = parser.parse_args()
    if args.operation == "backup":
        backup()
    elif args.operation == "register":
        register(managed_file(args.file), args.support)
    elif args.operation == "restore":
        restore(args.file)
    elif args.operation == "purge":
        if not args.evidence or not args.evidence.strip():
            parser.error("purge requiere --evidence de retiro en todos los soportes")
        purge(args.evidence)
    elif args.operation == "attest":
        if not args.evidence or not args.evidence.strip():
            parser.error("attest requiere --evidence de inventario físico verificado por el operador")
        query("UPDATE politica_copias SET inventario_verificado_en=CURRENT_TIMESTAMP,evidencia_retiro="
              + sql_string(args.evidence) + " WHERE id=1; SELECT completar_tratamientos();")
        print("Inventario certificado por operador; finalización sujeta a respaldo y copias sin pendientes.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, ValueError, OSError, subprocess.CalledProcessError):
        print("Operación incompleta. Mantener aislamiento/acceso bloqueado y revisar inventario; no se declara borrado físico.", file=sys.stderr)
        sys.exit(1)
