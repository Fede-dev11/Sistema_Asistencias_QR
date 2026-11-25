"""Cincuenta cuentas ficticias y cinco sesiones para la hipótesis de laboratorio."""
import json
import subprocess
import uuid

run = uuid.uuid4().hex
owner = '00000000-0000-0000-0000-000000000201'
sessions = []
accounts = []
statements = ['BEGIN;']
for index in range(50):
    person, account = str(uuid.uuid4()), str(uuid.uuid4())
    # Espacio ficticio aleatorio de diez dígitos; conservar ceros como texto.
    cedula = str(1000000000 + (uuid.uuid4().int % 8999999999))
    email = f'perf-{run}-{index}@example.test'
    statements += [
        f"INSERT INTO personas VALUES ('{person}','{cedula}','Persona ficticia rendimiento {index}','INSTITUCIONAL');",
        f"INSERT INTO cuentas SELECT '{account}','{person}','DEV','{email}',password_hash,true FROM cuentas WHERE id='00000000-0000-0000-0000-000000000202';",
        f"INSERT INTO roles_cuenta VALUES ('{account}','ESTUDIANTE');",
    ]
    accounts.append(email)
for index in range(5):
    activity, session = str(uuid.uuid4()), str(uuid.uuid4())
    statements += [
        f"INSERT INTO actividades(id,nombre,modalidad,responsable_id,acceso) VALUES ('{activity}','Laboratorio ficticio {run} {index}','EVENTO','{owner}','CERRADO');",
        f"INSERT INTO actividad_carreras VALUES ('{activity}','00000000-0000-0000-0000-000000000001');",
        f"INSERT INTO sesiones(id,actividad_id,ventana_inicio,ventana_fin) VALUES ('{session}','{activity}',CURRENT_TIMESTAMP-INTERVAL '1 hour',CURRENT_TIMESTAMP+INTERVAL '1 day');",
    ]
    sessions.append(session)
statements.append('COMMIT;')
subprocess.run(['docker','exec','-i','geoasist-db-1','psql','-v','ON_ERROR_STOP=1','-U','geoasist','-d','geoasist'], input='\n'.join(statements),text=True,check=True,stdout=subprocess.DEVNULL)
print(json.dumps({'sessions': sessions, 'emails': accounts}))
