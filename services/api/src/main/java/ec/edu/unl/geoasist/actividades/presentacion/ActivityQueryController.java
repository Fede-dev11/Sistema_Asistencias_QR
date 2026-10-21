package ec.edu.unl.geoasist.actividades.presentacion;

import ec.edu.unl.geoasist.actividades.aplicacion.FindVisibleActivities;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityQueryController {
  private final FindVisibleActivities queries;

  public ActivityQueryController(FindVisibleActivities queries) {
    this.queries = queries;
  }

  @GetMapping
  public Map<String, Object> list(
      @AuthenticationPrincipal UserSession user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String modalidad,
      @RequestParam(required = false) UUID carrera_id) {
    return queries.list(user, page, size, modalidad, carrera_id);
  }

  @GetMapping("/{activityId}")
  public Map<String, Object> get(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID activityId) {
    return queries.get(user, activityId);
  }
}
