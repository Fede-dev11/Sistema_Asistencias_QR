package ec.edu.unl.geoasist.compartido.aplicacion;

import java.util.UUID;

public final class RequestCorrelation {
  private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

  private RequestCorrelation() {}

  public static UUID begin() {
    UUID id = UUID.randomUUID();
    CURRENT.set(id);
    return id;
  }

  public static UUID id() {
    UUID id = CURRENT.get();
    return id == null ? UUID.randomUUID() : id;
  }

  public static void clear() {
    CURRENT.remove();
  }
}
