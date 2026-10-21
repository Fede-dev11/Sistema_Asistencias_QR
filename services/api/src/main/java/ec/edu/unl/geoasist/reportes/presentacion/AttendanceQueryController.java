package ec.edu.unl.geoasist.reportes.presentacion;

import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.reportes.aplicacion.FindAttendances;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
public class AttendanceQueryController {
  private final FindAttendances service;

  public AttendanceQueryController(FindAttendances service) {
    this.service = service;
  }

  @GetMapping("/{sessionId}/attendances")
  public Map<String, Object> find(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID sessionId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String estado) {
    return service.find(user, sessionId, page, size, estado);
  }
}
