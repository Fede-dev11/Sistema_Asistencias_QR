-- V010 permitía eventos con acceso NULL (un CHECK con NULL no falla); se cierran como CERRADO.
UPDATE actividades SET acceso='CERRADO' WHERE modalidad='EVENTO' AND acceso IS NULL;
ALTER TABLE actividades ADD CONSTRAINT actividades_acceso_evento_nn CHECK(modalidad<>'EVENTO' OR acceso IS NOT NULL);
