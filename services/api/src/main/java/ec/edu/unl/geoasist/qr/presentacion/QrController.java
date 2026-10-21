package ec.edu.unl.geoasist.qr.presentacion;

import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.qr.aplicacion.IssueSessionQr;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/qr")
public class QrController {
  private final IssueSessionQr service;

  public QrController(IssueSessionQr service) {
    this.service = service;
  }

  @GetMapping
  public Map<String, Object> get(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID sessionId) {
    return service.get(user, sessionId);
  }
}
