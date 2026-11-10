package ec.edu.unl.geoasist.asistencia.presentacion;

import ec.edu.unl.geoasist.asistencia.aplicacion.ListActiveSessions;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/active-sessions")
public class ActiveSessionsController {
  private final ListActiveSessions service;

  public ActiveSessionsController(ListActiveSessions service) {
    this.service = service;
  }

  @GetMapping
  public Map<String, Object> list(
      @AuthenticationPrincipal UserSession user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return service.list(user, page, size);
  }
}
