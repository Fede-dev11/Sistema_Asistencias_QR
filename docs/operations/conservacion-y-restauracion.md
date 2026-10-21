# Conservación y restauración

La fecha nominal vence cinco años calendario UTC después del cierre definitivo. La API bloquea
lectura y exportación con 410 desde ese instante aunque falle el job. Después de retirar los
encabezados responde 404. El cierre de una sesión no inicia este plazo.

## Proceso de base de datos

`AnonymizeExpiredActivity` ejecuta cada hora `anonimizar_vencidas(hora_servidor,100)`. El mismo
procedimiento versionado se utiliza al restaurar. Bloquea actividad, sesiones y personas en
orden, genera solo agregados admisibles y elimina revisiones, asistencias, intentos, listados,
auditoría, idempotencias, QR, inicios, encabezados y relaciones. Conserva cuentas globales y datos
de otra actividad vigente. Elimina invitados y espacios huérfanos únicamente sin otro propósito.
Una excepción revierte el lote. El acceso vencido continúa bloqueado y se emite una alerta sin IDs.

Los agregados son anuales por modalidad y versión; requieren diez personas distintas y dos
actividades. No hay totales complementarios, carreras, paralelos, horarios, hashes o enlaces
nominales. Solo se sella un año completamente vencido; un lote posterior no actualiza una celda
publicada, para evitar diferencias entre versiones. Grupos o lotes insuficientes se suprimen.
Esta supresión conservadora puede descartar estadísticas válidas; requiere evaluación institucional.
No hay un endpoint público de estadísticas ni se afirma que el umbral garantice anonimato.

## Copias y responsables

El operador de base de datos debe inventariar **todos** los backups, WAL, exports, snapshots,
réplicas y soportes externos recuperables. El responsable que descarga CSV debe cumplir la misma
conservación y documentar su retiro. La aplicación no conserva los CSV generados en disco;
si un operador conserva uno, debe registrarlo como `EXPORT`. El catálogo y la política solo son
accesibles mediante el acceso operativo de PostgreSQL, sin rutas HTTP.

El retiro nominal deja el tratamiento en `COPIAS_PENDIENTES`. Únicamente puede pasar a
`COMPLETADO` cuando existe inventario posterior al tratamiento, respaldo saneado posterior,
evidencia de retiro y ninguna copia recuperable sin sanear. Al completar se retiran FK,
fecha nominal y fecha de tratamiento; queda UUID aleatorio, versión y estado. Este control
no se publica ni se relaciona con celdas estadísticas.

## Herramientas

Requiere Python 3, PostgreSQL cliente compatible y Docker para restauración. Usar credenciales
PG* mediante entorno o `.pgpass`, nunca URL con contraseña en comandos. Para el contenedor local:

```bash
export PG_CONTAINER=geoasist-db-1 PGUSER=geoasist PGDATABASE=geoasist
export GEOASIST_BACKUP_ROOT=/ruta/operativa/privada/copias
mkdir -m 700 -p "$GEOASIST_BACKUP_ROOT"
# Registrar cada archivo ya conservado antes de retirarlo; solo archivos dentro de esta raíz.
python3 infra/backup/backup_tool.py register --file "$GEOASIST_BACKUP_ROOT/archivo.dump" --support BACKUP
python3 infra/backup/backup_tool.py register --file "$GEOASIST_BACKUP_ROOT/archivo.wal" --support WAL
bash infra/backup/sanitized-backup.sh
bash infra/backup/restore-isolated.sh --file "$GEOASIST_BACKUP_ROOT/archivo.dump"
```

La restauración crea un PostgreSQL 17.7 sin red, con almacenamiento temporal, aplica la política,
comprueba que no queden encabezados vencidos y genera una copia nueva saneada; luego retira el
contenedor. No inicia la API ni habilita el servicio. Una copia anterior a la migración V008
se rechaza: migrarla al esquema vigente en aislamiento antes de repetir el procedimiento.

Después de comprobar snapshots/soportes externos y registrar la referencia operativa real:

```bash
bash infra/backup/purge-expired-copies.sh --evidence REFERENCIA_REAL_DE_RETIRO
python3 infra/backup/backup_tool.py attest --evidence REFERENCIA_REAL_DE_INVENTARIO
```

`purge` elimina solo archivos regulares dentro de la raíz y falla ante enlaces, archivos ausentes
o copias fuera de alcance. Un fallo deja las entradas sin retirar y no certifica el tratamiento.
Si el archivo ya fue retirado fuera de la herramienta, el operador debe verificarlo y registrar
la evidencia en el catálogo mediante el acceso operativo. No usar una referencia ficticia con
datos reales. `DELETE`, `unlink` y un hash de integridad no prueban borrado físico de bloques,
WAL, snapshots ni dispositivos; la certificación del operador debe abarcar esos soportes.

## Ensayo reproducido

```bash
bash infra/tests/retention-restore.sh
```

El ensayo utiliza contenedores sin red y datos ficticios. Verifica copia previa a vencimiento,
restauración saneada, preservación de cuenta/actividad vigente, catálogo BACKUP/WAL/EXPORT,
rechazo de enlaces externos, bloqueo de finalización y retiro de archivos gestionados. Incluye
un segmento WAL real como soporte inventariado; no ejecuta recuperación PITR de una cadena WAL.
La habilitación productiva sigue requiriendo un ensayo de infraestructura institucional,
incluyendo snapshots, réplica y PITR si se utilizan.
