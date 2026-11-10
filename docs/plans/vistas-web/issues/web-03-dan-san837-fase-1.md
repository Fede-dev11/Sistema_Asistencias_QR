<!-- geoasist-vistas-web:WEB-03 -->

<!-- stitch-reference:v1 -->
## Referencia visual obligatoria: exportación de Stitch

Usar las pantallas entregadas por el usuario y versionadas en [el catálogo](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/README.md). Este material sustituye la generación de un estilo nuevo desde cero; conservar las tareas funcionales de este issue.

- **Administración y paralelos:** [captura](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/administracion-paralelos/screen.png) · [HTML](https://github.com/Fede-dev11/Sistema_Asistencias_QR/tree/docs/referencias-stitch-vistas/docs/design/stitch/campus-qr/screens/administracion-paralelos/code.html).

### Adaptación al código actual
- No hay pantalla dedicada de Personas ni Inicio administrativo: derivarlas del estilo de Administración y de los campos del issue.
- Mantener tabla Cédula/Nombres/Cuenta-roles/Estado y formularios reales.
- No incorporar nómina, perfiles con foto, ciclo curricular o integración SIU/SGA inexistentes.

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

# WEB-03 · Inicio administrativo y personas/cuentas

Responsable: @Dan-San837
Fase personal: 1/3
Orden global de commit/push: 3/9
Depende de: [WEB-01](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/19), [WEB-02](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/20)
Rama: `feature/vistas-dan-san837`
Commit mínimo sugerido: `feat(web): renovar inicio administrativo y personas`

## Objetivo y tareas
- [ ] Inicio administrativo: ¿Qué necesitas hacer?, accesos a Personas, Carreras y paralelos, Espacios e Importar listas, con cantidades disponibles.
- [ ] Personas: búsqueda por cédula/nombre, tabla Cédula/Nombres/Cuenta-roles/Estado y paginación.
- [ ] Panel Nueva persona: cédula, nombres, correo opcional, rol y Registrar persona.
- [ ] Panel de edición: nombres, roles por casillas, Cuenta activa y Guardar cambios; manejar persona sin cuenta.
- [ ] Reutilizar los componentes de [WEB-01](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/19); evitar modificar directamente el layout o SessionProvider.

## Archivos de referencia y propiedad
- `apps/web/src/features/home/AdminHome.tsx`
- `apps/web/src/app/admin/personas/page.tsx`
- `apps/web/src/features/admin/PeoplePanel.tsx`
Los directorios indican componentes de ese dominio; no autorizan modificaciones a trabajo ajeno. Los estilos globales/componentes comunes son de C-ael; proponer cambios antes de tocarlos.

## Criterios de aceptación
- [ ] Crear/editar usa las operaciones existentes sin cambiar contratos ni inventar recuperación de contraseña.
- [ ] Panel lateral accesible; conservar borrador al mostrar errores de validación.
- [ ] Solo administrador accede a las acciones y datos administrativos.
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
Genera el diseño de la entrega WEB-03 de UNL GeoAsist en español, con versiones desktop (1440 px) y móvil (390 px). Reutiliza el mismo sistema visual de las entregas anteriores. Mantén las funciones y los nombres de campos/botones del código; no inventes endpoints, dashboards, gráficos ni permisos. No incluyas documentación, Swagger, diagramas o simuladores. Usa datos ficticios, nunca credenciales. Diseña también carga, vacío, error y éxito, etiquetas accesibles y foco de teclado. Las pestañas/paneles internos no se convierten en rutas nuevas. Alcance:
- Inicio administrativo: ¿Qué necesitas hacer?, accesos a Personas, Carreras y paralelos, Espacios e Importar listas, con cantidades disponibles.
- Personas: búsqueda por cédula/nombre, tabla Cédula/Nombres/Cuenta-roles/Estado y paginación.
- Panel Nueva persona: cédula, nombres, correo opcional, rol y Registrar persona.
- Panel de edición: nombres, roles por casillas, Cuenta activa y Guardar cambios; manejar persona sin cuenta.
- Reutilizar los componentes de [WEB-01](https://github.com/Fede-dev11/Sistema_Asistencias_QR/issues/19); evitar modificar directamente el layout o SessionProvider.
Criterios:
- Crear/editar usa las operaciones existentes sin cambiar contratos ni inventar recuperación de contraseña.
- Panel lateral accesible; conservar borrador al mostrar errores de validación.
- Solo administrador accede a las acciones y datos administrativos.
```

## Cierre
Cerrar al integrarse por PR. Publicar el SHA y URL del commit; no cerrar por disponer únicamente del diseño de Stitch. Los diseños generados son referencias, no sustituyen la implementación conectada al backend.

