"""Convierte puml/*.puml en SVG (docs/arquitectura/_generated/). Solo libreria estandar."""
import os
import shutil
import subprocess
import sys
from pathlib import Path

VERSION = "1.2025.4"  # release publicada en Maven Central
AQUI = Path(__file__).resolve().parent
SALIDA = AQUI / "_generated"
MVNW = AQUI / ".." / ".." / "services" / "api" / ("mvnw.cmd" if os.name == "nt" else "mvnw")


def jar() -> Path:
    if os.environ.get("PLANTUML_JAR"):
        return Path(os.environ["PLANTUML_JAR"])
    base = Path.home() / ".m2" / "repository" / "net" / "sourceforge" / "plantuml" / "plantuml"
    jars = sorted(base.glob(f"{VERSION}/plantuml-{VERSION}.jar"))
    if not jars:
        print(f"Descargando PlantUML {VERSION} con Maven...")
        subprocess.run(
            [str(MVNW.resolve()), "-q", "dependency:get",
             f"-Dartifact=net.sourceforge.plantuml:plantuml:{VERSION}"],
            check=True, cwd=MVNW.resolve().parent,  # el wrapper resuelve .mvn desde el cwd
        )
        jars = sorted(base.glob(f"{VERSION}/plantuml-{VERSION}.jar"))
    if not jars:
        sys.exit("No se encontro el jar de PlantUML; defina PLANTUML_JAR.")
    return jars[-1]


def main() -> None:
    if not shutil.which("java"):
        sys.exit("Java no esta instalado o no esta en el PATH: PlantUML lo necesita (JDK 17+).")
    fuentes = sorted((AQUI / "puml").glob("*.puml"))
    SALIDA.mkdir(exist_ok=True)
    pendientes = [
        f for f in fuentes
        if not (SALIDA / (f.stem + ".svg")).exists()
        or (SALIDA / (f.stem + ".svg")).stat().st_mtime < f.stat().st_mtime
    ]
    if not pendientes:
        print("PlantUML: SVG al dia, nada que generar.")
        return
    subprocess.run(
        ["java", "-Djava.awt.headless=true", "-jar", str(jar()), "-tsvg", "-charset", "UTF-8",
         "-o", str(SALIDA), *map(str, pendientes)],
        check=True, cwd=AQUI,
    )
    print(f"PlantUML: {len(pendientes)} diagrama(s) generados en {SALIDA}")


if __name__ == "__main__":
    main()
