package ec.edu.unl.geoasist.estadisticas.presentacion;

import ec.edu.unl.geoasist.estadisticas.aplicacion.AttendanceStats;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.time.Instant;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class StatsController {
  private final AttendanceStats service;

  public StatsController(AttendanceStats service) {
    this.service = service;
  }

  @GetMapping("/activities/{id}/stats")
  public Map<String, Object> activity(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.activity(user, id, page, size);
  }

  @GetMapping("/stats/parallels")
  public Map<String, Object> parallels(
      @AuthenticationPrincipal UserSession user,
      @RequestParam(required = false) UUID careerId,
      @RequestParam(required = false) Instant desde,
      @RequestParam(required = false) Instant hasta) {
    return service.parallels(user, careerId, desde, hasta);
  }

  @GetMapping("/stats/careers")
  public Map<String, Object> careers(
      @AuthenticationPrincipal UserSession user,
      @RequestParam(required = false) Instant desde,
      @RequestParam(required = false) Instant hasta) {
    return service.careers(user, desde, hasta);
  }

  @GetMapping("/sessions/{id}/absences")
  public Map<String, Object> absences(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.absences(user, id, page, size);
  }
}
