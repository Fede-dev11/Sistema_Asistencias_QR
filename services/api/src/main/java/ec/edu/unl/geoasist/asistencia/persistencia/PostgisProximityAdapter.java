package ec.edu.unl.geoasist.asistencia.persistencia;

import ec.edu.unl.geoasist.asistencia.dominio.GeoMeasurement;
import ec.edu.unl.geoasist.asistencia.dominio.ProximityPort;
import ec.edu.unl.geoasist.sesiones.dominio.GeographicCapability;
import java.util.OptionalDouble;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "geoasist.geography-enabled", havingValue = "true")
public class PostgisProximityAdapter implements ProximityPort, GeographicCapability {
  private final JdbcTemplate db;

  public PostgisProximityAdapter(JdbcTemplate db) {
    this.db = db;
  }

  @Override
  public boolean available() {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname='postgis')", Boolean.class));
  }

  @Override
  public boolean configured(UUID session) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM sesiones s JOIN espacios e ON e.id=s.espacio_id WHERE s.id=? AND e.centro IS NOT NULL)",
            Boolean.class,
            session));
  }

  @Override
  public OptionalDouble distanceMeters(UUID session, GeoMeasurement measurement) {
    return db
        .query(
            "SELECT ST_Distance(e.centro,ST_SetSRID(ST_MakePoint(?,?),4326)::geography) FROM sesiones s JOIN espacios e ON e.id=s.espacio_id WHERE s.id=? AND e.centro IS NOT NULL",
            (row, number) -> row.getDouble(1),
            measurement.longitude(),
            measurement.latitude(),
            session)
        .stream()
        .mapToDouble(Double::doubleValue)
        .findFirst();
  }
}
