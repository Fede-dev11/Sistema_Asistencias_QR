package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuditWriter;
import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuthorization;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.*;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.sesiones.persistencia.SessionRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-006..FR-008: justificación por el responsable, solicitud del estudiante y su decisión. */
@Service
public class Justifications {
  private static final Set<String> STATES = Set.of("PENDIENTE", "APROBADA", "RECHAZADA");

  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityRepository activities;
  private final ActivityAuthorization scope;
  private final SessionRepository sessions;
  private final AttendanceLocks locks;
  private final CorrectionWindow window;
  private final IdempotencyService replay;
  private final ActivityAuditWriter audit;

  public Justifications(
      JdbcTemplate db,
      Clock clock,
      ActivityRepository activities,
      ActivityAuthorization scope,
      SessionRepository sessions,
      AttendanceLocks locks,
      CorrectionWindow window,
      IdempotencyService replay,
      ActivityAuditWriter audit) {
    this.db = db;
    this.clock = clock;
    this.activities = activities;
    this.scope = scope;
    this.sessions = sessions;
    this.locks = locks;
    this.window = window;
    this.replay = replay;
    this.audit = audit;
  }

  private void lock(UUID activity, UUID session, UUID person) {
    locks.activity(activity, false);
    locks.session(session);
    locks.person(person);
  }

  /** Responsable justifica directamente: queda APROBADA de origen RESPONSABLE. */
  @Transactional
  public Receipt justify(
      UserSession user, UUID sessionId, UUID personId, String reason, UUID key, Object payload) {
    UUID activityId = activities.activityOfSession(sessionId);
    scope.write(user, activityId);
    lock(activityId, sessionId, personId);
    scope.write(user, activityId);
    return replay.run(
        "justify-absence:" + personId,
        user.accountId().toString(),
        key,
        activityId,
        payload,
        () -> {
          window.require(sessions.find(sessionId));
          requireAbsent(sessionId, personId);
          if (pending(sessionId, personId))
            throw new ApiFailure(
                409,
                "JUSTIFICACION_PENDIENTE_EXISTE",
                "La ausencia tiene una solicitud pendiente: decídala.");
          Instant now = clock.instant();
          UUID id = UUID.randomUUID();
          db.update(
              "INSERT INTO justificaciones(id,sesion_id,persona_id,origen,motivo,estado,decidida_por,razon_decision,solicitada_en,decidida_en) VALUES (?,?,?,'RESPONSABLE',?,'APROBADA',?,?,?,?)",
              id,
              sessionId,
              personId,
              reason.trim(),
              user.accountId(),
              reason.trim(),
              Timestamp.from(now),
              Timestamp.from(now));
          markJustified(sessionId, personId);
          audit.write(
              activityId,
              user.accountId(),
              "JUSTIFICAR_AUSENCIA",
              reason.trim(),
              Map.of("sesion_id", sessionId, "persona_id", personId, "estado", "AUSENTE"),
              Map.of("justificacion_id", id, "estado", "JUSTIFICADA"));
          return new Receipt(201, summary(id, sessionId, personId, "APROBADA", now));
        });
  }

  /** Estudiante solicita justificar su propia ausencia, dentro del mismo plazo. */
  @Transactional
  public Receipt request(UserSession user, UUID sessionId, String motive, UUID key, Object payload) {
    UUID personId = user.personId();
    UUID activityId = activities.activityOfSession(sessionId);
    lock(activityId, sessionId, personId);
    return replay.run(
        "justification-request:" + sessionId,
        user.accountId().toString(),
        key,
        activityId,
        payload,
        () -> {
          window.require(sessions.find(sessionId));
          requireAbsent(sessionId, personId);
          if (pending(sessionId, personId))
            throw new ApiFailure(
                409, "JUSTIFICACION_PENDIENTE_EXISTE", "Ya existe una solicitud pendiente.");
          Instant now = clock.instant();
          UUID id = UUID.randomUUID();
          db.update(
              "INSERT INTO justificaciones(id,sesion_id,persona_id,origen,motivo,estado,solicitada_en) VALUES (?,?,?,'ESTUDIANTE',?,'PENDIENTE',?)",
              id,
              sessionId,
              personId,
              motive.trim(),
              Timestamp.from(now));
          // El motivo puede contener datos de salud: no se copia a la auditoría.
          audit.write(
              activityId,
              user.accountId(),
              "SOLICITAR_JUSTIFICACION",
              null,
              Map.of("sesion_id", sessionId, "persona_id", personId),
              Map.of("justificacion_id", id, "estado", "PENDIENTE"));
          return new Receipt(201, summary(id, sessionId, personId, "PENDIENTE", now));
        });
  }

  private record Row(UUID session, UUID person, UUID activity) {}

  private Row row(UUID id) {
    return db.query(
            "SELECT j.sesion_id,j.persona_id,s.actividad_id FROM justificaciones j JOIN sesiones s ON s.id=j.sesion_id WHERE j.id=?",
            (r, n) ->
                new Row(
                    r.getObject(1, UUID.class),
                    r.getObject(2, UUID.class),
                    r.getObject(3, UUID.class)),
            id)
        .stream()
        .findFirst()
        .orElseThrow(ApiFailure::missing);
  }

  @Transactional
  public Receipt decide(
      UserSession user, UUID id, String decision, String reason, UUID key, Object payload) {
    Row found = row(id);
    scope.write(user, found.activity());
    lock(found.activity(), found.session(), found.person());
    scope.write(user, found.activity());
    return replay.run(
        "justification-decision",
        user.accountId().toString(),
        key,
        found.activity(),
        payload,
        () -> {
          boolean approve = "APROBAR".equals(decision);
          String state = approve ? "APROBADA" : "RECHAZADA";
          Instant now = clock.instant();
          // El filtro PENDIENTE y el trigger de inmutabilidad hacen única la decisión.
          int updated =
              db.update(
                  "UPDATE justificaciones SET estado=?,decidida_por=?,razon_decision=?,decidida_en=? WHERE id=? AND estado='PENDIENTE'",
                  state,
                  user.accountId(),
                  reason.trim(),
                  Timestamp.from(now),
                  id);
          if (updated == 0) throw ApiFailure.conflict();
          if (approve) markJustified(found.session(), found.person());
          audit.write(
              found.activity(),
              user.accountId(),
              "DECIDIR_JUSTIFICACION",
              reason.trim(),
              Map.of("justificacion_id", id, "estado", "PENDIENTE"),
              Map.of("estado", state));
          return new Receipt(200, summary(id, found.session(), found.person(), state, now));
        });
  }

  @Transactional(readOnly = true)
  public Map<String, Object> list(
      UserSession user, UUID activityId, String state, int page, int size) {
    if (page < 0 || size < 1 || size > 100 || (state != null && !STATES.contains(state)))
      throw ApiFailure.invalid();
    scope.read(user, activityId);
    String from =
        " FROM justificaciones j JOIN sesiones s ON s.id=j.sesion_id JOIN personas p ON p.id=j.persona_id WHERE s.actividad_id=? AND (CAST(? AS varchar) IS NULL OR j.estado=?)";
    var items =
        db.query(
            "SELECT j.id,j.sesion_id,j.persona_id,p.cedula_normalizada,p.nombres,j.origen,j.motivo,j.estado,j.razon_decision,j.solicitada_en,j.decidida_en"
                + from
                + " ORDER BY j.solicitada_en DESC,j.id LIMIT ? OFFSET ?",
            (r, n) -> {
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("id", r.getObject(1, UUID.class));
              m.put("sesion_id", r.getObject(2, UUID.class));
              m.put("persona_id", r.getObject(3, UUID.class));
              m.put("cedula", r.getString(4));
              m.put("nombres", r.getString(5));
              m.put("origen", r.getString(6));
              m.put("motivo", r.getString(7));
              m.put("estado", r.getString(8));
              m.put("razon_decision", r.getString(9));
              m.put("solicitada_en", r.getTimestamp(10).toInstant());
              m.put(
                  "decidida_en", r.getTimestamp(11) == null ? null : r.getTimestamp(11).toInstant());
              return m;
            },
            activityId,
            state,
            state,
            size,
            (long) page * size);
    Long total = db.queryForObject("SELECT count(*)" + from, Long.class, activityId, state, state);
    return Map.of("items", items, "page", page, "size", size, "total", total);
  }

  private void requireAbsent(UUID session, UUID person) {
    var states =
        db.queryForList(
            "SELECT estado FROM ausencias WHERE sesion_id=? AND persona_id=?",
            String.class,
            session,
            person);
    if (states.isEmpty())
      throw new ApiFailure(409, "SIN_AUSENCIA", "No existe una ausencia que justificar.");
    if (!"AUSENTE".equals(states.getFirst())) throw ApiFailure.conflict();
  }

  private boolean pending(UUID session, UUID person) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM justificaciones WHERE sesion_id=? AND persona_id=? AND estado='PENDIENTE')",
            Boolean.class,
            session,
            person));
  }

  private void markJustified(UUID session, UUID person) {
    db.update(
        "UPDATE ausencias SET estado='JUSTIFICADA' WHERE sesion_id=? AND persona_id=?",
        session,
        person);
  }

  private static Map<String, Object> summary(
      UUID id, UUID session, UUID person, String state, Instant at) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", id);
    body.put("sesion_id", session);
    body.put("persona_id", person);
    body.put("estado", state);
    body.put("instante", at);
    return body;
  }
}
