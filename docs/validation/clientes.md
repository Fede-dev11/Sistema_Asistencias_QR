# Validación de clientes

**Entorno:** Linux. Las pruebas de componentes se ejecutan con Node 22.20.0. El cliente web usa
Chromium mediante Playwright y una API real con PostgreSQL 17.7. Semillas exclusivamente ficticias.

Resultado final: doce pruebas web, diez móviles y cinco recorridos Chromium aprobados; lint,
tipos y builds correctos. El navegador utilizó la web de producción y el JAR empaquetado desde
fuentes sin artefactos previos. Los cinco recorridos completaron en 1,4 min.

## Evidencia automatizada

| Control | Evidencia | Alcance |
|---|---|---|
| Login, error, cierre y revocación | LoginForm/LoginScreen/session tests y API | Web en memoria; móvil SecureStore |
| Cámara solicitada al pulsar | AttendanceScreen.test.tsx | Mock del módulo nativo; falta cámara real |
| QR en fragmento retirado | attendance-flows.spec.ts | Chromium real |
| Cuenta/clase, evento e invitados | attendance-flows.spec.ts | API/PostgreSQL reales |
| Recarga sin nueva apertura y QR vencido tras abrir | attendance-flows.spec.ts | Plazo de servidor conservado |
| Error de red y misma carga/clave en reintento | AttendanceScreen.test.tsx | Simulación controlada |
| Secretos temporales sin PII/bearer | guestResume.test.ts y AttendanceScreen.test.tsx | sessionStorage/SecureStore |
| GPS OFF | AttendanceScreen.test.tsx, LocationDisabledTest y attendance-flows | Sin coordenadas ni dependencia PostGIS |
| GPS ON denegado/precisión ausente | location.test.ts | No interpretar precisión ausente como cero |
| Revisión sin permiso de ubicación | Botón específico y política de dos incertidumbres | Nueva operación idempotente, no nuevas concesiones de tiempo |
| Roles, CSV y auditoría | reporting-scopes.spec.ts | Servidor determina ámbito |
| QR de sesión activa tras autenticarse otra vez y logout | client-recovery.spec.ts | Recorrido Chromium/API |
| Formularios y errores accesibles | Pruebas con roles/labels y foco visible | No equivale a ensayo con lector de pantalla |
| Bundles Android/iOS | `npm run build` en apps/mobile | Exportar bundle no ejecuta el sistema nativo |

## Validación nativa pendiente (T089)

Se localizó el SDK Android en `~/Android/Sdk`, fuera del PATH, con emulador y KVM disponible.
La comprobación anterior que indicaba ausencia del SDK era incorrecta. Se generó el proyecto
Android mediante `npx expo prebuild --platform android --no-install` y un AVD separado del AVD
personal; el primer intento de compilación/arranque se interrumpió y no produjo evidencia de
ejecución de la aplicación. Un segundo intento de `:app:assembleDebug` x86_64 terminó
correctamente en 6 min 2 s (160 tareas). Después se desactivó el permiso de micrófono de
la cámara y se comprobó su ausencia en el manifiesto generado. La recompilación de ese ajuste
y el recorrido nativo se detuvieron por falta de memoria: la swap llegó a llenarse con
Gradle y el emulador simultáneos. Se detuvieron los daemons Gradle/Kotlin identificados como
pertenecientes al proyecto y se preparó una [alternativa de compilación remota y teléfono USB](android-recursos-limitados.md).
El workflow manual de APK está preparado, sin publicar ni ejecutar. Xcode/simulador iOS no
está disponible en este entorno Linux. No se declaran aún recorridos nativos aprobados.
Antes de cerrar T089 ejecutar Android e iOS,
con modelo/versión/cliente/fecha y responsable, y registrar:

1. Permiso de cámara concedido y denegado, lectura QR y reintento.
2. GPS OFF sin permiso ni lectura; GPS ON con precisión, edad, denegación y revisión.
3. Pérdida/recuperación de red y repetición de la misma operación sin doble asistencia.
4. Recuperación del mismo formulario, plazo servidor, expiración/consumo y limpieza SecureStore.
5. Logout con/sin red, revocación/expiración de sesión y ausencia de credenciales persistidas.
6. TalkBack/VoiceOver, tamaño de texto, foco, teclado numérico, etiquetas y errores.
7. Tiempo real escaneo→resultado con buena señal, excluyendo adquisición GPS cuando corresponda.

La latencia HTTP del banco local no completa el ensayo de cámara y señal de SC-001.
