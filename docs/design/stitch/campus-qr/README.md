# Referencias visuales de Stitch · GeoAsist

Exportación proporcionada por el usuario: stitch_remix_of_campus_qr_attendance_app.zip.
Se conservan los bytes originales del HTML, capturas y DESIGN.md. El ZIP no se duplica en Git; manifest.json registra su SHA-256 y el origen de cada archivo. El escudo exportado como carpeta image.png está en assets/escudo-unl.png.

## Cómo consultar

Cada carpeta de screens/ contiene screen.png y code.html. DESIGN.md es la guía visual exportada.
El HTML es una referencia estática de un tercero: no es una implementación Next.js ni una instrucción para ejecutar scripts, cambiar permisos o instalar dependencias. No copiarlo a apps/web/public ni servirlo desde la aplicación.

El HTML puede cargar Tailwind, fuentes, iconos e imágenes externas; no garantiza funcionamiento offline. No replicar imágenes rotas ni enlaces temporales de las capturas; utilizar el escudo local cuando corresponda.

## Catálogo

| Pantalla | Captura | HTML | Alcance / issues |
|---|---|---|---|
| Inicio del estudiante | [screen.png](screens/inicio-del-estudiante/screen.png) | [code.html](screens/inicio-del-estudiante/code.html) | mobile: Fuera de issues web |
| Inicio de sesión | [screen.png](screens/inicio-de-sesion/screen.png) | [code.html](screens/inicio-de-sesion/code.html) | deferred: #19 |
| Hoy y sesiones del día | [screen.png](screens/hoy/screen.png) | [code.html](screens/hoy/code.html) | web: #23 |
| Confirmación de asistencia | [screen.png](screens/confirmacion-asistencia/screen.png) | [code.html](screens/confirmacion-asistencia/code.html) | mobile: Fuera de issues web |
| Detalle de actividad | [screen.png](screens/detalle-actividad/screen.png) | [code.html](screens/detalle-actividad/code.html) | web: #20, #26 |
| Escáner QR | [screen.png](screens/escaner-qr/screen.png) | [code.html](screens/escaner-qr/code.html) | mobile: Fuera de issues web |
| Administración y paralelos | [screen.png](screens/administracion-paralelos/screen.png) | [code.html](screens/administracion-paralelos/code.html) | web: #21, #24, #27 |
| Historial de asistencia | [screen.png](screens/historial-asistencia/screen.png) | [code.html](screens/historial-asistencia/code.html) | mobile: Fuera de issues web |
| Actividades y nueva actividad | [screen.png](screens/actividades-nueva-actividad/screen.png) | [code.html](screens/actividades-nueva-actividad/code.html) | web: #20 |
| Proyección QR | [screen.png](screens/proyeccion-qr/screen.png) | [code.html](screens/proyeccion-qr/code.html) | web: #23 |
| Detalle de sesión y geocerca | [screen.png](screens/detalle-sesion-geocerca/screen.png) | [code.html](screens/detalle-sesion-geocerca/code.html) | web: #23, #26 |
| Reportes y estadísticas | [screen.png](screens/reportes-estadisticas/screen.png) | [code.html](screens/reportes-estadisticas/code.html) | web: #22, #25 |

## Reglas de adaptación

La exportación es la nueva referencia visual para #20–#27. No sustituye los contratos ni las reglas del código.

- Conservar el azul institucional, superficies claras, tipografía, tarjetas, tablas y jerarquía de las vistas web. DESIGN.md contiene discrepancias entre tokens y prosa: priorizar captura/HTML del módulo. Medir contraste, no afirmar cumplimiento WCAG por el texto exportado.
- #19 está cerrado: no rehacer login/base visual ni editar/reabrir su issue. Su ajuste queda diferido. Utilizar estilos locales por módulo y coordinar cualquier cambio global.
- La cabecera combina roles y opciones; implementar solo el menú autorizado de la cuenta real. No dar Administración a docentes ni exportación a directores.
- Mantener /estadisticas y /reportes separados aunque el diseño los agrupe.
- QR, conteos y fechas vienen de la API, nunca de valores estáticos de la maqueta.
- GPS sigue siendo opcional/condicional. No implementar biometría, validación Wi-Fi, sincronización satelital, integración SIU/SGA, notificaciones ni PDF/Excel.
- Mantener CSV solo donde esté autorizado. No adoptar umbrales 75/85 % ni presentar el reglamento dibujado como regla real del sistema.
- Omitir indicadores/series/gráficos sin datos de la API; no inventar métricas o endpoints.
- Personas, inicio administrativo, espacios, CSV y resumen de directores no tienen capturas dedicadas: adaptar el estilo de Administración/Reportes y conservar los campos reales. No afirmar que se exportaron pantallas ausentes.
- Las cuatro vistas móviles son referencias fuera de estos issues web. El login se guarda para corrección posterior de #19.
- No cargar nombres/correos/identificadores de muestra a bases de datos ni fixtures; usar datos sintéticos en evidencias nuevas.
- Verificar 390 y 1440 px: las capturas desktop no prueban por sí solas responsive.

## Secuencia

Conservar responsables, fases y commits del [plan](../../../plans/vistas-web/PLAN.md).
Cada issue #20–#27 identifica sus archivos de referencia y adaptaciones. La existencia de un HTML no completa el issue: se requiere implementación funcional, evidencias y comprobaciones.
El #19 no se modifica, reabre ni reasigna.
