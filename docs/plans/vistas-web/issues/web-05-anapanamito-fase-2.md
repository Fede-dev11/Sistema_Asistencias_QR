<!-- geoasist-vistas-web:WEB-05 -->

<!-- stitch-reference:v1 -->
## Referencia visual obligatoria: exportación de Stitch

Usar las pantallas entregadas por el usuario y versionadas en [el catálogo](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/README.md). Este material sustituye la generación de un estilo nuevo desde cero; conservar las tareas funcionales de este issue.

- **Hoy y sesiones del día:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/hoy/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/hoy/code.html).
- **Detalle de sesión y geocerca:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/detalle-sesion-geocerca/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/detalle-sesion-geocerca/code.html).
- **Proyección QR:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/proyeccion-qr/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/proyeccion-qr/code.html).

### Adaptación al código actual
- Aplicar agenda/tarjetas de Hoy, detalle y proyección dedicada, manteniendo programada/activa/cerrada como estados reales, no pestañas que alteran el estado artificialmente.
- GPS conserva su activación condicional y herencia. Omitir biometría, modelos de teléfono y precisión individual no disponibles.
- QR, renovación, vigencia y presentes deben venir del servidor; conservar pantalla completa y cierre confirmado.

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

# WEB-05 · Hoy, sesiones y proyección del QR

Responsable: @AnaPanamito
Fase personal: 2/3
Orden global de commit/push: 5/9
Depende de: [WEB-02](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/20), [WEB-04](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/22)
Rama: `feature/vistas-anapanamito`
Commit mínimo sugerido: `feat(web): renovar sesiones y proyeccion de qr`

## Objetivo y tareas
- [ ] Hoy: fecha Ecuador, tarjetas con actividad/horario/espacio/estado; acciones Iniciar y proyectar QR, Ver en vivo y Revisar ausencias según estado.
- [ ] Vacío de Hoy: próxima sesión y Ver actividades.
- [ ] /sesiones: seleccionar actividad y sesión para abrir su detalle.
- [ ] Detalle programado: Iniciar y proyectar QR y ubicación. Activo: Proyectar QR, Cerrar sesión, asistencias en vivo y ubicación.
- [ ] Ubicación: habilitación, radio, precisión máxima, antigüedad máxima y Guardar ubicación; valores heredados del espacio.
- [ ] Proyección sin navegación normal: actividad/espacio, QR grande renovable, cuenta regresiva, presentes, Pantalla completa y cierre confirmado.
- [ ] Mantener pestañas de sesión cerrada y sus acciones existentes para completarlas en WEB-08.

## Archivos de referencia y propiedad
- `apps/web/src/app/hoy/page.tsx`
- `apps/web/src/features/home/TeacherToday.tsx`
- `apps/web/src/app/sesiones/`
- `apps/web/src/features/sessions/`
Los directorios indican componentes de ese dominio; no autorizan modificaciones a trabajo ajeno. Los estilos globales/componentes comunes son de C-ael; proponer cambios antes de tocarlos.

## Criterios de aceptación
- [ ] QR conserva token, vigencia y renovación del servidor; no se registra ni se guarda en recursos de diseño.
- [ ] Cerrar sesión requiere confirmación y lleva al resumen; sesión inactiva muestra Volver a la sesión.
- [ ] Temporizadores/polling se limpian al salir; proyección funciona con y sin pantalla completa.
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
Genera el diseño de la entrega WEB-05 de UNL GeoAsist en español, con versiones desktop (1440 px) y móvil (390 px). Reutiliza el mismo sistema visual de las entregas anteriores. Mantén las funciones y los nombres de campos/botones del código; no inventes endpoints, dashboards, gráficos ni permisos. No incluyas documentación, Swagger, diagramas o simuladores. Usa datos ficticios, nunca credenciales. Diseña también carga, vacío, error y éxito, etiquetas accesibles y foco de teclado. Las pestañas/paneles internos no se convierten en rutas nuevas. Alcance:
- Hoy: fecha Ecuador, tarjetas con actividad/horario/espacio/estado; acciones Iniciar y proyectar QR, Ver en vivo y Revisar ausencias según estado.
- Vacío de Hoy: próxima sesión y Ver actividades.
- /sesiones: seleccionar actividad y sesión para abrir su detalle.
- Detalle programado: Iniciar y proyectar QR y ubicación. Activo: Proyectar QR, Cerrar sesión, asistencias en vivo y ubicación.
- Ubicación: habilitación, radio, precisión máxima, antigüedad máxima y Guardar ubicación; valores heredados del espacio.
- Proyección sin navegación normal: actividad/espacio, QR grande renovable, cuenta regresiva, presentes, Pantalla completa y cierre confirmado.
- Mantener pestañas de sesión cerrada y sus acciones existentes para completarlas en WEB-08.
Criterios:
- QR conserva token, vigencia y renovación del servidor; no se registra ni se guarda en recursos de diseño.
- Cerrar sesión requiere confirmación y lleva al resumen; sesión inactiva muestra Volver a la sesión.
- Temporizadores/polling se limpian al salir; proyección funciona con y sin pantalla completa.
```

## Cierre
Cerrar al integrarse por PR. Publicar el SHA y URL del commit; no cerrar por disponer únicamente del diseño de Stitch. Los diseños generados son referencias, no sustituyen la implementación conectada al backend.

