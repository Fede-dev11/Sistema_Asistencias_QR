"""Ensayo local reproducible: contenedores y archivos exclusivamente ficticios."""
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
import uuid

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("backup_tool", ROOT / "infra/backup/backup_tool.py")
tool = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tool)


def main():
    container = "geoasist-retention-test-" + uuid.uuid4().hex
    directory = Path(tempfile.mkdtemp(prefix="geoasist-retention-"))
    os.environ.update(PG_CONTAINER=container, PGUSER="postgres", PGDATABASE="postgres",
                      GEOASIST_BACKUP_ROOT=str(directory), POSTGRES_PASSWORD=uuid.uuid4().hex)
    subprocess.run(["docker", "run", "-d", "--name", container, "--network", "none",
                    "--tmpfs", "/var/lib/postgresql/data:rw", "-e", "PGDATA=/var/lib/postgresql/data", "-e", "POSTGRES_PASSWORD", "postgres:17.7"],
                   check=True, stdout=subprocess.DEVNULL)
    try:
        tool.wait_for_postgres(container)
        for migration in sorted((ROOT / "services/api/src/main/resources/db/migration").glob("*.sql")):
            try:
                tool.query(migration.read_text())
            except RuntimeError as error:
                raise RuntimeError("No se pudo aplicar la migración " + migration.name) from error
        person, account, expired, current = [str(uuid.uuid4()) for _ in range(4)]
        tool.query(f"""
        INSERT INTO personas VALUES ('{person}','0012345678','Cuenta ficticia vigente','INSTITUCIONAL');
        INSERT INTO cuentas VALUES ('{account}','{person}','DEV','restore@example.test',NULL,true);
        INSERT INTO actividades(id,nombre,modalidad,responsable_id,estado,cerrada_en,anonimizar_en)
          VALUES ('{expired}','MARCADOR_NOMINAL_VENCIDO','EVENTO','{account}','CERRADA',
                  CURRENT_TIMESTAMP-INTERVAL '5 years'+INTERVAL '2 seconds',CURRENT_TIMESTAMP+INTERVAL '2 seconds');
        INSERT INTO actividades(id,nombre,modalidad,responsable_id)
          VALUES ('{current}','Evento ficticio vigente','EVENTO','{account}');
        """)
        original = directory / (str(uuid.uuid4()) + ".dump")
        with original.open("wb") as output:
            original.chmod(0o600)
            subprocess.run(tool.command("pg_dump") + ["-Fc", "--no-owner", "--no-acl"], stdout=output, check=True)
        tool.register(original, "BACKUP")
        # Segmento WAL real del contenedor ficticio, catalogado como copia recuperable.
        wal_name = tool.query("SELECT name FROM pg_ls_waldir() ORDER BY name LIMIT 1;")
        wal_file = directory / (str(uuid.uuid4()) + ".wal")
        data_directory = tool.query("SHOW data_directory;")
        with wal_file.open("wb") as output:
            subprocess.run(["docker", "exec", container, "cat", data_directory + "/pg_wal/" + wal_name], check=True, stdout=output)
        wal_file.chmod(0o600)
        tool.register(wal_file, "WAL")
        export_file = directory / (str(uuid.uuid4()) + ".csv")
        export_file.write_text("nombres\nInvitada ficticia\n")
        export_file.chmod(0o600)
        tool.register(export_file, "EXPORT")
        time.sleep(2.1)
        tool.restore(original)
        candidates = [path for path in directory.glob("*.dump") if path != original]
        assert len(candidates) == 1
        restored = subprocess.run(["docker", "exec", "-i", container, "pg_restore", "--data-only", "--file=-"],
                                  input=candidates[0].read_bytes(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
        assert b"MARCADOR_NOMINAL_VENCIDO" not in restored.stdout
        assert expired.encode() not in restored.stdout
        assert b"Cuenta ficticia vigente" in restored.stdout
        tool.sanitize()
        assert tool.query("SELECT count(*) FROM actividades WHERE id='" + expired + "';") == "0"
        assert tool.query("SELECT count(*) FROM actividades WHERE id='" + current + "';") == "1"
        assert tool.query("SELECT count(*) FROM tratamientos_conservacion WHERE fase='COMPLETADO';") == "0"
        tool.backup()
        # Fallo comprobado: enlace fuera del directorio; no cambia a RETIRADA ni COMPLETADO.
        link = directory / "external-link"
        link.symlink_to("/etc/hosts")
        try:
            tool.managed_file(link)
            raise AssertionError("Se aceptó enlace externo")
        except ValueError:
            pass
        tool.query("SELECT completar_tratamientos();")
        assert tool.query("SELECT count(*) FROM tratamientos_conservacion WHERE fase='COMPLETADO';") == "0"
        tool.purge("ensayo-ficticio-sin-snapshots-externos")
        tool.query("UPDATE politica_copias SET inventario_verificado_en=CURRENT_TIMESTAMP,evidencia_retiro='ensayo-aislado'; SELECT completar_tratamientos();")
        assert tool.query("SELECT count(*) FROM tratamientos_conservacion WHERE fase='COMPLETADO';") == "1"
        assert tool.query("SELECT count(*) FROM catalogo_copias WHERE estado='RETIRADA';") == "3"
        print("PASS: restore previo al vencimiento saneado sin red; cuenta/actividad vigente preservadas; BACKUP/WAL/EXPORT retirados; fallos mantienen pendiente.")
    finally:
        subprocess.run(["docker", "rm", "-fv", container], check=True, stdout=subprocess.DEVNULL)
        shutil.rmtree(directory)


if __name__ == "__main__":
    main()
