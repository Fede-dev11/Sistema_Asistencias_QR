package ec.edu.unl.geoasist.sesiones.persistencia;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRepository {
  public record Session(
      UUID id,
      UUID activity,
      String state,
      Instant windowStart,
      Instant windowEnd,
      Instant started,
      Instant closed) {}

  private final JdbcTemplate db;

  public SessionRepository(JdbcTemplate db) {
    this.db = db;
  }

  public Session find(UUID id) {
    return db
        .query(
            "SELECT * FROM sesiones WHERE id=?",
            (r, n) ->
                new Session(
                    r.getObject("id", UUID.class),
                    r.getObject("actividad_id", UUID.class),
                    r.getString("estado"),
                    r.getTimestamp("ventana_inicio").toInstant(),
                    r.getTimestamp("ventana_fin").toInstant(),
                    r.getTimestamp("iniciada_en") == null
                        ? null
                        : r.getTimestamp("iniciada_en").toInstant(),
                    r.getTimestamp("cerrada_en") == null
                        ? null
                        : r.getTimestamp("cerrada_en").toInstant()),
            id)
        .stream()
        .findFirst()
        .orElseThrow(ApiFailure::missing);
  }

  /**
   * Regla única de inicio: un método con GPS exige geolocalización habilitada (manual y
   * automático).
   */
  public void requireGpsConfigured(UUID id) {
    if (method(id).contains("GPS")
        && !Boolean.TRUE.equals(
            db.queryForObject(
                "SELECT COALESCE((SELECT habilitada FROM configuracion_geografica WHERE sesion_id=?),false)",
                Boolean.class,
                id)))
      throw new ApiFailure(
          409,
          "CONFIGURACION_GPS_REQUERIDA",
          "El método incluye GPS: configura la geolocalización antes de iniciar.");
  }

  public String method(UUID id) {
    return db.queryForObject(
        "SELECT metodo_verificacion FROM sesiones WHERE id=?", String.class, id);
  }

  public static boolean usesQr(String method) {
    return method.equals("SOLO_QR") || method.equals("QR_Y_GPS");
  }

  public static Map<String, Object> dto(Session s) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", s.id());
    result.put("activity_id", s.activity());
    result.put("estado", s.state());
    result.put("iniciada_en", s.started());
    result.put("cerrada_en", s.closed());
    return result;
  }
}
