<!-- geoasist-vistas-web:WEB-01 -->
# WEB-01 · Base visual compartida, portada y acceso

Responsable: @C-ael
Fase personal: 1/3
Orden global de commit/push: 1/9
Depende de: ninguna; habilita los componentes compartidos
Rama: `feature/vistas-c-ael`
Commit mínimo sugerido: `feat(web): unificar interfaz y vistas de acceso`

## Objetivo y tareas
- [ ] Definir colores, tipografía, espaciado, botones, tablas, formularios, estados, pestañas, panel lateral y confirmación sobre los componentes existentes.
- [ ] Conservar encabezado UNL · GeoAsist, usuario/rol, menú por rol, menú móvil y Salir.
- [ ] Diseñar portada con mensaje existente y botón Ingresar.
- [ ] Respetar el login real: SSO si /auth/sso-config responde; formulario local como fallback cuando falla esa consulta, sin prometer que esté habilitado en la API.
- [ ] Diseñar formulario externo SSO con correo, contraseña y error; conservar service y campos técnicos del formulario existente.
- [ ] Diseñar callback con procesamiento/error y aviso a estudiantes para usar la app; conservar redirecciones por rol.

## Archivos de referencia y propiedad
- `apps/web/src/app/layout.tsx`
- `apps/web/src/app/globals.css`
- `apps/web/src/app/page.tsx`
- `apps/web/src/app/login/page.tsx`
- `apps/web/src/app/auth/callback/page.tsx`
- `apps/web/src/features/ui/`
- `apps/web/src/features/auth/`
- `services/auth/src/main/resources/templates/login.html`
Los directorios indican componentes de ese dominio; no autorizan modificaciones a trabajo ajeno. Los estilos globales/componentes comunes son de C-ael; proponer cambios antes de tocarlos.

## Criterios de aceptación
- [ ] Menú y redirecciones correctos para los seis roles; no aparecen accesos administrativos para docentes.
- [ ] Se puede recorrer con teclado portada → login → SSO → callback; los fallos son visibles.
- [ ] Los componentes compartidos quedan disponibles antes de WEB-02 y WEB-03.
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

## Prompt para Stitch
```text
Genera el diseño de la entrega WEB-01 de UNL GeoAsist en español, con versiones desktop (1440 px) y móvil (390 px). Reutiliza el mismo sistema visual de las entregas anteriores. Mantén las funciones y los nombres de campos/botones del código; no inventes endpoints, dashboards, gráficos ni permisos. No incluyas documentación, Swagger, diagramas o simuladores. Usa datos ficticios, nunca credenciales. Diseña también carga, vacío, error y éxito, etiquetas accesibles y foco de teclado. Las pestañas/paneles internos no se convierten en rutas nuevas. Alcance:
- Definir colores, tipografía, espaciado, botones, tablas, formularios, estados, pestañas, panel lateral y confirmación sobre los componentes existentes.
- Conservar encabezado UNL · GeoAsist, usuario/rol, menú por rol, menú móvil y Salir.
- Diseñar portada con mensaje existente y botón Ingresar.
- Respetar el login real: SSO si /auth/sso-config responde; formulario local como fallback cuando falla esa consulta, sin prometer que esté habilitado en la API.
- Diseñar formulario externo SSO con correo, contraseña y error; conservar service y campos técnicos del formulario existente.
- Diseñar callback con procesamiento/error y aviso a estudiantes para usar la app; conservar redirecciones por rol.
Criterios:
- Menú y redirecciones correctos para los seis roles; no aparecen accesos administrativos para docentes.
- Se puede recorrer con teclado portada → login → SSO → callback; los fallos son visibles.
- Los componentes compartidos quedan disponibles antes de WEB-02 y WEB-03.
```

## Cierre
Cerrar al integrarse por PR. Publicar el SHA y URL del commit; no cerrar por disponer únicamente del diseño de Stitch. Los diseños generados son referencias, no sustituyen la implementación conectada al backend.

