package ec.edu.unl.geoasist.asistencia.dominio;

public record AttendanceOutcome(
    Result result, String code, String message, Double distance, Double precision) {
  public enum Result {
    ACEPTADO,
    RECHAZADO,
    REPETIR,
    PENDIENTE_REVISION
  }

  public static AttendanceOutcome accepted() {
    return new AttendanceOutcome(
        Result.ACEPTADO, "ASISTENCIA_CONFIRMADA", "Asistencia guardada.", null, null);
  }

  public static AttendanceOutcome accepted(Double distance, Double precision) {
    return new AttendanceOutcome(
        Result.ACEPTADO, "ASISTENCIA_CONFIRMADA", "Asistencia guardada.", distance, precision);
  }

  public static AttendanceOutcome rejected(String code, String message) {
    return new AttendanceOutcome(Result.RECHAZADO, code, message, null, null);
  }

  public static AttendanceOutcome rejected(
      String code, String message, Double distance, Double precision) {
    return new AttendanceOutcome(Result.RECHAZADO, code, message, distance, precision);
  }

  public static AttendanceOutcome repeat(
      String code, String message, Double distance, Double precision) {
    return new AttendanceOutcome(Result.REPETIR, code, message, distance, precision);
  }

  public static AttendanceOutcome pending(
      String code, String message, Double distance, Double precision) {
    return new AttendanceOutcome(Result.PENDIENTE_REVISION, code, message, distance, precision);
  }

  public static AttendanceOutcome duplicate() {
    return new AttendanceOutcome(
        Result.ACEPTADO, "YA_REGISTRADO", "La asistencia ya estaba registrada.", null, null);
  }
}
