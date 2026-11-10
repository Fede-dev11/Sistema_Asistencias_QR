<!-- geoasist-vistas-web:WEB-02 -->

<!-- stitch-reference:v1 -->
## Referencia visual obligatoria: exportación de Stitch

Usar las pantallas entregadas por el usuario y versionadas en [el catálogo](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/README.md). Este material sustituye la generación de un estilo nuevo desde cero; conservar las tareas funcionales de este issue.

- **Actividades y nueva actividad:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/actividades-nueva-actividad/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/actividades-nueva-actividad/code.html).
- **Detalle de actividad:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/detalle-actividad/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/detalle-actividad/code.html).

### Adaptación al código actual
- Adaptar listado, filtros, tarjetas/tabla y asistente conservando los tres pasos reales y payloads existentes.
- No añadir Exportar en Actividades, biometría, GPS obligatorio, códigos de asignatura o campos sin API.
- Conservar paneles de sesiones, participantes y horario. Invitados/justificaciones/auditoría siguen en #26.

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

# WEB-02 · Actividades, asistente y detalle inicial

Responsable: @AnaPanamito
Fase personal: 1/3
Orden global de commit/push: 2/9
Depende de: [WEB-01](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/19)
Rama: `feature/vistas-anapanamito`
Commit mínimo sugerido: `feat(web): renovar actividades y asistente de creacion`

## Objetivo y tareas
- [ ] Listado /actividades: búsqueda, filtros tipo/estado, tabla Nombre/Tipo/Estado/Próxima sesión, enlace al detalle y Nueva actividad.
- [ ] Asistente /actividades/nueva: Tipo y nombre; Dónde y cuándo; Quiénes. Conservar campos y ramas de Clase/Evento.
- [ ] Clase: espacio, método de verificación, horario semanal opcional, días, horas, rango de fechas, vista previa hasta 200 sesiones y paralelos.
- [ ] Evento: espacio, inicio/fin, acceso abierto/cerrado y tolerancia de invitados superior a 40 segundos. Resumen y Crear clase/evento.
- [ ] Detalle: breadcrumbs, estado, Más acciones y confirmación de cancelación; estructura de pestañas existente.
- [ ] Sesiones: editar inicio/fin, guardar/descartar, cancelar y añadir sesión suelta; participantes: añadir por cédula, quitar y sincronizar; horario: reemplazar programación.

## Archivos de referencia y propiedad
- `apps/web/src/app/actividades/`
- `apps/web/src/features/activities/ActivityList.tsx`
- `apps/web/src/features/activities/ActivityWizard.tsx`
- `apps/web/src/features/activities/ActivityDetail.tsx`
- `apps/web/src/features/activities/ClassSessions.tsx`
- `apps/web/src/features/activities/AuthorizedStudents.tsx`
- `apps/web/src/features/activities/SchedulePanel.tsx`
- `apps/web/src/features/spaces/SpacePicker.tsx`
Los directorios indican componentes de ese dominio; no autorizan modificaciones a trabajo ajeno. Los estilos globales/componentes comunes son de C-ael; proponer cambios antes de tocarlos.

## Criterios de aceptación
- [ ] Crear clase y evento conserva payloads e Idempotency-Key; los errores aparecen junto al campo.
- [ ] Filtros y pestaña seleccionada mantienen parámetros de URL.
- [ ] Invitados, justificaciones y auditoría conservan sus componentes hasta completarse en WEB-08.
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
Ejecutar las pruebas e2e existentes pertinentes con la infraestructura de prueba disponible. No afirmar que un recorrido e2e pasó si no se ejecutó. Para la plantilla SSO de [WEB-01](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/19), verificar también el servicio auth.
Adjuntar captura desktop/móvil sin PII ni secretos y resultado de comprobaciones. Declarar uso de IA y revisión humana.

## Prompt original (solo alcance funcional; diseño sustituido arriba)
```text
Genera el diseño de la entrega WEB-02 de UNL GeoAsist en español, con versiones desktop (1440 px) y móvil (390 px). Reutiliza el mismo sistema visual de las entregas anteriores. Mantén las funciones y los nombres de campos/botones del código; no inventes endpoints, dashboards, gráficos ni permisos. No incluyas documentación, Swagger, diagramas o simuladores. Usa datos ficticios, nunca credenciales. Diseña también carga, vacío, error y éxito, etiquetas accesibles y foco de teclado. Las pestañas/paneles internos no se convierten en rutas nuevas. Alcance:
- Listado /actividades: búsqueda, filtros tipo/estado, tabla Nombre/Tipo/Estado/Próxima sesión, enlace al detalle y Nueva actividad.
- Asistente /actividades/nueva: Tipo y nombre; Dónde y cuándo; Quiénes. Conservar campos y ramas de Clase/Evento.
- Clase: espacio, método de verificación, horario semanal opcional, días, horas, rango de fechas, vista previa hasta 200 sesiones y paralelos.
- Evento: espacio, inicio/fin, acceso abierto/cerrado y tolerancia de invitados superior a 40 segundos. Resumen y Crear clase/evento.
- Detalle: breadcrumbs, estado, Más acciones y confirmación de cancelación; estructura de pestañas existente.
- Sesiones: editar inicio/fin, guardar/descartar, cancelar y añadir sesión suelta; participantes: añadir por cédula, quitar y sincronizar; horario: reemplazar programación.
Criterios:
- Crear clase y evento conserva payloads e Idempotency-Key; los errores aparecen junto al campo.
- Filtros y pestaña seleccionada mantienen parámetros de URL.
- Invitados, justificaciones y auditoría conservan sus componentes hasta completarse en WEB-08.
```

## Cierre
Cerrar al integrarse por PR. Publicar el SHA y URL del commit; no cerrar por disponer únicamente del diseño de Stitch. Los diseños generados son referencias, no sustituyen la implementación conectada al backend.

