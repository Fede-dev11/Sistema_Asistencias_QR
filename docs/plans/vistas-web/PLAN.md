# Plan de vistas web · tres integrantes, tres fases por integrante

Fecha: 2026-11-10. Repositorio: Fede-dev11/Sistema_Asistencias_QR.
Estado: nueve issues publicados en GitHub (#19–#27), con responsables y dependencias. Enlaces confirmados en published.json.

## Resultado esperado

Mejorar las vistas existentes de GeoAsist con un diseño coherente generado con apoyo de Stitch e implementado en Next.js. El trabajo comprende diseño, integración con el código actual y validación. Son nueve entregas funcionales, tres por integrante, con al menos un commit propio por entrega (mínimo nueve commits). Este plan no ejecuta esos commits ni suplanta a sus autores.

La aplicación ya tiene rutas y lógica: cada integrante debe modificar sus componentes existentes y conservar llamadas a la API, roles, estados, confirmaciones, filtros, parámetros de URL e idempotencia. No se parte de una aplicación vacía.

## Responsables y límites

| Persona | Dominio | Tres fases |
|---|---|---|
| @C-ael | Base visual, autenticación y consultas | WEB-01 acceso/base → WEB-04 resumen/estadísticas → WEB-07 reportes/integración de acceso |
| @AnaPanamito | Operación de actividades y asistencia | WEB-02 actividades → WEB-05 sesiones/QR → WEB-08 invitados/correcciones |
| @Dan-San837 | Administración | WEB-03 personas → WEB-06 carreras/paralelos → WEB-09 espacios/CSV |

C-ael es propietario de layout.tsx, globals.css, features/ui y navegación/sesión. Los otros integrantes reutilizan esa base y mantienen estilos propios con ámbito local; cualquier cambio global se coordina con C-ael. /inicio conserva AdminHome (Dan-San837) y DirectorSummary (C-ael); no reemplazar uno por otro.

## Orden obligatorio de commits y publicaciones

Para interpretar “commits seguidos” se fija una secuencia global de nueve entregas. Cada persona termina y publica su fase antes de comenzar la siguiente propia; además se respeta el predecesor global. No comenzar varias fases personales a la vez.

| Orden | Issue local | Persona | Fase | Commit mínimo |
|---|---|---|---|---|
| 1 | WEB-01 | @C-ael | 1/3 | `feat(web): unificar interfaz y vistas de acceso` |
| 2 | WEB-02 | @AnaPanamito | 1/3 | `feat(web): renovar actividades y asistente de creacion` |
| 3 | WEB-03 | @Dan-San837 | 1/3 | `feat(web): renovar inicio administrativo y personas` |
| 4 | WEB-04 | @C-ael | 2/3 | `feat(web): renovar resumen y estadisticas por rol` |
| 5 | WEB-05 | @AnaPanamito | 2/3 | `feat(web): renovar sesiones y proyeccion de qr` |
| 6 | WEB-06 | @Dan-San837 | 2/3 | `feat(web): renovar carreras paralelos e integrantes` |
| 7 | WEB-07 | @C-ael | 3/3 | `feat(web): renovar reportes y validar acceso por rol` |
| 8 | WEB-08 | @AnaPanamito | 3/3 | `feat(web): renovar invitados y correcciones de asistencia` |
| 9 | WEB-09 | @Dan-San837 | 3/3 | `feat(web): renovar espacios evidencia geografica e importacion` |

Los commits pueden ser más de nueve si se necesitan correcciones, pero nunca menos de tres por persona. No crear commits vacíos para completar el número.

## Flujo Git

1. Cada persona configura su identidad real de Git y crea su rama feature/vistas-<usuario> desde la base de integración aprobada.
2. Preparar un PR por persona hacia develop, conforme a la constitución del proyecto. Si develop aún no existe en remoto, acordar/crear esa rama antes de integrar; no empujar directamente a main.
3. En cada turno global: actualizar la rama con las entregas previas necesarias, implementar únicamente la fase asignada y ejecutar las comprobaciones.
4. Revisar el diff, agregar archivos específicos, crear el commit indicado y hacer push a la rama personal. No usar git add . sobre cambios ajenos.
5. Añadir al issue el SHA/URL del commit, capturas sintéticas y resultados de validación; actualizar el PR.
6. Solo entonces avanzar al siguiente turno global. Los cambios base de WEB-01 deben estar disponibles en las ramas posteriores mediante la rama de integración o un cherry-pick coordinado. No duplicar archivos comunes.
7. Integrar con revisión humana y preservando los commits (merge/rebase sin squash). Cerrar los issues al integrar, no por generar una imagen en Stitch.

Ejemplo para una fase; reemplazar rama, rutas y mensaje por los propios:
```bash
git status --short
git add RUTA_DE_ARCHIVO_PROPIO
git commit -m "feat(web): mensaje de la fase"
git push -u origin feature/vistas-usuario
```

No ejecutar estos comandos automáticamente con los cambios actuales del workspace: existen modificaciones anteriores ajenas a este plan. Ninguna entrega de diseño autoriza modificar Expo, backend, contratos o datos de desarrollo salvo el formulario HTML SSO expresamente asignado en WEB-01.

## Pantallas cubiertas

| Área | Rutas/paneles | Responsable |
|---|---|---|
| Acceso | /, /login, /auth/callback, formulario externo SSO, aviso de estudiante | C-ael |
| Operación diaria | /hoy, /actividades, /actividades/nueva, /actividades/[id] | AnaPanamito |
| Sesiones | /sesiones, /sesiones/[id], /sesiones/[id]/proyectar, ubicación y asistencia en vivo | AnaPanamito |
| Paneles de actividad | Sesiones, participantes, horario, invitados, justificaciones, auditoría | AnaPanamito |
| Paneles de sesión cerrada | Resumen/ausencias, asistencias, asistencia manual, revisiones | AnaPanamito |
| Consulta | /inicio para directores, /estadisticas, /reportes | C-ael |
| Administración | /inicio para administrador, /admin/personas, /admin/carreras, integrantes, /admin/espacios, evidencia GPS, /admin/importar | Dan-San837 |

Fuera de alcance: /documentacion y subrutas, GitHub Pages, diagramas, /pruebas/*, app móvil, nuevos endpoints, nuevas políticas de permisos y nuevas pantallas no implementadas como registro público o recuperación de contraseña. No borrar rutas fuera del alcance.

## Uso de Stitch

- Referencia vigente: [exportación de Stitch aportada por el usuario](../../design/stitch/campus-qr/README.md). #20–#27 deben adaptarse a estas capturas/HTML, sin generar un estilo alternativo.
- WEB-01 (#19) está cerrado y se conserva intacto. Su ajuste al diseño queda diferido; no editar/reabrir/reasignar. Para #20–#27 mantener español, 390/1440 px, etiquetas, foco y contraste.
- #20–#27 incluyen referencias precisas y un prompt de adaptación. Los prompts originales solo conservan alcance funcional; ahora se parte de la exportación. Mantener carga/error/vacío/éxito con datos ficticios.
- Guardar el enlace e identificadores de proyecto/pantalla de Stitch en el issue al generarlos; no inventarlos ni marcar capturas como implementación.
- Adaptar el diseño a los componentes actuales. No pegar una página HTML entera que elimine RoleGuard, SessionProvider, clientes de API o controles de seguridad.
- La cuenta universitaria se ejecuta en services/auth; no mover la captura de contraseña SSO a la pantalla Next.js.
- La disponibilidad del formulario local en LoginForm depende actualmente de que falle la consulta de configuración SSO. No inventar un selector permanente de acceso ni confundir esto con habilitar /auth/login en la API.
- No enviar .env, contraseñas, tokens, claves API ni datos reales a prompts o diseños.
- La conexión MCP de Stitch se configura fuera del repositorio. No versionar claves ni añadirlas a los issues.

## Criterios compartidos de terminado

- Página desktop/móvil usable; navegación por teclado, etiquetas, contraste y foco.
- Roles y ámbito conservados; ocultar botones no sustituye autorización de API.
- Fechas de sesión según America/Guayaquil; cantidades nulas no se presentan como cero.
- Acciones peligrosas con confirmación; formularios con errores junto al campo, sin perder los datos ingresados.
- Paginación, filtros y pestañas conservan la implementación existente.
- No ofrecer CSV a cuentas sin permiso ni edición/eliminación de evidencia geográfica.
- Reutilizar componentes compartidos, sin inventar datos o métricas.
- Evidencias sin credenciales/PII. Declaración de asistencia de IA y revisión humana conforme a la constitución.
- Ejecutar lint, typecheck, tests y build de apps/web; e2e pertinentes cuando su entorno esté disponible. Registrar lo realmente ejecutado. No son comprobaciones ejecutadas por redactar este plan.

## Issues preparados

Los nueve cuerpos Markdown en issues/ están listos para publicar; manifest.json identifica título, responsable, orden y dependencias por clave local.
Las claves WEB-01..WEB-09 NO son números de issue de GitHub.

Publicación requiere una conexión GitHub autenticada con permiso para crear issues y asignar a esos usuarios. Se verificó el 2026-11-10 que develop existe, los tres usuarios son asignables (HTTP 204) y no había issues con los marcadores de este plan entre los primeros 100 consultados. La publicación vuelve a recorrer todas las páginas para comprobar duplicados; no atribuir un issue a otra persona si la cuenta no resulta asignable.
Después de publicar, sustituir dependencias locales por enlaces a issues reales y registrar sus URLs en published.json.

Como alternativa al conector GitHub, publicar con una sesión local de GitHub CLI ya autenticada:
```bash
python3 docs/plans/vistas-web/publicar_issues.py
python3 docs/plans/vistas-web/publicar_issues.py --publish
```
La primera orden solo valida; la segunda crea los nueve issues y sus asignaciones. El script admite GH_TOKEN/GITHUB_TOKEN ya configurados en el entorno o gh auth login, nunca secretos escritos en este plan. Si se interrumpe, volver a ejecutar recupera los issues existentes por marcador sin duplicarlos.

## Referencias del proyecto

- apps/web/src/app: rutas actuales.
- apps/web/src/features: componentes funcionales.
- apps/web/AGENTS.md: leer las guías instaladas de Next.js antes de implementar cambios.
- .specify/memory/constitution.md: ramas, atribución, revisión humana y límites.
- specs/007-optimizacion-ux-web: navegación y organización previas.

No se ha implementado ninguna vista ni arrancado servicios al preparar este plan.


## Actualización con el ZIP de Stitch

Se importaron 12 pantallas (7 web, 4 móviles y 1 login diferido), un escudo y DESIGN.md. Ver [catálogo y reglas de adaptación](../../design/stitch/campus-qr/README.md). El HTML se archiva como referencia; no se ejecuta ni integra automáticamente. El contrato prevalece sobre funciones dibujadas no implementadas.

#20–#27 conservan responsables, fases y commits, con referencias visuales verificables. #19 permanece cerrado e intacto. Las pantallas móviles no amplían el alcance web. Si falta una exportación dedicada, se indica la referencia derivada.
