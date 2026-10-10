# Sistema_Asistencias_QR

## GitHub Spec Kit

### Requisitos previos

Antes de instalar y utilizar **GitHub Spec Kit**, asegúrate de que tu sistema cumple con los siguientes requisitos:

* **Python:** Versión `3.11` o superior.
* **Gestor de paquetes `uv`:** Es el gestor recomendado para instalar la herramienta de forma rápida y eficiente.
* **Git:** Debe estar instalado y configurado correctamente en tu sistema.

## Instalación de Python

### En Windows

1. **Descargar:** Ve a [python.org/downloads](https://www.python.org/downloads/) y descarga el instalador oficial.
2. **Ejecutar e incluir PATH:** Abre el instalador descargado y **marca imprescindiblemente** la casilla **"Add python.exe to PATH"** en la parte inferior.
3. **Instalar:** Haz clic en **Install Now** y completa el asistente.
4. **Verificar:** Abre la Terminal (CMD o PowerShell) y escribe:
   ```bash
   python --version
### En Linux 
1. **Actualizar paquetes:** Abre la terminal y ejecuta: sudo apt update
2. **Instalar Python y pip:** Ejecuta el siguiente comando: sudo apt install python3 python3-pip -y
3. **Verificar:** Confirma la instalación con: python3 --version

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

### Alternativa para cualquier sistema (con pip)

Si ya tienes Python y pip instalados:

```bash
pip install uv
```

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

Documentación oficial: https://docs.astral.sh/uv/getting-started/installation/