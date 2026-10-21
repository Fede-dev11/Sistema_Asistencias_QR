package ec.edu.unl.geoasist.asistencia.dominio;

import java.time.Instant;

public record GeoMeasurement(
    double latitude, double longitude, double precision, Instant measuredAt) {
  @Override
  public String toString() {
    return "GeoMeasurement[ubicación omitida]";
  }
}
