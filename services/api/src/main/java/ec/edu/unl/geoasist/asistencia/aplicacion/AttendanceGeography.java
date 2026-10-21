package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.asistencia.dominio.*;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.time.Instant;
import java.util.OptionalDouble;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AttendanceGeography {
  private final JdbcTemplate db;
  private final ProximityPort proximity;
  private final GeographicPolicy policy = new GeographicPolicy();

  public AttendanceGeography(JdbcTemplate db, ProximityPort proximity) {
    this.db = db;
    this.proximity = proximity;
  }

  public void validatePayload(UUID session, GeoMeasurement location) {
    if (!configuration(session).enabled() && location != null)
      throw new ApiFailure(400, "UBICACION_NO_REQUERIDA", "Esta sesión no solicita ubicación.");
  }

  public AttendanceOutcome evaluate(
      UUID session, GeoMeasurement location, int previousUncertainties, Instant received) {
    var config = configuration(session);
    if (!config.enabled()) {
      validatePayload(session, location);
      return AttendanceOutcome.accepted();
    }
    Double distance = null;
    if (location != null
        && finiteCoordinates(location)
        && proximity.available()
        && proximity.configured(session)) {
      OptionalDouble measured = proximity.distanceMeters(session, location);
      if (measured.isPresent()) distance = measured.getAsDouble();
    }
    return policy.decide(config, location, distance, previousUncertainties, received);
  }

  private GeographicPolicy.Config configuration(UUID session) {
    return db
        .query(
            "SELECT habilitada,modo,radio_m,precision_max_m,antiguedad_max_s,evidencia_id IS NOT NULL FROM configuracion_geografica WHERE sesion_id=?",
            (row, number) -> {
              if (!row.getBoolean(1)) return GeographicPolicy.Config.off();
              var mode = GeographicPolicy.Mode.valueOf(row.getString(2));
              return new GeographicPolicy.Config(
                  true, mode, row.getDouble(3), row.getDouble(4), row.getInt(5), row.getBoolean(6));
            },
            session)
        .stream()
        .findFirst()
        .orElseGet(GeographicPolicy.Config::off);
  }

  private static boolean finiteCoordinates(GeoMeasurement value) {
    return Double.isFinite(value.latitude())
        && value.latitude() >= -90
        && value.latitude() <= 90
        && Double.isFinite(value.longitude())
        && value.longitude() >= -180
        && value.longitude() <= 180;
  }
}
