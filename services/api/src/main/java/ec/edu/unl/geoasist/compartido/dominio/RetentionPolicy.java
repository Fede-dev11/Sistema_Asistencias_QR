package ec.edu.unl.geoasist.compartido.dominio;

import java.time.*;

public final class RetentionPolicy {
  private RetentionPolicy() {}

  public static Instant expires(Instant closed) {
    return closed.atZone(ZoneOffset.UTC).plusYears(5).toInstant();
  }

  public static boolean expired(Instant deadline, Instant now) {
    return deadline != null && !now.isBefore(deadline);
  }
}
