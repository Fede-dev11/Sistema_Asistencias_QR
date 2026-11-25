package ec.edu.unl.geoasist.sesiones.presentacion;

import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.sesiones.aplicacion.ManageScheduledSessions;
import ec.edu.unl.geoasist.sesiones.aplicacion.SessionLifecycle;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
public class SessionController {
  public record Patch(
      java.time.Instant inicio,
      java.time.Instant fin,
      UUID espacio_id,
      String metodo_verificacion) {}

  private final SessionLifecycle service;
  private final ManageScheduledSessions scheduled;

  public SessionController(SessionLifecycle service, ManageScheduledSessions scheduled) {
    this.service = service;
    this.scheduled = scheduled;
  }

  @PatchMapping
  public Map<String, Object> edit(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID sessionId,
      @RequestBody Patch body) {
    return scheduled.edit(
        user,
        sessionId,
        new ManageScheduledSessions.Changes(
            body.inicio(), body.fin(), body.espacio_id(), body.metodo_verificacion()));
  }

  @PostMapping("/cancel")
  public Map<String, Object> cancel(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID sessionId) {
    return scheduled.cancel(user, sessionId);
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
