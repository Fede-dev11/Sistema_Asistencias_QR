package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecturas de lo que guardan ReplaceGuestList, ConfigureGuests y ReplaceSchedule. */
@Service
public class ReadActivityConfiguration {
  private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
  private final JdbcTemplate db;
  private final ActivityAuthorization scope;

  public ReadActivityConfiguration(JdbcTemplate db, ActivityAuthorization scope) {
    this.db = db;
    this.scope = scope;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> guestList(UserSession user, UUID id, int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw ApiFailure.invalid();
    scope.read(user, id);
    var items =
        db.queryForList(
            "SELECT g.persona_id,p.cedula_normalizada AS cedula,g.nombres,g.activo FROM invitados_evento g JOIN personas p ON p.id=g.persona_id WHERE g.actividad_id=? ORDER BY p.cedula_normalizada LIMIT ? OFFSET ?",
            id,
            size,
            (long) page * size);
    Long total =
        db.queryForObject(
            "SELECT count(*) FROM invitados_evento WHERE actividad_id=?", Long.class, id);
    return Map.of("items", items, "page", page, "size", size, "total", total);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> guestSettings(UserSession user, UUID id) {
    scope.read(user, id);
    return db
        .queryForList(
            "SELECT habilitado,tolerancia_invitado_segundos FROM configuracion_invitados WHERE actividad_id=?",
            id)
        .stream()
        .findFirst()
        .orElseThrow(ApiFailure::missing);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> schedule(UserSession user, UUID id) {
    scope.read(user, id);
    return db
        .query(
            "SELECT dias,hora_inicio,hora_fin,fecha_desde,fecha_hasta,espacio_id,zona FROM horarios_clase WHERE actividad_id=?",
            (r, n) -> {
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("dias", Arrays.asList((Object[]) r.getArray(1).getArray()));
              m.put("hora_inicio", r.getTime(2).toLocalTime().format(HOUR));
              m.put("hora_fin", r.getTime(3).toLocalTime().format(HOUR));
              m.put("desde", r.getDate(4).toLocalDate().toString());
              m.put("hasta", r.getDate(5).toLocalDate().toString());
              m.put("espacio_id", r.getObject(6, UUID.class));
              m.put("zona", r.getString(7));
              return m;
            },
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ApiFailure(404, "SIN_HORARIO", "La actividad no tiene horario."));
  }
}
