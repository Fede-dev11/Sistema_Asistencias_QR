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
      @RequestParam(required = false) UUID carrera_id,
      @RequestParam(required = false) UUID paralelo_id,
      @RequestParam(required = false) UUID docente_id,
      @RequestParam(required = false) UUID director_id,
      @RequestParam(required = false) String materia,
      @RequestParam(required = false) String estado) {
    return queries.list(
        user,
        page,
        size,
        modalidad,
        carrera_id,
        paralelo_id,
        docente_id,
        director_id,
        materia,
        estado);
  }

  @GetMapping("/{activityId}")
  public Map<String, Object> get(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID activityId) {
    return queries.get(user, activityId);
  }
}
