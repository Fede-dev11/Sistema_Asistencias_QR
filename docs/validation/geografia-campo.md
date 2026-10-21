# Validación geográfica de campo

**Estado:** PENDIENTE

**Modo permitido mientras esté pendiente:** `REVIEW_ONLY` (`PILOTO_REVISION`)
**Modo bloqueado:** `AUTO` (`VALIDACION_AUTOMATICA`)

Este documento prepara el ensayo real requerido antes de registrar una evidencia geográfica
verificada en el servidor. Las pruebas automatizadas y coordenadas ficticias no sustituyen este
ensayo y no producen tasas institucionales de falsos positivos o negativos.

## Responsables y trazabilidad

Completar antes de ejecutar:

- Responsable del ensayo:
- Responsable que verifica y carga la evidencia:
- Fecha y zona horaria:
- Referencia documental inmutable:
- Versión de la evidencia:
- Versión del API, aplicación móvil y configuración PostGIS:

## Espacios mínimos

Ejecutar el mismo protocolo en, como mínimo:

1. Un aula real, con centro y radio aprobados por su responsable.
2. Un espacio abierto real, con centro y radio aprobados por su responsable.

Para cada espacio registrar nombre, tipo, dimensiones, obstrucciones, densidad de personas y
condiciones ambientales relevantes. No incluir coordenadas de participantes en este documento.

## Dispositivos y red

Registrar por dispositivo:

| Campo | Valor real |
|---|---|
| Fabricante/modelo | PENDIENTE |
| Sistema y versión | PENDIENTE |
| Cliente y versión | PENDIENTE |
| Proveedor de ubicación | PENDIENTE |
| Red disponible | PENDIENTE |
| Condición de señal | PENDIENTE |

Incluir al menos dos dispositivos con capacidades distintas. Documentar conectividad estable e
intermitente sin convertir el tiempo de adquisición GPS en latencia del servidor.

## Parámetros por ejecución

| Espacio | Radio (m) | Precisión máxima (m) | Antigüedad máxima (s) | Muestras interiores | Muestras exteriores |
|---|---:|---:|---:|---:|---:|
| Aula | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |
| Espacio abierto | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |

## Protocolo

1. Verificar que la sesión permanezca en `REVIEW_ONLY` y que no exista evidencia previa asociada.
2. Fijar centro, radio, umbral de precisión y antigüedad antes de iniciar la sesión.
3. Definir posiciones interiores, exteriores y cercanas al límite sin usar datos personales.
4. Obtener una medición nueva por intento. Registrar solo precisión y distancia redondeadas que
   conserva el servidor; no copiar latitud o longitud a archivos de resultados.
5. Repetir en cada posición y dispositivo, alternando red estable e intermitente.
6. Clasificar cada resultado esperado con observación humana del punto de prueba, no de la
   identidad de una persona.
7. Calcular falsos positivos como aceptaciones de muestras exteriores sobre el total exterior, y
   falsos negativos como rechazos de muestras interiores sobre el total interior. Reportar también
   repeticiones y revisiones; no ocultarlas del denominador documentado.
8. Revisar la referencia, fecha, versión, responsable, muestra y tasas. Solo entonces un operador
   autorizado puede insertar la evidencia verificada del espacio en el servidor.
9. Repetir la configuración de una nueva sesión y comprobar que el servidor deriva `AUTO`; el
   cliente nunca envía el modo ni un indicador de evidencia.

## Resultados reales

| Espacio | Muestras | Falsos positivos | Tasa FP | Falsos negativos | Tasa FN | Repeticiones | Revisiones |
|---|---:|---:|---:|---:|---:|---:|---:|
| Aula | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |
| Espacio abierto | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |

## Criterio para cerrar la tarea

T071 permanece sin marcar hasta completar ambos espacios con dispositivos reales, resultados
observados, responsables, fecha y referencia verificable. La aceptación de las tasas corresponde
al equipo y a la autoridad institucional. Hasta entonces no se insertará evidencia en
`evidencias_geograficas` y el sistema mantendrá `PILOTO_REVISION`.
