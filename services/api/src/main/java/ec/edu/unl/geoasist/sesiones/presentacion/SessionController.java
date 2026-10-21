package ec.edu.unl.geoasist.sesiones.presentacion;

import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.sesiones.aplicacion.SessionLifecycle;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
public class SessionController {
  private final SessionLifecycle service;

  public SessionController(SessionLifecycle service) {
    this.service = service;
  }

  @PostMapping("/start")
  public ResponseEntity<Map<String, Object>> start(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID sessionId,
      @RequestHeader("Idempotency-Key") UUID key) {
    var receipt = service.change(user, sessionId, key, true);
    return ResponseEntity.status(receipt.status()).body(receipt.body());
  }

  @PostMapping("/close")
  public ResponseEntity<Map<String, Object>> close(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID sessionId,
      @RequestHeader("Idempotency-Key") UUID key) {
    var receipt = service.change(user, sessionId, key, false);
    return ResponseEntity.status(receipt.status()).body(receipt.body());
  }
}
