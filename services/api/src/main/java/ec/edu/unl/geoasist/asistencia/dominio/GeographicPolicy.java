package ec.edu.unl.geoasist.asistencia.dominio;

import java.time.Duration;
import java.time.Instant;

public class GeographicPolicy {
  public enum Mode {
    OFF,
    REVIEW_ONLY,
    AUTO
  }

  public record Config(
      boolean enabled,
      Mode mode,
      double radius,
      double maximumPrecision,
      int maximumAgeSeconds,
      boolean verifiedEvidence) {
    public static Config off() {
      return new Config(false, Mode.OFF, 0, 0, 0, false);
    }

    public static Config reviewOnly(double radius, double precision, int age) {
      return new Config(true, Mode.REVIEW_ONLY, radius, precision, age, false);
    }

    public static Config automatic(
        double radius, double precision, int age, boolean verifiedEvidence) {
      return new Config(true, Mode.AUTO, radius, precision, age, verifiedEvidence);
    }
  }

  public AttendanceOutcome decide(
      Config config,
      GeoMeasurement measurement,
      Double distance,
      int previousUncertainties,
      Instant received) {
    if (!config.enabled()) return AttendanceOutcome.accepted();
    if (measurement == null)
      return uncertain(
          "UBICACION_REQUERIDA",
          "Obtén tu ubicación para continuar.",
          previousUncertainties,
          null,
          null);
    if (!valid(measurement))
      return uncertain(
          "UBICACION_INVALIDA",
          "La medición de ubicación no es válida.",
          previousUncertainties,
          distance,
          measurement.precision());
    if (measurement.measuredAt().isAfter(received.plusSeconds(5))
        || Duration.between(measurement.measuredAt(), received).getSeconds()
            > config.maximumAgeSeconds())
      return uncertain(
          "UBICACION_ANTIGUA",
          "Vuelve a obtener tu ubicación.",
          previousUncertainties,
          distance,
          measurement.precision());
    if (measurement.precision() > config.maximumPrecision())
      return uncertain(
          "PRECISION_INSUFICIENTE",
          "La precisión no es suficiente. Vuelve a medir.",
          previousUncertainties,
          distance,
          measurement.precision());
    if (distance == null || !Double.isFinite(distance) || distance < 0)
      return uncertain(
          "UBICACION_INCIERTA",
          "No se pudo calcular la proximidad.",
          previousUncertainties,
          null,
          measurement.precision());
    if (config.mode() == Mode.REVIEW_ONLY || !config.verifiedEvidence())
      return AttendanceOutcome.pending(
          "REVISION_UBICACION",
          "El encargado revisará el intento.",
          distance,
          measurement.precision());

    double innerEdge = Math.max(0, distance - measurement.precision());
    double outerEdge = distance + measurement.precision();
    if (outerEdge <= config.radius())
      return AttendanceOutcome.accepted(distance, measurement.precision());
    if (innerEdge > config.radius())
      return AttendanceOutcome.rejected(
          "FUERA_DEL_ESPACIO",
          "La medición está fuera del espacio configurado.",
          distance,
          measurement.precision());
    return uncertain(
        "UBICACION_INCIERTA",
        "La precisión cruza el límite del espacio. Vuelve a medir.",
        previousUncertainties,
        distance,
        measurement.precision());
  }

  private static boolean valid(GeoMeasurement value) {
    return Double.isFinite(value.latitude())
        && value.latitude() >= -90
        && value.latitude() <= 90
        && Double.isFinite(value.longitude())
        && value.longitude() >= -180
        && value.longitude() <= 180
        && Double.isFinite(value.precision())
        && value.precision() >= 0
        && value.measuredAt() != null;
  }

  private static AttendanceOutcome uncertain(
      String code, String message, int previous, Double distance, Double precision) {
    return previous >= 1
        ? AttendanceOutcome.pending(code, "El encargado revisará el intento.", distance, precision)
        : AttendanceOutcome.repeat(code, message, distance, precision);
  }
}
