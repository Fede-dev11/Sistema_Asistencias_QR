package ec.edu.unl.geoasist.asistencia.persistencia;

import ec.edu.unl.geoasist.asistencia.dominio.GeoMeasurement;
import ec.edu.unl.geoasist.asistencia.dominio.ProximityPort;
import ec.edu.unl.geoasist.sesiones.dominio.GeographicCapability;
import java.util.OptionalDouble;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "geoasist.geography-enabled",
    havingValue = "false",
    matchIfMissing = true)
public class NoGeographyAdapter implements ProximityPort, GeographicCapability {
  @Override
  public boolean available() {
    return false;
  }

  @Override
  public boolean configured(UUID session) {
    return false;
  }

  @Override
  public OptionalDouble distanceMeters(UUID session, GeoMeasurement measurement) {
    return OptionalDouble.empty();
  }
}
