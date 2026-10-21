package ec.edu.unl.geoasist.actividades.presentacion;

import ec.edu.unl.geoasist.actividades.aplicacion.CloseActivity;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activities/{activityId}")
public class ActivityCommandController {
  private final CloseActivity service;

  public ActivityCommandController(CloseActivity service) {
    this.service = service;
  }

  @PostMapping("/close")
  public ResponseEntity<Map<String, Object>> close(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @RequestHeader("Idempotency-Key") UUID key) {
    var receipt = service.close(user, activityId, key);
    return ResponseEntity.status(receipt.status()).body(receipt.body());
  }
}
