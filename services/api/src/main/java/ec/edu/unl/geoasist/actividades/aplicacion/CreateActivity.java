package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.actividades.dominio.WeeklySchedule;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateActivity {
  public record Schedule(
      List<Integer> dias,
      LocalTime hora_inicio,
      LocalTime hora_fin,
      LocalDate desde,
      LocalDate hasta) {
    public WeeklySchedule weekly() {
      return new WeeklySchedule(dias, hora_inicio, hora_fin, desde, hasta, null);
    }
  }

  /** Cuerpo de {@code POST /activities}; los campos de EVENTO se añaden aquí. */
  public record Command(
      String modalidad,
      String nombre,
      UUID espacio_id,
      String metodo_verificacion,
      List<UUID> paralelo_ids,
      Schedule horario) {}

  private final JdbcTemplate db;
  private final ActivityAuditWriter audit;
  private final SpaceMethodRules rules;
  private final ReplaceSchedule schedules;
  private final FindVisibleActivities queries;
  private final ActivityRepository activities;

  public CreateActivity(
      JdbcTemplate db,
      ActivityAuditWriter audit,
      SpaceMethodRules rules,
      ReplaceSchedule schedules,
      FindVisibleActivities queries,
      ActivityRepository activities) {
    this.db = db;
    this.audit = audit;
    this.rules = rules;
    this.schedules = schedules;
    this.queries = queries;
    this.activities = activities;
  }

  @Transactional
  public Map<String, Object> create(UserSession user, Command c) {
    if (c == null || c.modalidad() == null) throw ApiFailure.invalid();
    return switch (c.modalidad()) {
      case "CLASE" -> createClass(user, c);
      default -> throw ApiFailure.invalid();
    };
  }

  private Map<String, Object> createClass(UserSession user, Command c) {
    user.requireAnyRole("DOCENTE");
    if (c.nombre() == null
        || c.nombre().isBlank()
        || c.nombre().length() > 200
        || c.espacio_id() == null
        || c.paralelo_ids() == null
        || c.paralelo_ids().isEmpty()
        || c.paralelo_ids().stream().anyMatch(Objects::isNull)) throw ApiFailure.invalid();
    var parallels = new ArrayList<>(new LinkedHashSet<>(c.paralelo_ids()));
    var marks = String.join(",", Collections.nCopies(parallels.size(), "?"));
    var args = new ArrayList<Object>();
    args.add(user.accountId());
    args.addAll(parallels);
    Integer assigned =
        db.queryForObject(
            "SELECT count(*) FROM docentes_paralelo WHERE cuenta_id=? AND vigente AND paralelo_id IN ("
                + marks
                + ")",
            Integer.class,
            args.toArray());
    if (assigned == null || assigned != parallels.size())
      throw new ApiFailure(403, "PARALELO_NO_ASIGNADO", "No tienes asignado ese paralelo.");
    String method = c.metodo_verificacion() == null ? "SOLO_QR" : c.metodo_verificacion();
    rules.check(c.espacio_id(), method);
    var schedule = c.horario() == null ? null : c.horario().weekly();
    if (schedule != null) schedule.expand();

    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO actividades(id,nombre,modalidad,responsable_id,espacio_id,metodo_verificacion) VALUES (?,?,'CLASE',?,?,?)",
        id,
        c.nombre().strip(),
        user.accountId(),
        c.espacio_id(),
        method);
    db.update(
        "INSERT INTO actividad_carreras SELECT DISTINCT ?::uuid,carrera_id FROM paralelos WHERE id IN ("
            + marks
            + ")",
        prepend(id, parallels));
    db.update(
        "INSERT INTO actividad_paralelos SELECT ?::uuid,carrera_id,id FROM paralelos WHERE id IN ("
            + marks
            + ")",
        prepend(id, parallels));
    int students =
        db.update(
            "INSERT INTO estudiantes_autorizados SELECT DISTINCT ?::uuid,persona_id,true FROM matriculas WHERE vigente AND paralelo_id IN ("
                + marks
                + ") ON CONFLICT DO NOTHING",
            prepend(id, parallels));
    int sessions = schedule == null ? 0 : schedules.generate(id, schedule, c.espacio_id(), method);
    audit.write(
        id,
        user.accountId(),
        "CREAR_ACTIVIDAD",
        null,
        Map.of(),
        Map.of(
            "modalidad",
            "CLASE",
            "paralelos",
            parallels.size(),
            "estudiantes",
            students,
            "sesiones",
            sessions));
    var out = new LinkedHashMap<>(queries.dto(activities.find(id)));
    out.put("espacio_id", c.espacio_id());
    out.put("metodo_verificacion", method);
    out.put("estudiantes_autorizados", students);
    out.put(
        "sesiones",
        db.query(
            "SELECT id,estado,ventana_inicio,ventana_fin FROM sesiones WHERE actividad_id=? ORDER BY ventana_inicio",
            (r, n) ->
                Map.of(
                    "id", r.getObject("id", UUID.class),
                    "estado", r.getString("estado"),
                    "inicio", r.getTimestamp("ventana_inicio").toInstant(),
                    "fin", r.getTimestamp("ventana_fin").toInstant()),
            id));
    return out;
  }

  private static Object[] prepend(UUID first, List<UUID> rest) {
    var all = new ArrayList<Object>();
    all.add(first);
    all.addAll(rest);
    return all.toArray();
  }
}
