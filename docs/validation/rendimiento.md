# Rendimiento de laboratorio

Ensayo ejecutado el 20 de octubre de 2026. Resultado bruto sin datos nominales ni secretos en
[rendimiento-laboratorio.json](rendimiento-laboratorio.json).

| Parámetro | Medición |
|---|---|
| Duración | 600,03 s |
| Carga | 200 intentos/min; 2.000 intentos |
| Mezcla | 50 personas ficticias × cinco sesiones EVENTO, GPS OFF |
| Concurrencia | Ráfagas de 50 solicitudes cada 15 s |
| Nuevas asistencias | 250 respuestas 201 |
| Duplicados coherentes | 1.750 respuestas 200; claves nuevas por intento |
| Errores | 0 |
| p95 HTTP | 487,77 ms |
| Máximo HTTP | 667,97 ms |
| Bloqueos DB | Máximo observado 0; 119 muestras cada cinco segundos |
| CPU | AMD Ryzen 7 3700U, ocho CPU lógicas |
| Memoria | 9,1 GiB |
| Red | Loopback HTTP; API y PostgreSQL Docker en el mismo host Linux |
| Herramientas | Java 21.0.12.1, PostgreSQL 17.7; harness Node 24.15.0 disponible al iniciarlo |

La latencia HTTP incluye red local, servidor y cola de respuesta; no incluye adquisición GPS
ni escaneo. En este perfil es menor que dos segundos en el percentil 95. La máquina también
ejecutó verificaciones de construcción durante parte del ensayo. El muestreo de bloqueos es
grueso y puede perder esperas breves entre muestras; no prueba ausencia de contención.

La prueba de unicidad SC-003 es independiente: cien rondas de cien solicitudes simultáneas
para la misma persona/sesión en PostgreSQL real. El banco de 2.000 solicitudes no la sustituye.

## Reproducir

Iniciar PostgreSQL local y la API con datos ficticios en 18081 (sin multiplicador de límites).
Usar la contraseña ficticia y HMAC de `.env` mediante el cargador, sin imprimirlos:

```bash
# services/api
python3 ../../infra/with-local-env.py -- env SPRING_PROFILES_ACTIVE=dev API_PORT=18081 ./mvnw spring-boot:run
# raíz, otra terminal; Node >=22.13 (proyecto fijado en 22.20.0)
python3 infra/with-local-env.py -- env API_URL=http://127.0.0.1:18081/api/v1 node tests/performance/attendance.js
```

El script crea cincuenta cuentas ficticias aisladas de las semillas, cinco eventos y sesiones.
Las identidades y tokens se mantienen en memoria y no se incluyen en el resultado. El resultado
se reemplaza en cada ejecución y el proceso falla si hay errores, menos de 2.000 respuestas o
p95 HTTP ≥2 s. No usar este generador con datos reales.

## Pendientes

El tiempo **escaneo→resultado <10 s con buena señal** sigue sin medición en cámara/dispositivo
real (SC-001, T088/T089). El ensayo no acredita capacidad institucional ni rendimiento sobre
Wi-Fi de aula, telefonía móvil, CAS, GPS o infraestructura productiva. T088 se mantiene abierta
hasta adjuntar esa parte del recorrido real.
