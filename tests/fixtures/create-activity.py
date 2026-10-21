"""Solo pruebas locales: crea actividad y sesión ficticias sin endpoints públicos adicionales."""
import json
import subprocess
import sys
import uuid
mode = sys.argv[1] if len(sys.argv) > 1 else "EVENTO"
if mode not in {"CLASE", "EVENTO"}:
    raise SystemExit("Modalidad inválida")
activity, session = str(uuid.uuid4()), str(uuid.uuid4())
owner = "00000000-0000-0000-0000-000000000201" if mode == "EVENTO" else "00000000-0000-0000-0000-000000000200"
name = "Prueba web " + activity[:8]
sql = f"""BEGIN;
INSERT INTO actividades(id,nombre,modalidad,responsable_id) VALUES ('{activity}','{name}','{mode}','{owner}');
INSERT INTO actividad_carreras VALUES ('{activity}','00000000-0000-0000-0000-000000000001');
INSERT INTO sesiones(id,actividad_id,ventana_inicio,ventana_fin) VALUES ('{session}','{activity}',CURRENT_TIMESTAMP-INTERVAL '1 hour',CURRENT_TIMESTAMP+INTERVAL '1 day');
{"INSERT INTO estudiantes_autorizados VALUES ('" + activity + "','00000000-0000-0000-0000-000000000102',true);" if mode == "CLASE" else ""}
COMMIT;"""
subprocess.run(["docker", "exec", "-i", "geoasist-db-1", "psql", "-v", "ON_ERROR_STOP=1", "-U", "geoasist", "-d", "geoasist"], input=sql, text=True, check=True, stdout=subprocess.DEVNULL)
print(json.dumps({"activity": activity, "session": session, "name": name}))
