# Android sin emulador local

El 20 de octubre de 2026, el equipo de 9,1 GiB de RAM llegó a ocupar toda su swap de
7,5 GiB al ejecutar Gradle, el emulador, Metro, la API y el IDE. La primera compilación
Android x86_64 terminó correctamente en 6 min 2 s, pero no se completó el recorrido nativo.
El APK posterior al ajuste de permisos todavía requiere compilación y validación.

## Opción preferida: compilación remota y teléfono por USB

El workflow `.github/workflows/android-apk.yml` compila únicamente al solicitarlo. Usa la
máquina de GitHub, sin iniciar emuladores ni instalar Gradle en el equipo del desarrollador.
Genera un APK de depuración ARM64, firmado con la clave de desarrollo de Expo y conservado
como artefacto durante tres días. No es una entrega para publicar en una tienda.

El workflow está preparado localmente; aún no está publicado ni ejecutado en GitHub.
Después de publicar la rama y habilitar el workflow, ejecutarlo desde Actions y descargar
`geoasist-android-debug-arm64`. Confirmar que el teléfono usa `arm64-v8a`:

```bash
adb shell getprop ro.product.cpu.abi
adb install -r /ruta/al/app-debug.apk
adb reverse tcp:8081 tcp:18083
adb reverse tcp:18082 tcp:18082
```

Ejecutar los comandos ADB con un único teléfono conectado y depuración USB autorizada.
Este APK de depuración necesita Metro. Desde `apps/mobile`, iniciar solo ese servidor:

```bash
EXPO_PUBLIC_API_URL=http://127.0.0.1:18082/api/v1 npx expo start --localhost --port 18083 --max-workers 1
```

Abrir la app en el teléfono. El puerto 8081 del teléfono se redirige a Metro; el 18082 se
redirige a la API del equipo. Iniciar la API en ese puerto con los datos ficticios y limitar
su heap con `java -Xmx384m -jar ...`. Si la API está en otro equipo accesible por HTTPS,
configurar esa URL en `EXPO_PUBLIC_API_URL` y omitir la segunda redirección.
No arrancar Next.js ni los bancos de carga para este recorrido. Usar cédulas y cuentas ficticias.

## Si es imprescindible compilar localmente

Cerrar primero el emulador y Metro. Compilar una arquitectura y un único worker:

```bash
# Desde apps/mobile; requiere SDK, Node y JDK 21 configurados.
npx expo prebuild --platform android --no-install
cd android
NODE_ENV=development ./gradlew :app:assembleDebug --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1024m -Pkotlin.compiler.execution.strategy=in-process -PreactNativeArchitectures=arm64-v8a
```

El límite corresponde al heap de Gradle; otros procesos y herramientas nativas consumen
memoria adicional. No garantiza que la compilación quepa en cualquier equipo.
Esperar a que termine antes de iniciar Metro o la API. Para un emulador x86_64 cambiar la
arquitectura, pero reservar ese recorrido para otro equipo con memoria suficiente.

La configuración local ignorada `.vscode/settings.json` desactiva la importación automática
de Gradle y el seguimiento de los directorios Android/iOS generados y del AVD de validación.
La API Maven conserva su configuración. Si VS Code mantiene un daemon previo, recargar la
ventana para aplicar el ajuste. No borrar la swap ni las cachés como solución a la falta de RAM.

Compilar un APK no completa T089: registrar por separado los permisos, cámara, GPS OFF,
red, recuperación, logout y accesibilidad reales descritos en `clientes.md`. iOS y los ensayos
de campo continúan pendientes.
