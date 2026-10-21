package ec.edu.unl.geoasist.actividades.persistencia;

import ec.edu.unl.geoasist.actividades.dominio.Activity;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityRepository {
  private final JdbcTemplate db;

  public ActivityRepository(JdbcTemplate db) {
    this.db = db;
  }

  public Activity find(UUID id) {
    return db
        .query(
            "SELECT * FROM actividades WHERE id=?",
            (r, n) ->
                new Activity(
                    r.getObject("id", UUID.class),
                    r.getString("nombre"),
                    r.getString("modalidad"),
                    r.getObject("responsable_id", UUID.class),
                    r.getString("estado"),
                    r.getTimestamp("cerrada_en") == null
                        ? null
                        : r.getTimestamp("cerrada_en").toInstant(),
                    r.getTimestamp("anonimizar_en") == null
                        ? null
                        : r.getTimestamp("anonimizar_en").toInstant()),
            id)
        .stream()
        .findFirst()
        .orElseThrow(ApiFailure::missing);
  }

  public UUID activityOfSession(UUID session) {
    return db
        .query(
            "SELECT actividad_id FROM sesiones WHERE id=?",
            (r, n) -> r.getObject(1, UUID.class),
            session)
        .stream()
        .findFirst()
        .orElseThrow(ApiFailure::missing);
  }

  public boolean directs(UUID account, UUID activity) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM direcciones_carrera d JOIN actividad_carreras ac ON ac.carrera_id=d.carrera_id WHERE d.cuenta_id=? AND d.vigente AND ac.actividad_id=?)",
            Boolean.class,
            account,
            activity));
  }
}
