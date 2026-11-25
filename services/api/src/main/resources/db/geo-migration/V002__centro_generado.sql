ALTER TABLE espacios DROP COLUMN centro;
ALTER TABLE espacios DROP COLUMN radio_recomendado_m;
ALTER TABLE espacios ADD COLUMN centro geography(Point,4326) GENERATED ALWAYS AS (
 CASE WHEN latitud IS NULL THEN NULL ELSE ST_SetSRID(ST_MakePoint(longitud,latitud),4326)::geography END) STORED;
