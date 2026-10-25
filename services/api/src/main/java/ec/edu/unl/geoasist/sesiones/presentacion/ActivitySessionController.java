package ec.edu.unl.geoasist.sesiones.presentacion;

import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.sesiones.aplicacion.ManageScheduledSessions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Sesiones de una actividad; vive en este módulo para que actividades no dependa de sesiones. */
@RestController
@RequestMapping("/api/v1/activities/{activityId}/sessions")
public class ActivitySessionController {
  public record Input(@NotNull Instant inicio, @NotNull Instant fin, UUID espacio_id) {}

  private final ManageScheduledSessions service;

  public ActivitySessionController(ManageScheduledSessions service) {
    this.service = service;
  }

  @GetMapping
  public Map<String, Object> list(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(user, activityId, page, size);
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @Valid @RequestBody Input body) {
    return ResponseEntity.status(201)
        .body(
            service.create(
                user,
                activityId,
                new ManageScheduledSessions.Changes(body.inicio(), body.fin(), body.espacio_id(), null)));
  }
}
