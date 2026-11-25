package ec.edu.unl.geoasist.sesiones.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuthorization;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura de lo que guarda ConfigureSessionGeolocation; sin fila devuelve el estado por defecto.
 */
@Service
public class ReadSessionGeolocation {
  private final JdbcTemplate db;
  private final ActivityRepository activities;
  private final ActivityAuthorization scope;

  public ReadSessionGeolocation(
      JdbcTemplate db, ActivityRepository activities, ActivityAuthorization scope) {
    this.db = db;
    this.activities = activities;
    this.scope = scope;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> read(UserSession user, UUID sessionId) {
    scope.read(user, activities.activityOfSession(sessionId));
    Map<String, Object> m = new LinkedHashMap<>();
    var row =
        db
            .queryForList(
                "SELECT habilitada,radio_m,precision_max_m,antiguedad_max_s,modo,evidencia_id FROM configuracion_geografica WHERE sesion_id=?",
                sessionId)
            .stream()
            .findFirst()
            .orElse(null);
    m.put("habilitada", row != null && (Boolean) row.get("habilitada"));
    m.put("radio_m", row == null ? null : row.get("radio_m"));
    m.put("precision_max_m", row == null ? null : row.get("precision_max_m"));
    m.put("antiguedad_max_s", row == null ? null : row.get("antiguedad_max_s"));
    m.put("modo", row == null ? "OFF" : row.get("modo"));
    m.put("evidencia_id", row == null ? null : row.get("evidencia_id"));
    m.put(
        "evidencia_vigente",
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM sesiones s JOIN evidencias_geograficas e ON e.espacio_id=s.espacio_id WHERE s.id=?)",
            Boolean.class,
            sessionId));
    return m;
  }
}
