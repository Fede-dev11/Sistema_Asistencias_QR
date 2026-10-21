-- La proyección nominal pagina confirmaciones por sesión y fecha; la unicidad existente cubre
-- sesión/persona y este índice evita ordenar todas las confirmaciones de una sesión.
CREATE INDEX asistencias_sesion_registrada_idx
  ON asistencias(sesion_id,registrada_en DESC,persona_id);

-- auditoria_actividad_instante_idx y intentos_sesion_persona_orden_idx ya cubren las otras dos
-- consultas de US4; no se duplican ni se añade GiST sin un plan geográfico que lo justifique.
