<!-- geoasist-vistas-web:WEB-06 -->

<!-- stitch-reference:v1 -->
## Referencia visual obligatoria: exportación de Stitch

Usar las pantallas entregadas por el usuario y versionadas en [el catálogo](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/README.md). Este material sustituye la generación de un estilo nuevo desde cero; conservar las tareas funcionales de este issue.

- **Administración y paralelos:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/administracion-paralelos/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/administracion-paralelos/code.html).

### Adaptación al código actual
- Referencia directa: carreras seleccionables a la izquierda y paralelos/integrantes a la derecha; adaptar paneles existentes y móvil.
- Mantener Código/Nombre de carrera, Código de paralelo, matriculados/docentes, reemplazos y errores por entrada.
- No incorporar nivel/ciclo/cupos ni sincronización SIU/SGA inexistentes.

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

# WEB-06 · Carreras, paralelos y sus integrantes

Responsable: @Dan-San837
Fase personal: 2/3
Orden global de commit/push: 6/9
Depende de: [WEB-03](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/21), [WEB-05](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/23)
Rama: `feature/vistas-dan-san837`
Commit mínimo sugerido: `feat(web): renovar carreras paralelos e integrantes`

## Objetivo y tareas
- [ ] Tabla de carreras con Código/Nombre y Nueva carrera.
- [ ] Seleccionar carrera para ver tabla Paralelo/Matriculados/Docentes y Nuevo paralelo.
- [ ] Panel de carrera: código/nombre y Crear carrera. Panel de paralelo: código y Crear paralelo.
- [ ] Panel de integrantes con pestañas Matriculados/Docentes y listado paginado, identificador, nombre y vigencia.
- [ ] Área multilínea: cédulas para estudiantes o correos para docentes; Reemplazar matriculados/docentes.
- [ ] Conservar resumen de vigentes/rechazados y tabla Entrada/Motivo; dejar explícito que la operación reemplaza el listado.

## Archivos de referencia y propiedad
- `apps/web/src/app/admin/carreras/page.tsx`
- `apps/web/src/features/admin/CareersPanel.tsx`
- `apps/web/src/features/admin/ParallelMembers.tsx`
Los directorios indican componentes de ese dominio; no autorizan modificaciones a trabajo ajeno. Los estilos globales/componentes comunes son de C-ael; proponer cambios antes de tocarlos.

## Criterios de aceptación
- [ ] La selección de carrera y paralelo es clara y no mezcla los integrantes de diferentes paralelos.
- [ ] Los formularios no inventan edición o eliminación de carreras ausente en el componente actual.
- [ ] Los errores por entrada se muestran sin descartar el listado proporcionado.
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
Genera el diseño de la entrega WEB-06 de UNL GeoAsist en español, con versiones desktop (1440 px) y móvil (390 px). Reutiliza el mismo sistema visual de las entregas anteriores. Mantén las funciones y los nombres de campos/botones del código; no inventes endpoints, dashboards, gráficos ni permisos. No incluyas documentación, Swagger, diagramas o simuladores. Usa datos ficticios, nunca credenciales. Diseña también carga, vacío, error y éxito, etiquetas accesibles y foco de teclado. Las pestañas/paneles internos no se convierten en rutas nuevas. Alcance:
- Tabla de carreras con Código/Nombre y Nueva carrera.
- Seleccionar carrera para ver tabla Paralelo/Matriculados/Docentes y Nuevo paralelo.
- Panel de carrera: código/nombre y Crear carrera. Panel de paralelo: código y Crear paralelo.
- Panel de integrantes con pestañas Matriculados/Docentes y listado paginado, identificador, nombre y vigencia.
- Área multilínea: cédulas para estudiantes o correos para docentes; Reemplazar matriculados/docentes.
- Conservar resumen de vigentes/rechazados y tabla Entrada/Motivo; dejar explícito que la operación reemplaza el listado.
Criterios:
- La selección de carrera y paralelo es clara y no mezcla los integrantes de diferentes paralelos.
- Los formularios no inventan edición o eliminación de carreras ausente en el componente actual.
- Los errores por entrada se muestran sin descartar el listado proporcionado.
```

## Cierre
Cerrar al integrarse por PR. Publicar el SHA y URL del commit; no cerrar por disponer únicamente del diseño de Stitch. Los diseños generados son referencias, no sustituyen la implementación conectada al backend.

