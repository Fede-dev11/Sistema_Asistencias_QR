package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuditWriter;
import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuthorization;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.asistencia.aplicacion.AttendanceEligibility.Person;
import ec.edu.unl.geoasist.asistencia.persistencia.AttemptRepository;
import ec.edu.unl.geoasist.asistencia.persistencia.GuestStartRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.*;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.sesiones.persistencia.SessionRepository;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolveAttendanceReview {
  public record Command(String decision, String reason) {}

  private record Pending(
      UUID session,
      UUID person,
      UUID guestStart,
      String channel,
      String result,
      String name,
      String deviceHash) {}

  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityRepository activities;
  private final ActivityAuthorization scope;
  private final SessionRepository sessions;
  private final AttendanceLocks locks;
  private final CorrectionWindow window;
  private final AttemptRepository attempts;
  private final GuestStartRepository guestStarts;
  private final IdempotencyService replay;
  private final ActivityAuditWriter audit;

  public ResolveAttendanceReview(
      JdbcTemplate db,
      Clock clock,
      ActivityRepository activities,
      ActivityAuthorization scope,
      SessionRepository sessions,
      AttendanceLocks locks,
      CorrectionWindow window,
      AttemptRepository attempts,
      GuestStartRepository guestStarts,
      IdempotencyService replay,
      ActivityAuditWriter audit) {
    this.db = db;
    this.clock = clock;
    this.activities = activities;
    this.scope = scope;
    this.sessions = sessions;
    this.locks = locks;
    this.window = window;
    this.attempts = attempts;
    this.guestStarts = guestStarts;
    this.replay = replay;
    this.audit = audit;
  }

  @Transactional
  public Receipt resolve(
      UserSession user, UUID attemptId, Command command, UUID key, Object canonicalPayload) {
    Pending initial = find(attemptId);
    UUID activityId = activities.activityOfSession(initial.session());
    scope.write(user, activityId);

    // Orden único: actividad, sesión, persona, inicio invitado e intento.
    locks.activity(activityId, false);
    locks.session(initial.session());
    if (initial.person() != null) locks.person(initial.person());
    if (initial.guestStart() != null) locks.guestStart(initial.guestStart());
    locks.attempt(attemptId);
    scope.write(user, activityId);

    return replay.run(
        "review-attempt",
        user.accountId().toString(),
        key,
        activityId,
        canonicalPayload,
        () -> apply(user, attemptId, command, activityId));
  }

  private Receipt apply(UserSession user, UUID attemptId, Command command, UUID activityId) {
    Pending pending = find(attemptId);
    if (!"PENDIENTE_REVISION".equals(pending.result()) || alreadyReviewed(attemptId))
      throw ApiFailure.conflict();

    Instant reviewedAt = clock.instant();
    UUID attendanceId = null;
    if ("APROBAR".equals(command.decision())) {
      if (pending.person() == null) throw ApiFailure.conflict();
      window.require(sessions.find(pending.session()));
      Person person = eligible(pending, activityId);
      var existing = attempts.existing(pending.session(), pending.person());
      if (existing.isEmpty()) {
        var deviceAttendance = attempts.existingDevice(pending.session(), pending.deviceHash());
        if (deviceAttendance.isPresent()
            && !deviceAttendance.get().person().equals(pending.person()))
          throw AttendanceRecording.deviceConflict();
        boolean confirmed =
            attempts.confirm(
                pending.session(), person, attemptId, reviewedAt, "REVISION", pending.deviceHash());
        if (!confirmed && attempts.existing(pending.session(), pending.person()).isEmpty())
          throw AttendanceRecording.deviceConflict();
        existing = attempts.existing(pending.session(), pending.person());
      }
      attendanceId = existing.orElseThrow(ApiFailure::conflict).id();
      db.update("DELETE FROM ausencias WHERE sesion_id=? AND persona_id=?", pending.session(), pending.person());
      if (pending.guestStart() != null)
        guestStarts.consume(pending.guestStart(), person.id(), attendanceId, reviewedAt);
    }

    db.update(
        "INSERT INTO revisiones_asistencia(intento_id,revisor_id,decision,razon,revisada_en,asistencia_id) VALUES (?,?,?,?,?,?)",
        attemptId,
        user.accountId(),
        command.decision(),
        command.reason().trim(),
        Timestamp.from(reviewedAt),
        attendanceId);

    Map<String, Object> before = Map.of("estado", "PENDIENTE_REVISION");
    Map<String, Object> after = new LinkedHashMap<>();
    after.put("decision", command.decision());
    after.put("estado", "APROBAR".equals(command.decision()) ? "CONFIRMADA" : "RECHAZADA");
    audit.write(
        activityId, user.accountId(), "REVISAR_INTENTO", command.reason().trim(), before, after);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("attempt_id", attemptId);
    body.put("asistencia_id", attendanceId);
    body.put("decision", command.decision());
    body.put("revisada_en", reviewedAt);
    return new Receipt(200, body);
  }

  private Person eligible(Pending pending, UUID activityId) {
    if ("CREDENCIALES".equals(pending.channel())) {
      var rows =
          db.query(
              "SELECT p.id,p.nombres FROM personas p JOIN cuentas c ON c.persona_id=p.id AND c.activa WHERE p.id=?",
              (r, n) -> new Person(r.getObject(1, UUID.class), r.getString(2)),
              pending.person());
      if (rows.isEmpty()) throw ApiFailure.forbidden();
      String mode = activities.find(activityId).mode();
      if ("CLASE".equals(mode)
          && !Boolean.TRUE.equals(
              db.queryForObject(
                  "SELECT EXISTS(SELECT 1 FROM estudiantes_autorizados e JOIN cuentas c ON c.persona_id=e.persona_id JOIN roles_cuenta r ON r.cuenta_id=c.id AND r.rol='ESTUDIANTE' WHERE e.actividad_id=? AND e.persona_id=? AND e.vigente AND c.activa)",
                  Boolean.class,
                  activityId,
                  pending.person()))) throw ApiFailure.forbidden();
      return rows.getFirst();
    }
    if ("INVITADO_LISTADO".equals(pending.channel())) {
      if (!"EVENTO".equals(activities.find(activityId).mode())
          || !Boolean.TRUE.equals(
              db.queryForObject(
                  "SELECT EXISTS(SELECT 1 FROM configuracion_invitados WHERE actividad_id=? AND habilitado)",
                  Boolean.class,
                  activityId))) throw ApiFailure.forbidden();
      if (Boolean.TRUE.equals(
          db.queryForObject(
              "SELECT EXISTS(SELECT 1 FROM cuentas WHERE persona_id=?)",
              Boolean.class,
              pending.person()))) throw ApiFailure.forbidden();
      return db
          .query(
              "SELECT persona_id,nombres FROM invitados_evento WHERE actividad_id=? AND persona_id=? AND activo",
              (r, n) -> new Person(r.getObject(1, UUID.class), r.getString(2)),
              activityId,
              pending.person())
          .stream()
          .findFirst()
          .orElseThrow(ApiFailure::forbidden);
    }
    throw ApiFailure.conflict();
  }

  private Pending find(UUID attemptId) {
    return db
        .query(
            "SELECT sesion_id,persona_id,inicio_invitado_id,canal,resultado,nombres_referencia,dispositivo_hash FROM intentos_asistencia WHERE id=?",
            (r, n) ->
                new Pending(
                    r.getObject(1, UUID.class),
                    r.getObject(2, UUID.class),
                    r.getObject(3, UUID.class),
                    r.getString(4),
                    r.getString(5),
                    r.getString(6),
                    r.getString(7)),
            attemptId)
        .stream()
        .findFirst()
        .orElseThrow(ApiFailure::missing);
  }

  private boolean alreadyReviewed(UUID attemptId) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM revisiones_asistencia WHERE intento_id=?)",
            Boolean.class,
            attemptId));
  }
}
