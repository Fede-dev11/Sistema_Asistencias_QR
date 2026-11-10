<!-- geoasist-vistas-web:WEB-07 -->

<!-- stitch-reference:v1 -->
## Referencia visual obligatoria: exportación de Stitch

Usar las pantallas entregadas por el usuario y versionadas en [el catálogo](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/README.md). Este material sustituye la generación de un estilo nuevo desde cero; conservar las tareas funcionales de este issue.

- **Reportes y estadísticas:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/reportes-estadisticas/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/reportes-estadisticas/code.html).

### Adaptación al código actual
- Adaptar el aspecto de filtros y tabla a /reportes. Conservar actividad, sesión UUID, estado y carrera UUID opcional; no inventar catálogos de períodos.
- Sustituir PDF/Excel por Descargar CSV solo cuando esté autorizado; directores conservan consulta.
- No agregar Notificar, sanciones, normativa o datos personales extra. Las columnas deben corresponder al contrato.

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

# WEB-07 · Reportes y cierre del flujo de acceso/consulta

Responsable: @C-ael
Fase personal: 3/3
Orden global de commit/push: 7/9
Depende de: [WEB-04](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/22), [WEB-06](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/24)
Rama: `feature/vistas-c-ael`
Commit mínimo sugerido: `feat(web): renovar reportes y validar acceso por rol`

## Objetivo y tareas
- [ ] Consultar asistencias: carrera UUID opcional, actividad, sesión UUID y estado Todos/Confirmada/Pendiente de revisión/Rechazada.
- [ ] Consultar y Descargar CSV solo cuando el código y permisos lo permiten; no habilitar exportación para directores.
- [ ] Tabla Cédula/Nombres/Estado/Fecha/Motivo y paginación.
- [ ] No sustituir campos UUID por selectores sin una tarea funcional separada: el diseño debe reflejar la implementación actual.
- [ ] Revisar flujos portada/acceso/rol/salir, sesión vencida, menús responsive y consultas con credenciales de desarrollo.
- [ ] Cerrar revisión de componentes comunes sin modificar los módulos asignados a otras personas; registrar evidencias sintéticas.

## Archivos de referencia y propiedad
- `apps/web/src/app/reportes/page.tsx`
- `apps/web/src/features/reports/AttendanceReportPage.tsx`
- `apps/web/src/features/auth/`
- `apps/web/tests/`
Los directorios indican componentes de ese dominio; no autorizan modificaciones a trabajo ajeno. Los estilos globales/componentes comunes son de C-ael; proponer cambios antes de tocarlos.

## Criterios de aceptación
- [ ] Los reportes conservan filtros, datos y restricciones de exportación existentes.
- [ ] Pruebas de acceso no contienen tokens ni contraseñas reales; no se capturan credenciales en screenshots.
- [ ] El PR de C-ael contiene sus tres commits en orden y evidencia de validación.
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
Genera el diseño de la entrega WEB-07 de UNL GeoAsist en español, con versiones desktop (1440 px) y móvil (390 px). Reutiliza el mismo sistema visual de las entregas anteriores. Mantén las funciones y los nombres de campos/botones del código; no inventes endpoints, dashboards, gráficos ni permisos. No incluyas documentación, Swagger, diagramas o simuladores. Usa datos ficticios, nunca credenciales. Diseña también carga, vacío, error y éxito, etiquetas accesibles y foco de teclado. Las pestañas/paneles internos no se convierten en rutas nuevas. Alcance:
- Consultar asistencias: carrera UUID opcional, actividad, sesión UUID y estado Todos/Confirmada/Pendiente de revisión/Rechazada.
- Consultar y Descargar CSV solo cuando el código y permisos lo permiten; no habilitar exportación para directores.
- Tabla Cédula/Nombres/Estado/Fecha/Motivo y paginación.
- No sustituir campos UUID por selectores sin una tarea funcional separada: el diseño debe reflejar la implementación actual.
- Revisar flujos portada/acceso/rol/salir, sesión vencida, menús responsive y consultas con credenciales de desarrollo.
- Cerrar revisión de componentes comunes sin modificar los módulos asignados a otras personas; registrar evidencias sintéticas.
Criterios:
- Los reportes conservan filtros, datos y restricciones de exportación existentes.
- Pruebas de acceso no contienen tokens ni contraseñas reales; no se capturan credenciales en screenshots.
- El PR de C-ael contiene sus tres commits en orden y evidencia de validación.
```

## Cierre
Cerrar al integrarse por PR. Publicar el SHA y URL del commit; no cerrar por disponer únicamente del diseño de Stitch. Los diseños generados son referencias, no sustituyen la implementación conectada al backend.

