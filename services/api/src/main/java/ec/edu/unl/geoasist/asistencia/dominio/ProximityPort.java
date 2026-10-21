package ec.edu.unl.geoasist.asistencia.dominio;

import java.util.OptionalDouble;
import java.util.UUID;

public interface ProximityPort {
  boolean available();

  boolean configured(UUID session);

  OptionalDouble distanceMeters(UUID session, GeoMeasurement measurement);
}
