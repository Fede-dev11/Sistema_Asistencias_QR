<!-- geoasist-vistas-web:WEB-04 -->

<!-- stitch-reference:v1 -->
## Referencia visual obligatoria: exportación de Stitch

Usar las pantallas entregadas por el usuario y versionadas en [el catálogo](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/README.md). Este material sustituye la generación de un estilo nuevo desde cero; conservar las tareas funcionales de este issue.

- **Reportes y estadísticas:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/reportes-estadisticas/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/reportes-estadisticas/code.html).

### Adaptación al código actual
- Usar jerarquía de filtros, tarjetas y tabla para /estadisticas y resumen de directores; el resumen no tiene captura dedicada.
- Mantener 70 % del resumen y umbral de clase informado por API, sin adoptar 75/85 % de la maqueta.
- Omitir evolución semanal, distribución o comparaciones sin datos reales; no agregar PDF/Excel/notificaciones.

### Aceptación visual adicional
- [ ] Adaptar las referencias a componentes React existentes, sin servir HTML exportado como aplicación.
- [ ] Verificar desktop/móvil y adjuntar comparación con la referencia, justificando omisiones por falta de datos o permisos.
- [ ] Mantener responsable, fase, dependencias y commit/push.
- [ ] No modificar ni reabrir #19; login/base visual se corregirán por separado.
- [ ] No copiar secretos, datos personales de muestra, QR estáticos ni scripts remotos al código funcional.

### Prompt de adaptación
```text
Adaptar la vista asignada usando capturas y code.html enlazados. Mantener azul institucional, composición del módulo, desktop/móvil y estados de carga/error/vacío/éxito. Usar solo campos, métricas, permisos y acciones implementados en GeoAsist. No modificar login/base cerrada en #19. Omitir las funciones no implementadas según las reglas de este issue y del catálogo.
```
<!-- /stitch-reference:v1 -->

# WEB-04 · Resumen de directores y estadísticas

Responsable: @C-ael
Fase personal: 2/3
Orden global de commit/push: 4/9
Depende de: [WEB-03](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/21)
Rama: `feature/vistas-c-ael`
Commit mínimo sugerido: `feat(web): renovar resumen y estadisticas por rol`

## Objetivo y tareas
- [ ] Resumen director: número de paralelos bajo 70 %, hasta cinco con menor porcentaje y botón Ver estadísticas completas.
- [ ] Mantener mensaje si todos cumplen el umbral y estado vacío cuando no hay sesiones cerradas.
- [ ] Estadísticas por clase para docentes/encargados: selector, umbral, promedio, listado paginado y marca Bajo umbral.
- [ ] Estadísticas por paralelos: carrera cuando aplique, Desde/Hasta exclusivo, Consultar y tabla de sesiones/asistencias/ausencias/porcentaje.
- [ ] Para directores, conservar estadísticas por carrera y ámbito de la API. No inventar gráficos o métricas ausentes.
- [ ] Mantener la elección de AdminHome o DirectorSummary en /inicio según el rol.

## Archivos de referencia y propiedad
- `apps/web/src/app/inicio/page.tsx`
- `apps/web/src/features/home/DirectorSummary.tsx`
- `apps/web/src/app/estadisticas/page.tsx`
- `apps/web/src/features/stats/`
Los directorios indican componentes de ese dominio; no autorizan modificaciones a trabajo ajeno. Los estilos globales/componentes comunes son de C-ael; proponer cambios antes de tocarlos.

## Criterios de aceptación
- [ ] Administrador mantiene su inicio; directores ven su resumen, no el administrativo.
- [ ] Los valores nulos se distinguen de cero; fechas y porcentajes conservan la semántica actual.
- [ ] No se amplía el ámbito de datos ni permisos de exportación.
- [ ] Commit propio y push realizados antes de iniciar la siguiente fase.
- [ ] Evidencia desktop/móvil, comprobaciones y referencia al commit adjuntas al issue.

## Reglas de ejecución
- Trabajar sobre la implementación existente; no crear endpoints ni rediseñar reglas de negocio.
- Diseño responsive para 390 px y 1440 px; navegación por teclado, etiquetas y contraste legibles.
- Mantener carga, error, vacío, éxito, sesión vencida y denegación por rol.
- No incluir documentación, Swagger, arquitectura, simuladores de prueba, claves ni datos reales.
- No modificar archivos ajenos a la entrega ni regenerar todo el proyecto desde Stitch.
- No hacer commits directos a main/develop. Una rama feature por persona, tres commits mínimos y un PR por persona. Cada fase debe publicarse antes de iniciar la siguiente.
- Conservar autoría real: cada responsable realiza sus commits y push con su cuenta; no suplantar autores.
- No usar squash ni agrupar las tres fases en un único commit al integrar.

## Comprobación de la entrega
Desde apps/web:
```bash
npm run lint
npm run typecheck
npm test
npm run build
```
Ejecutar las pruebas e2e existentes pertinentes con la infraestructura de prueba disponible. No afirmar que un recorrido e2e pasó si no se ejecutó. Para la plantilla SSO de WEB-01, verificar también el servicio auth.
Adjuntar captura desktop/móvil sin PII ni secretos y resultado de comprobaciones. Declarar uso de IA y revisión humana.

## Prompt original (solo alcance funcional; diseño sustituido arriba)
```text
Genera el diseño de la entrega WEB-04 de UNL GeoAsist en español, con versiones desktop (1440 px) y móvil (390 px). Reutiliza el mismo sistema visual de las entregas anteriores. Mantén las funciones y los nombres de campos/botones del código; no inventes endpoints, dashboards, gráficos ni permisos. No incluyas documentación, Swagger, diagramas o simuladores. Usa datos ficticios, nunca credenciales. Diseña también carga, vacío, error y éxito, etiquetas accesibles y foco de teclado. Las pestañas/paneles internos no se convierten en rutas nuevas. Alcance:
- Resumen director: número de paralelos bajo 70 %, hasta cinco con menor porcentaje y botón Ver estadísticas completas.
- Mantener mensaje si todos cumplen el umbral y estado vacío cuando no hay sesiones cerradas.
- Estadísticas por clase para docentes/encargados: selector, umbral, promedio, listado paginado y marca Bajo umbral.
- Estadísticas por paralelos: carrera cuando aplique, Desde/Hasta exclusivo, Consultar y tabla de sesiones/asistencias/ausencias/porcentaje.
- Para directores, conservar estadísticas por carrera y ámbito de la API. No inventar gráficos o métricas ausentes.
- Mantener la elección de AdminHome o DirectorSummary en /inicio según el rol.
Criterios:
- Administrador mantiene su inicio; directores ven su resumen, no el administrativo.
- Los valores nulos se distinguen de cero; fechas y porcentajes conservan la semántica actual.
- No se amplía el ámbito de datos ni permisos de exportación.
```

## Cierre
Cerrar al integrarse por PR. Publicar el SHA y URL del commit; no cerrar por disponer únicamente del diseño de Stitch. Los diseños generados son referencias, no sustituyen la implementación conectada al backend.

