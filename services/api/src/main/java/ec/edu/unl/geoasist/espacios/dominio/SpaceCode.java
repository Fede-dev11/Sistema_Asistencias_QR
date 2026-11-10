package ec.edu.unl.geoasist.espacios.dominio;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.util.Locale;

/** Código de espacio normalizado: mayúsculas, sin espacios ni guiones, 1 a 40 caracteres. */
public final class SpaceCode {
  private SpaceCode() {}

  public static String normalize(String raw) {
    if (raw == null) throw ApiFailure.invalid();
    String code = raw.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    if (code.isEmpty() || code.length() > 40) throw ApiFailure.invalid();
    return code;
  }
}
