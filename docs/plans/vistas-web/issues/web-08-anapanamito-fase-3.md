<!-- geoasist-vistas-web:WEB-08 -->

<!-- stitch-reference:v1 -->
## Referencia visual obligatoria: exportación de Stitch

Usar las pantallas entregadas por el usuario y versionadas en [el catálogo](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/README.md). Este material sustituye la generación de un estilo nuevo desde cero; conservar las tareas funcionales de este issue.

- **Detalle de actividad:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/detalle-actividad/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/detalle-actividad/code.html).
- **Detalle de sesión y geocerca:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/detalle-sesion-geocerca/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/detalle-sesion-geocerca/code.html).

### Adaptación al código actual
- Las capturas definen contenedor, pestañas y confirmaciones; faltan capturas dedicadas de algunos formularios de invitados/justificaciones/revisiones. Derivarlos con el mismo estilo.
- Conservar motivos, Aprobar/Rechazar, asistencia manual, ausencias, listado de invitados y auditoría con permisos/plazos existentes.
- No añadir atrasos, quórum, modelos de equipo ni reglas que solo estén dibujadas.

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

# WEB-08 · Invitados, justificaciones, auditoría y correcciones

Responsable: @AnaPanamito
Fase personal: 3/3
Orden global de commit/push: 8/9
Depende de: [WEB-05](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/23), [WEB-07](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/25)
Rama: `feature/vistas-anapanamito`
Commit mínimo sugerido: `feat(web): renovar invitados y correcciones de asistencia`

## Objetivo y tareas
- [ ] Evento: habilitar invitados sin cuenta, tiempo del formulario y Guardar configuración; archivo JSON/textarea del listado y Reemplazar listado (máximo 1000).
- [ ] Justificaciones: pendientes, razón de decisión y Aprobar/Rechazar.
- [ ] Auditoría de actividad: Consultar auditoría y tabla Fecha/Acción/Responsable/Razón.
- [ ] Sesión cerrada: contadores, ausencias, Justificar, razón, Confirmar justificación y Descartar.
- [ ] Asistencia manual: estudiante, razón y Registrar asistencia. Revisión: ID del intento, razón y Aprobar/Rechazar.
- [ ] Conservar confirmación de Cerrar actividad definitivamente y el aviso de conservación; no crear páginas separadas para paneles internos.

## Archivos de referencia y propiedad
- `apps/web/src/features/activities/GuestListView.tsx`
- `apps/web/src/features/activities/GuestSettingsPanel.tsx`
- `apps/web/src/features/corrections/`
- `apps/web/src/features/reviews/ReviewPanel.tsx`
- `apps/web/src/features/reports/ActivityAuditPanel.tsx`
- `apps/web/src/features/sessions/SessionSummary.tsx`
Los directorios indican componentes de ese dominio; no autorizan modificaciones a trabajo ajeno. Los estilos globales/componentes comunes son de C-ael; proponer cambios antes de tocarlos.

## Criterios de aceptación
- [ ] Listados de invitados conservan reemplazo completo y validaciones; no se habilitan por defecto.
- [ ] Las correcciones conservan razón, permisos, plazos y auditoría de la API.
- [ ] El recorrido actividad → sesión → QR → cierre → ausencias/corrección funciona; PR con tres commits atribuibles.
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
Genera el diseño de la entrega WEB-08 de UNL GeoAsist en español, con versiones desktop (1440 px) y móvil (390 px). Reutiliza el mismo sistema visual de las entregas anteriores. Mantén las funciones y los nombres de campos/botones del código; no inventes endpoints, dashboards, gráficos ni permisos. No incluyas documentación, Swagger, diagramas o simuladores. Usa datos ficticios, nunca credenciales. Diseña también carga, vacío, error y éxito, etiquetas accesibles y foco de teclado. Las pestañas/paneles internos no se convierten en rutas nuevas. Alcance:
- Evento: habilitar invitados sin cuenta, tiempo del formulario y Guardar configuración; archivo JSON/textarea del listado y Reemplazar listado (máximo 1000).
- Justificaciones: pendientes, razón de decisión y Aprobar/Rechazar.
- Auditoría de actividad: Consultar auditoría y tabla Fecha/Acción/Responsable/Razón.
- Sesión cerrada: contadores, ausencias, Justificar, razón, Confirmar justificación y Descartar.
- Asistencia manual: estudiante, razón y Registrar asistencia. Revisión: ID del intento, razón y Aprobar/Rechazar.
- Conservar confirmación de Cerrar actividad definitivamente y el aviso de conservación; no crear páginas separadas para paneles internos.
Criterios:
- Listados de invitados conservan reemplazo completo y validaciones; no se habilitan por defecto.
- Las correcciones conservan razón, permisos, plazos y auditoría de la API.
- El recorrido actividad → sesión → QR → cierre → ausencias/corrección funciona; PR con tres commits atribuibles.
```

## Cierre
Cerrar al integrarse por PR. Publicar el SHA y URL del commit; no cerrar por disponer únicamente del diseño de Stitch. Los diseños generados son referencias, no sustituyen la implementación conectada al backend.

