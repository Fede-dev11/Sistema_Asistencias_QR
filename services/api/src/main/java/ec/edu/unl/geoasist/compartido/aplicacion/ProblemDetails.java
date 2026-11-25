package ec.edu.unl.geoasist.compartido.aplicacion;

import java.util.*;

public final class ProblemDetails {
  private ProblemDetails() {}

  public static Map<String, Object> of(ApiFailure error) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("type", "urn:unl:problem:" + error.code().toLowerCase(Locale.ROOT).replace('_', '-'));
    out.put("title", error.getMessage());
    out.put("status", error.status());
    out.put("code", error.code());
    out.put("mensaje", error.getMessage());
    out.put("correlation_id", RequestCorrelation.id().toString());
    if (error.detail() != null) out.put("detail", error.detail());
    out.putAll(error.extras());
    return out;
  }
}
