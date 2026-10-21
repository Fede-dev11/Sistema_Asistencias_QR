package ec.edu.unl.geoasist.reportes.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuthorization;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.reportes.persistencia.AttendanceReadRepository;
import ec.edu.unl.geoasist.reportes.persistencia.AttendanceReadRepository.Row;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindAttendances {
  private static final Set<String> STATES = Set.of("CONFIRMADA", "PENDIENTE_REVISION", "RECHAZADA");

  private final ActivityRepository activities;
  private final ActivityAuthorization scope;
  private final AttendanceReadRepository rows;

  public FindAttendances(
      ActivityRepository activities, ActivityAuthorization scope, AttendanceReadRepository rows) {
    this.activities = activities;
    this.scope = scope;
    this.rows = rows;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> find(
      UserSession user, UUID session, int page, int size, String state) {
    validate(page, size, state);
    scope.read(user, activities.activityOfSession(session));
    List<Map<String, Object>> items =
        rows.find(session, state, size, (long) page * size).stream()
            .map(FindAttendances::dto)
            .toList();
    return Map.of(
        "items", items,
        "page", page,
        "size", size,
        "total", rows.count(session, state));
  }

  public static void validate(int page, int size, String state) {
    if (page < 0 || size < 1 || size > 100 || (state != null && !STATES.contains(state)))
      throw ApiFailure.invalid();
  }

  public static Map<String, Object> dto(Row row) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("asistencia_id", row.attendance());
    result.put("persona_id", row.person());
    result.put("cedula", row.cedula());
    result.put("nombres", row.names());
    result.put("estado", row.state());
    result.put("registrada_en", row.registered());
    result.put("attempt_id", row.attempt());
    result.put("code", row.code());
    result.put("mensaje", row.message());
    return result;
  }
}
