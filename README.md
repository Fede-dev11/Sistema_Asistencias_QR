# Sistema_Asistencias_QR

Sistema de asistencia para clases y eventos: API Spring Boot, web Next.js y app Expo.
Incluye QR dinámico, invitados precargados, revisión responsable, reportes por ámbito y
conservación de cinco años con anonimización y restauración aislada. Usa datos ficticios de
desarrollo; la integración CAS y la validación en dispositivos/institución permanecen pendientes.

### Ejecutar y comprobar el incremento

Requiere Java 21, Node 22.20.0 y Docker. Maven 3.9.11 y las dependencias de cada cliente están
fijados por wrapper y lockfiles. Desde la raíz:

```bash
python3 infra/init-local-env.py
docker compose --env-file .env -f infra/docker-compose.yml up -d --wait db
```

En terminales separadas:

```bash
# API: .env se carga sin imprimir secretos
python3 infra/with-local-env.py -- env SPRING_PROFILES_ACTIVE=dev services/api/mvnw -f services/api/pom.xml spring-boot:run
# Web: dentro de apps/web
npm ci
npm run dev
# Móvil: dentro de apps/mobile
npm ci
npm start
```

La API usa el puerto 8080 y PostgreSQL de desarrollo el 5433. La web usa 3000. Para un
dispositivo configura `EXPO_PUBLIC_API_URL` con una dirección accesible desde él. Cuentas
ficticias: `docente@example.test`, `encargado@example.test`, `estudiante@example.test`,
`director@example.test` y `director-carrera@example.test`; la contraseña proviene de
`DEV_SEED_PASSWORD`, nunca del repositorio.

Administrador de desarrollo: `admin@dev.geoasist.local` (misma contraseña local). Importación CSV
de estudiantes: `POST /api/v1/admin/imports/students`; las cuentas creadas usan la contraseña de
`geoasist.dev.import-password` (por defecto `geoasist.dev-seed-password`), solo en perfil `dev`.

Comprobaciones: `./mvnw -B spotless:check verify` dentro de `services/api`; `npm run lint`,
`npm run typecheck`, `npm test` y `npm run build` en cada cliente; `npm run validate` y
`npm run check` en `tools/contracts`; `bash infra/tests/retention-restore.sh` desde la raíz.
El ensayo de copias crea solo contenedores y archivos ficticios, sin tocar la base de desarrollo.

Incremento 003: ausencias y estadísticas, asistencia manual y justificaciones, historial propio,
ciclo automático de sesiones y renovación de cuenta; ver `specs/003-ausencias-reportes-cuenta/` y
[su revisión](docs/validation/revision-incremento-003.md).

La [guía de validación](specs/001-asistencia-qr-dinamico/quickstart.md) contiene los recorridos,
el [procedimiento de conservación](docs/operations/conservacion-y-restauracion.md) documenta
backups/WAL/exportaciones y el [registro de revisión](docs/validation/revision-incremento.md)
enumera resultados y condiciones pendientes antes de integrar o utilizar datos reales.

## GitHub Spec Kit

El proyecto incluye habilidades de Spec Kit para **Codex** en `.agents/skills/`.
En el chat de Codex puedes comenzar con:

```text
$speckit-specify Describe aquí la funcionalidad que necesitas.
```

Consulta el [flujo SDD y los comandos para Codex y Claude Code](docs/sdd/README.md).

### Requisitos previos

Antes de instalar y utilizar **GitHub Spec Kit**, asegúrate de que tu sistema cumple con los siguientes requisitos:

* **Python:** Versión `3.11` o superior.
* **Gestor de paquetes `uv`:** Es el gestor recomendado para instalar la herramienta de forma rápida y eficiente.
* **Git:** Debe estar instalado y configurado correctamente en tu sistema.

## Instalación de uv

[uv](https://docs.astral.sh/uv/) es el gestor de paquetes y herramientas de Python que usamos para instalar Spec Kit. Instálalo según tu sistema operativo:

### Linux y macOS

Abre una terminal y ejecuta:

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

Si no tienes `curl`, puedes usar `wget`:

```bash
wget -qO- https://astral.sh/uv/install.sh | sh
```

### Windows

Abre **PowerShell** y ejecuta:

```powershell
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

Alternativa con winget:

```powershell
winget install --id=astral-sh.uv -e
```

### Alternativa con Python y pip

Si ya tienes Python instalado, también puedes instalar `uv` con pip.

**1. Verifica tu versión de Python** (debe ser `3.11` o superior):

```bash
python --version
```

> En Linux y macOS puede que el comando sea `python3 --version`. En Windows también puedes usar `py --version`.

**2. Verifica que pip esté disponible:**

```bash
python -m pip --version
```

**3. Instala uv:**

```bash
python -m pip install uv
```

> Usa `python3` en lugar de `python` si es el comando que te funcionó en el paso 1. En Windows puedes usar `py -m pip install uv`.

Si no tienes Python `3.11` o superior, descárgalo desde https://www.python.org/downloads/. En Windows, marca la casilla **Add Python to PATH** durante la instalación.

### Verificar la instalación

**Cierra y vuelve a abrir la terminal** (para que se actualice el PATH) y ejecuta:

```bash
uv --version
```

Debe mostrar una versión, por ejemplo `uv 0.x.x`. Si dice que el comando no se encuentra, revisa la sección de problemas comunes.

### Problemas comunes

- **`uv: command not found` (Linux/macOS):** cierra y abre la terminal, o ejecuta `source $HOME/.local/bin/env`.
- **`uv` no se reconoce (Windows):** cierra y abre PowerShell. Si persiste, reinicia la sesión.
- **Error de políticas de ejecución (Windows):** usa el comando con `-ExecutionPolicy ByPass` tal como aparece arriba.
- **`python` o `pip` no se reconocen:** prueba con `python3` / `py`, o reinstala Python marcando la opción **Add Python to PATH**.
- **`error: externally-managed-environment` (Linux/macOS):** tu sistema no permite instalar paquetes globales con pip. Usa el instalador oficial de la sección de Linux y macOS.
- **Versión de Python menor a `3.11`:** actualiza Python antes de continuar.

## Instalación de Python

### En Windows

1. **Descargar:** Ve a [python.org/downloads](https://www.python.org/downloads/) y descarga el instalador oficial.
2. **Ejecutar e incluir PATH:** Abre el instalador descargado y **marca imprescindiblemente** la casilla **"Add python.exe to PATH"** en la parte inferior.
3. **Instalar:** Haz clic en **Install Now** y completa el asistente.
4. **Verificar:** Abre la Terminal (CMD o PowerShell) y escribe:
   ```bash
   python --version
   ```
### En Linux 
1. **Actualizar paquetes:** Abre la terminal y ejecuta: sudo apt update
2. **Instalar Python y pip:** Ejecuta el siguiente comando: sudo apt install python3 python3-pip -y
3. **Verificar:** Confirma la instalación con: python3 --version

Documentación oficial: https://docs.astral.sh/uv/getting-started/installation/
