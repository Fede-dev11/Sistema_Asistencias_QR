package ec.edu.unl.geoasist.compartido.dominio;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;

/** Cédula ecuatoriana validada con módulo 10; única regla de validación del sistema. */
public record Cedula(String value) {
  public static Cedula of(String raw) {
    String v = raw == null ? "" : raw.strip();
    if (!v.matches("[0-9]{10}")) throw invalid();
    int province = Integer.parseInt(v.substring(0, 2));
    if ((province < 1 || province > 24) && province != 30) throw invalid();
    if (v.charAt(2) >= '6') throw invalid();
    int sum = 0;
    for (int i = 0; i < 9; i++) {
      int d = (v.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
      sum += d > 9 ? d - 9 : d;
    }
    if ((10 - sum % 10) % 10 != v.charAt(9) - '0') throw invalid();
    return new Cedula(v);
  }

  private static ApiFailure invalid() {
    return new ApiFailure(400, "CEDULA_INVALIDA", "La cédula no es válida.");
  }
}
