package ec.edu.unl.geoasist.reportes.presentacion;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuthorization;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/activities")
public class AuditQueryController {
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final ActivityAuthorization scope;

  public AuditQueryController(JdbcTemplate db, ObjectMapper json, ActivityAuthorization scope) {
    this.db = db;
    this.json = json;
    this.scope = scope;
  }

  @GetMapping("/{activityId}/audit")
  @Transactional(readOnly = true)
  public Map<String, Object> find(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (page < 0 || size < 1 || size > 100) throw ApiFailure.invalid();
    scope.read(user, activityId);
    Long total =
        db.queryForObject(
            "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=?",
            Long.class,
            activityId);
    var items =
        db.query(
            "SELECT id,actividad_id,actor_id,accion,instante,razon,anterior::text,nuevo::text FROM auditoria_actividad WHERE actividad_id=? ORDER BY instante DESC,id LIMIT ? OFFSET ?",
            (row, number) -> {
              Map<String, Object> value = new LinkedHashMap<>();
              value.put("id", row.getObject(1, UUID.class));
              value.put("activity_id", row.getObject(2, UUID.class));
              value.put("responsable_id", row.getObject(3, UUID.class));
              value.put("accion", row.getString(4));
              value.put("ocurrida_en", row.getTimestamp(5).toInstant());
              if (row.getString(6) != null) value.put("razon", row.getString(6));
              value.put("anterior", values(row.getString(7)));
              value.put("nuevo", values(row.getString(8)));
              return value;
            },
            activityId,
            size,
            (long) page * size);
    return Map.of("items", items, "page", page, "size", size, "total", total);
  }

  private Map<String, Object> values(String source) {
    return json.readValue(source, new TypeReference<Map<String, Object>>() {});
  }
}
