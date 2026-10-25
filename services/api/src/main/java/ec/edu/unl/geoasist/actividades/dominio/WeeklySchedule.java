package ec.edu.unl.geoasist.actividades.dominio;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.time.*;
import java.util.*;

/** Horario semanal recurrente; las horas son locales a {@code zone} y las ventanas salen en UTC. */
public record WeeklySchedule(
    List<Integer> days, LocalTime start, LocalTime end, LocalDate from, LocalDate to, ZoneId zone) {
  public static final int MAX_SESSIONS = 200;
  public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Guayaquil");

  public record Window(Instant start, Instant end) {}

  public WeeklySchedule {
    if (days == null
        || days.isEmpty()
        || new HashSet<>(days).size() != days.size()
        || days.stream().anyMatch(d -> d == null || d < 1 || d > 7)
        || start == null
        || end == null
        || !end.isAfter(start)
        || from == null
        || to == null
        || to.isBefore(from)) throw ApiFailure.invalid();
    days = List.copyOf(days);
    zone = zone == null ? DEFAULT_ZONE : zone;
  }

  public List<Window> expand() {
    List<Window> out = new ArrayList<>();
    for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
      if (!days.contains(d.getDayOfWeek().getValue())) continue;
      if (out.size() == MAX_SESSIONS)
        throw new ApiFailure(
            400, "HORARIO_DEMASIADO_LARGO", "El horario genera más de 200 sesiones.");
      out.add(
          new Window(
              d.atTime(start).atZone(zone).toInstant(), d.atTime(end).atZone(zone).toInstant()));
    }
    return out;
  }
}
