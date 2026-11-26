package ec.edu.unl.geoasist.compartido.aplicacion;

import java.time.Instant;

/** Ventana semiabierta evaluada exclusivamente con un instante del servidor. */
public final class SessionWindow {
  private SessionWindow() {}

  public static boolean contains(Instant start, Instant end, Instant now) {
    return !now.isBefore(start) && now.isBefore(end);
  }

  public static void require(Instant start, Instant end, Instant now) {
    if (!contains(start, end, now))
      throw new ApiFailure(
              409, "FUERA_DE_HORARIO", "La operación está fuera del horario permitido.")
          .with("horario_inicio", start)
          .with("horario_fin", end)
          .with("hora_servidor", now);
  }
}
