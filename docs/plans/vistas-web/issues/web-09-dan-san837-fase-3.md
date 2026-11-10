<!-- geoasist-vistas-web:WEB-09 -->

<!-- stitch-reference:v1 -->
## Referencia visual obligatoria: exportación de Stitch

Usar las pantallas entregadas por el usuario y versionadas en [el catálogo](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/README.md). Este material sustituye la generación de un estilo nuevo desde cero; conservar las tareas funcionales de este issue.

- **Administración y paralelos:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/administracion-paralelos/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/administracion-paralelos/code.html).
- **Detalle de sesión y geocerca:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/detalle-sesion-geocerca/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/detalle-sesion-geocerca/code.html).

### Adaptación al código actual
- No hay capturas dedicadas de Espacios, evidencia o CSV. Derivar formularios de Administración y composición del panel de geocerca.
- Conservar mapa, coordenadas, radio/precisión y evidencia versionada sin edición/eliminación.
- Conservar CSV UTF-8 máximo 2 MB/5000 filas, tipos y errores existentes; no agregar Excel ni fuentes SIU/SGA.

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

# WEB-09 · Espacios, evidencia GPS e importación CSV

Responsable: @Dan-San837
Fase personal: 3/3
Orden global de commit/push: 9/9
Depende de: [WEB-06](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/24), [WEB-08](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/26)
Rama: `feature/vistas-dan-san837`
Commit mínimo sugerido: `feat(web): renovar espacios evidencia geografica e importacion`

## Objetivo y tareas
- [ ] Espacios: buscar por código/nombre, tabla Código/Nombre/Tipo/Ubicación, Nuevo espacio y paginación.
- [ ] Formulario: código (inmutable al editar), nombre, tipo Aula/Laboratorio/Auditorio/Exterior/Otro, mapa, latitud/longitud, radio y precisión predeterminados; Guardar espacio.
- [ ] Evidencia: historial versión/referencia/fecha/falsos positivos/falsos negativos; registrar con valores 0..1. No ofrecer edición/eliminación de evidencia.
- [ ] Importar CSV: Estudiantes/Docentes/Matrículas, columnas esperadas, archivo UTF-8 máximo 2 MB y 5000 filas, Importar/Importando y errores por fila.
- [ ] Validar formularios administrativos en 390/1440 px, navegación con teclado y permisos; cerrar integración del plan.

## Archivos de referencia y propiedad
- `apps/web/src/app/admin/espacios/page.tsx`
- `apps/web/src/features/spaces/SpaceForm.tsx`
- `apps/web/src/features/spaces/GeoEvidencePanel.tsx`
- `apps/web/src/app/admin/importar/page.tsx`
- `apps/web/src/features/admin/ImportPanel.tsx`
Los directorios indican componentes de ese dominio; no autorizan modificaciones a trabajo ajeno. Los estilos globales/componentes comunes son de C-ael; proponer cambios antes de tocarlos.

## Criterios de aceptación
- [ ] Mapa y coordenadas permanecen sincronizados; no se confunden latitud y longitud.
- [ ] CSV muestra aceptados/rechazados y sus causas sin inventar resultados ni exponer datos reales.
- [ ] El PR de Dan-San837 conserva tres commits y la integración final pasa lint, tipos, tests y build.
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
Genera el diseño de la entrega WEB-09 de UNL GeoAsist en español, con versiones desktop (1440 px) y móvil (390 px). Reutiliza el mismo sistema visual de las entregas anteriores. Mantén las funciones y los nombres de campos/botones del código; no inventes endpoints, dashboards, gráficos ni permisos. No incluyas documentación, Swagger, diagramas o simuladores. Usa datos ficticios, nunca credenciales. Diseña también carga, vacío, error y éxito, etiquetas accesibles y foco de teclado. Las pestañas/paneles internos no se convierten en rutas nuevas. Alcance:
- Espacios: buscar por código/nombre, tabla Código/Nombre/Tipo/Ubicación, Nuevo espacio y paginación.
- Formulario: código (inmutable al editar), nombre, tipo Aula/Laboratorio/Auditorio/Exterior/Otro, mapa, latitud/longitud, radio y precisión predeterminados; Guardar espacio.
- Evidencia: historial versión/referencia/fecha/falsos positivos/falsos negativos; registrar con valores 0..1. No ofrecer edición/eliminación de evidencia.
- Importar CSV: Estudiantes/Docentes/Matrículas, columnas esperadas, archivo UTF-8 máximo 2 MB y 5000 filas, Importar/Importando y errores por fila.
- Validar formularios administrativos en 390/1440 px, navegación con teclado y permisos; cerrar integración del plan.
Criterios:
- Mapa y coordenadas permanecen sincronizados; no se confunden latitud y longitud.
- CSV muestra aceptados/rechazados y sus causas sin inventar resultados ni exponer datos reales.
- El PR de Dan-San837 conserva tres commits y la integración final pasa lint, tipos, tests y build.
```

## Cierre
Cerrar al integrarse por PR. Publicar el SHA y URL del commit; no cerrar por disponer únicamente del diseño de Stitch. Los diseños generados son referencias, no sustituyen la implementación conectada al backend.

