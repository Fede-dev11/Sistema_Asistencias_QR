package ec.edu.unl.geoasist.estadisticas.presentacion;

import ec.edu.unl.geoasist.estadisticas.aplicacion.AttendanceStats;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class MyHistoryController {
  private final AttendanceStats service;

  public MyHistoryController(AttendanceStats service) {
    this.service = service;
  }

  @GetMapping("/attendances")
  public Map<String, Object> attendances(
      @AuthenticationPrincipal UserSession user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.myHistory(user, page, size);
  }

  @GetMapping("/stats")
  public Map<String, Object> stats(@AuthenticationPrincipal UserSession user) {
    return service.myStats(user);
  }
}
