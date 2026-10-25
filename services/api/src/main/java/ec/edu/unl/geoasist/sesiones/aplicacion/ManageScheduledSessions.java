package ec.edu.unl.geoasist.sesiones.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.*;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageScheduledSessions {
  /** Campos nulos no cambian. */
  public record Changes(
      Instant inicio, Instant fin, UUID espacio_id, String metodo_verificacion) {}

  private final JdbcTemplate db;
  private final ActivityRepository activities;
  private final ActivityAuthorization scope;
  private final AttendanceLocks locks;
  private final ActivityAuditWriter audit;
  private final SpaceMethodRules rules;
  private final IdempotencyService replay;

  public ManageScheduledSessions(
      JdbcTemplate db,
      ActivityRepository activities,
      ActivityAuthorization scope,
      AttendanceLocks locks,
      ActivityAuditWriter audit,
      SpaceMethodRules rules,
      IdempotencyService replay) {
    this.db = db;
    this.activities = activities;
    this.scope = scope;
    this.locks = locks;
    this.audit = audit;
    this.rules = rules;
    this.replay = replay;
  }

  private static final String COLUMNS =
      "id,actividad_id,estado,ventana_inicio,ventana_fin,espacio_id,metodo_verificacion,horario_id";

  private static Map<String, Object> row(java.sql.ResultSet r, int n) throws java.sql.SQLException {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", r.getObject("id", UUID.class));
    m.put("activity_id", r.getObject("actividad_id", UUID.class));
    m.put("estado", r.getString("estado"));
    m.put("inicio", r.getTimestamp("ventana_inicio").toInstant());
    m.put("fin", r.getTimestamp("ventana_fin").toInstant());
    m.put("espacio_id", r.getObject("espacio_id", UUID.class));
    m.put("metodo_verificacion", r.getString("metodo_verificacion"));
    m.put("horario_id", r.getObject("horario_id", UUID.class));
    return m;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> list(UserSession user, UUID activity, int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw ApiFailure.invalid();
    scope.read(user, activity);
    Long total =
        db.queryForObject(
            "SELECT count(*) FROM sesiones WHERE actividad_id=?", Long.class, activity);
    var items =
        db.query(
            "SELECT "
                + COLUMNS
                + " FROM sesiones WHERE actividad_id=? ORDER BY ventana_inicio,id LIMIT ? OFFSET ?",
            ManageScheduledSessions::row,
            activity,
            size,
            (long) page * size);
    return Map.of("items", items, "page", page, "size", size, "total", total);
  }

  @Transactional
  public Receipt create(UserSession user, UUID activityId, UUID key, Changes c) {
    return replay.run("create-activity-session", user.accountId().toString(), key, activityId, c,
        () -> new Receipt(201, createOnce(user, activityId, c)));
  }

  private Map<String, Object> createOnce(UserSession user, UUID activityId, Changes c) {
    scope.write(user, activityId);
    locks.activity(activityId, true);
    var activity = scope.write(user, activityId);
    if (!activity.state().equals("ABIERTA")) throw ApiFailure.conflict();
    if (c.inicio() == null || c.fin() == null || !c.fin().isAfter(c.inicio()))
      throw ApiFailure.invalid();
    var defaults =
        db.queryForMap(
            "SELECT espacio_id,metodo_verificacion FROM actividades WHERE id=?", activityId);
    UUID space = c.espacio_id() == null ? (UUID) defaults.get("espacio_id") : c.espacio_id();
    String method = (String) defaults.get("metodo_verificacion");
    rules.check(space, method);
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO sesiones(id,actividad_id,espacio_id,ventana_inicio,ventana_fin,metodo_verificacion) VALUES (?,?,?,?,?,?)",
        id,
        activityId,
        space,
        Timestamp.from(c.inicio()),
        Timestamp.from(c.fin()),
        method);
    audit.write(
        activityId,
        user.accountId(),
        "CREAR_SESION",
        null,
        Map.of(),
        Map.of("sesion_id", id, "inicio", c.inicio().toString(), "fin", c.fin().toString()));
    return find(id);
  }

  @Transactional
  public Map<String, Object> edit(UserSession user, UUID session, Changes c) {
    var row = lockScheduled(user, session);
    Instant start = c.inicio() == null ? (Instant) row.get("inicio") : c.inicio();
    Instant end = c.fin() == null ? (Instant) row.get("fin") : c.fin();
    UUID space = c.espacio_id() == null ? (UUID) row.get("espacio_id") : c.espacio_id();
    if (!end.isAfter(start)) throw ApiFailure.invalid();
    String before = (String) row.get("metodo_verificacion");
    String method = c.metodo_verificacion() == null ? before : c.metodo_verificacion();
    rules.check(space, method);
    db.update(
        "UPDATE sesiones SET ventana_inicio=?,ventana_fin=?,espacio_id=?,metodo_verificacion=? WHERE id=?",
        Timestamp.from(start),
        Timestamp.from(end),
        space,
        method,
        session);
    if (!method.equals(before) && !method.contains("GPS"))
      db.update(
          "UPDATE configuracion_geografica SET habilitada=false,radio_m=NULL,precision_max_m=NULL,antiguedad_max_s=NULL,modo='OFF',evidencia_id=NULL WHERE sesion_id=?",
          session);
    audit.write(
        (UUID) row.get("activity_id"),
        user.accountId(),
        "EDITAR_SESION",
        null,
        Map.of(
            "sesion_id",
            session,
            "inicio",
            row.get("inicio").toString(),
            "fin",
            row.get("fin").toString(),
            "metodo_verificacion",
            before),
        Map.of(
            "sesion_id",
            session,
            "inicio",
            start.toString(),
            "fin",
            end.toString(),
            "metodo_verificacion",
            method));
    return find(session);
  }

  @Transactional
  public Map<String, Object> cancel(UserSession user, UUID session) {
    var row = lockScheduled(user, session);
    db.update("UPDATE sesiones SET estado='CANCELADA' WHERE id=?", session);
    audit.write(
        (UUID) row.get("activity_id"),
        user.accountId(),
        "CANCELAR_SESION",
        null,
        Map.of("sesion_id", session, "estado", "PROGRAMADA"),
        Map.of("sesion_id", session, "estado", "CANCELADA"));
    return find(session);
  }

  private Map<String, Object> lockScheduled(UserSession user, UUID session) {
    UUID id = activities.activityOfSession(session);
    scope.write(user, id);
    locks.activity(id, false);
    locks.session(session);
    var activity = scope.write(user, id);
    var row = find(session);
    if (!activity.state().equals("ABIERTA") || !row.get("estado").equals("PROGRAMADA"))
      throw new ApiFailure(409, "SESION_NO_EDITABLE", "La sesión ya inició o fue cancelada.");
    return row;
  }

  private Map<String, Object> find(UUID session) {
    return db
        .query(
            "SELECT " + COLUMNS + " FROM sesiones WHERE id=?",
            ManageScheduledSessions::row,
            session)
        .stream()
        .findFirst()
        .orElseThrow(ApiFailure::missing);
  }
}
