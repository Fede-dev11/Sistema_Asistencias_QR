# ADR 0005: Mapa web para elegir coordenadas de un espacio

**Fecha**: 2026-10-25. **Estado**: Propuesto; pendiente de revisión.

## Decisión

Usar Leaflet 1.9.4 (versión fija, sin cambios automáticos) con teselas de OpenStreetMap para
que el responsable marque el centro de un espacio en la web. También se aceptan latitud y
longitud escritas a mano, de modo que el mapa es una ayuda y no un requisito.

## Motivo

Leaflet es ligero, sin clave de API y sin costo; OpenStreetMap evita proveedores comerciales.

## Condiciones y riesgos

- Atribución visible "© OpenStreetMap contributors" en el mapa.
- Las teselas se piden al servicio público de OSM, cuya política de uso admite tráfico moderado;
  si crece, usar un servidor de teselas propio o contratado.
- Al cargar teselas el navegador revela su IP y la zona consultada al proveedor; no se envían
  datos personales ni ubicación del usuario.
- La dependencia se añade solo en `apps/web` cuando se implemente la historia correspondiente.

## Alternativas descartadas

Google Maps / Mapbox (clave de API, costo y condiciones comerciales); solo campos numéricos
(poco usable para docentes).
