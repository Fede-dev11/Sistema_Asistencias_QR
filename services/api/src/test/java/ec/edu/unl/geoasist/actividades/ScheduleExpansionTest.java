package ec.edu.unl.geoasist.actividades;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.actividades.dominio.WeeklySchedule;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleExpansionTest {
  private static WeeklySchedule of(List<Integer> days, String from, String to, String a, String b) {
    return new WeeklySchedule(
        days,
        LocalTime.parse(a),
        LocalTime.parse(b),
        LocalDate.parse(from),
        LocalDate.parse(to),
        null);
  }

  @Test
  void mondaysAndWednesdaysIncludeBothEndsAndConvertToUtc() {
    var windows = of(List.of(1, 3), "2026-11-02", "2026-11-27", "08:00", "10:00").expand();
    assertEquals(8, windows.size());
    assertEquals(Instant.parse("2026-11-02T13:00:00Z"), windows.getFirst().start());
    assertEquals(Instant.parse("2026-11-02T15:00:00Z"), windows.getFirst().end());
    // hasta es inclusivo: viernes 27 entra aunque sea el último día
    assertEquals(1, of(List.of(5), "2026-11-27", "2026-11-27", "08:00", "10:00").expand().size());
  }

  @Test
  void crossesMonthBoundary() {
    var windows = of(List.of(6, 7), "2026-10-30", "2026-11-02", "09:00", "10:00").expand();
    assertEquals(
        List.of(Instant.parse("2026-10-31T14:00:00Z"), Instant.parse("2026-11-01T14:00:00Z")),
        windows.stream().map(WeeklySchedule.Window::start).toList());
  }

  @Test
  void rejectsInvalidShapes() {
    assertThrows(
        ApiFailure.class, () -> of(List.of(1), "2026-11-02", "2026-11-03", "10:00", "10:00"));
    assertThrows(
        ApiFailure.class, () -> of(List.of(1, 1), "2026-11-02", "2026-11-03", "08:00", "10:00"));
    assertThrows(
        ApiFailure.class, () -> of(List.of(8), "2026-11-02", "2026-11-03", "08:00", "10:00"));
    assertThrows(
        ApiFailure.class, () -> of(List.of(), "2026-11-02", "2026-11-03", "08:00", "10:00"));
    assertThrows(
        ApiFailure.class, () -> of(List.of(1), "2026-11-03", "2026-11-02", "08:00", "10:00"));
  }

  @Test
  void limitsToTwoHundredSessions() {
    var days = List.of(1, 2, 3, 4, 5, 6, 7);
    assertEquals(200, of(days, "2026-01-01", "2026-07-19", "08:00", "09:00").expand().size());
    var e =
        assertThrows(
            ApiFailure.class,
            () -> of(days, "2026-01-01", "2026-07-20", "08:00", "09:00").expand());
    assertEquals("HORARIO_DEMASIADO_LARGO", e.code());
  }
}
