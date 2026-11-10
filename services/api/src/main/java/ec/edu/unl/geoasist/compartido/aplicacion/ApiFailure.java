package ec.edu.unl.geoasist.compartido.aplicacion;

public class ApiFailure extends RuntimeException {
  private final int status;
  private final String code;
  private final String detail;
  private final java.util.Map<String, Object> extras = new java.util.LinkedHashMap<>();

  public ApiFailure(int status, String code, String message) {
    this(status, code, message, null);
  }

  public ApiFailure(int status, String code, String message, String detail) {
    super(message);
    this.status = status;
    this.code = code;
    this.detail = detail;
  }

  public ApiFailure with(String key, Object value) {
    extras.put(key, value);
    return this;
  }

  public java.util.Map<String, Object> extras() {
    return extras;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public String detail() {
    return detail;
  }

  public static ApiFailure forbidden() {
    return new ApiFailure(403, "NO_AUTORIZADO", "No tienes autorización para esta operación.");
  }

  public static ApiFailure missing() {
    return new ApiFailure(404, "RECURSO_NO_ENCONTRADO", "Recurso inexistente.");
  }

  public static ApiFailure unauthenticated() {
    return new ApiFailure(401, "NO_AUTENTICADO", "Accede con credenciales vigentes.");
  }

  public static ApiFailure invalid() {
    return new ApiFailure(400, "SOLICITUD_INVALIDA", "Revisa los datos enviados.");
  }

  public static ApiFailure conflict() {
    return new ApiFailure(409, "ESTADO_INCOMPATIBLE", "El estado actual impide esta operación.");
  }
}
