CREATE EXTENSION IF NOT EXISTS postgis;
ALTER TABLE espacios ADD COLUMN centro geography(Point,4326);
ALTER TABLE espacios ADD COLUMN radio_recomendado_m double precision CHECK(radio_recomendado_m>0);
