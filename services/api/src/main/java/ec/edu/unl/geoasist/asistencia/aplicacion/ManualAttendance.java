package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuditWriter;
import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuthorization;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.asistencia.aplicacion.AttendanceEligibility.Person;
import ec.edu.unl.geoasist.asistencia.dominio.AttendanceOutcome;
import ec.edu.unl.geoasist.asistencia.persistencia.AttemptRepository;
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

/** FR-005: asistencia manual del responsable; no aplica la regla de dispositivo (no hay dispositivo). */
@Service
public class ManualAttendance {
  public record Command(UUID personId, String reason) {}

  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityRepository activities;
  private final ActivityAuthorization scope;
  private final SessionRepository sessions;
  private final AttendanceLocks locks;
  private final CorrectionWindow window;
  private final AttemptRepository attempts;
  private final IdempotencyService replay;
  private final ActivityAuditWriter audit;

  public ManualAttendance(
      JdbcTemplate db,
      Clock clock,
      ActivityRepository activities,
      ActivityAuthorization scope,
      SessionRepository sessions,
      AttendanceLocks locks,
      CorrectionWindow window,
      AttemptRepository attempts,
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
    this.replay = replay;
    this.audit = audit;
  }

  @Transactional
  public Receipt register(
      UserSession user, UUID sessionId, Command command, UUID key, Object canonicalPayload) {
    UUID activityId = activities.activityOfSession(sessionId);
    scope.write(user, activityId);
    // Orden único de AttendanceLocks: actividad, sesión, persona.
    locks.activity(activityId, false);
    locks.session(sessionId);
    locks.person(command.personId());
    scope.write(user, activityId);
    return replay.run(
        "manual-attendance",
        user.accountId().toString(),
        key,
        activityId,
        canonicalPayload,
        () -> apply(user, sessionId, activityId, command));
  }

  private Receipt apply(UserSession user, UUID sessionId, UUID activityId, Command command) {
    window.require(sessions.find(sessionId));
    UUID personId = command.personId();
    var names =
        db.queryForList(
            "SELECT p.nombres FROM personas p JOIN estudiantes_autorizados e ON e.persona_id=p.id AND e.actividad_id=? AND e.vigente WHERE p.id=?",
            String.class,
            activityId,
            personId);
    if (names.isEmpty())
      throw new ApiFailure(
          422, "NO_AUTORIZADO_EN_SESION", "El estudiante no está autorizado en esta actividad.");
    if (attempts.existing(sessionId, personId).isPresent())
      throw new ApiFailure(
          409, "ASISTENCIA_DUPLICADA", "El estudiante ya tiene asistencia en la sesión.");

    Instant now = clock.instant();
    String reason = command.reason().trim();
    // El pendiente de revisión queda resuelto como rechazado por registro manual (edge case).
    int resolved =
        db.update(
            "INSERT INTO revisiones_asistencia(intento_id,revisor_id,decision,razon,revisada_en) "
                + "SELECT i.id,?,'RECHAZAR','Registro manual',? FROM intentos_asistencia i "
                + "WHERE i.sesion_id=? AND i.persona_id=? AND i.resultado='PENDIENTE_REVISION' "
                + "AND NOT EXISTS(SELECT 1 FROM revisiones_asistencia r WHERE r.intento_id=i.id)",
            user.accountId(),
            Timestamp.from(now),
            sessionId,
            personId);
    Person person = new Person(personId, names.getFirst());
    UUID attempt =
        attempts.record(sessionId, person, AttendanceOutcome.accepted(), null, "MANUAL", now, null);
    UUID attendance = UUID.randomUUID();
    db.update(
        "INSERT INTO asistencias(id,sesion_id,persona_id,intento_id,registrada_en,forma,nombres_registro,razon_manual,registrada_por) VALUES (?,?,?,?,?,'MANUAL',?,?,?)",
        attendance,
        sessionId,
        personId,
        attempt,
        Timestamp.from(now),
        person.name(),
        reason,
        user.accountId());
    int removed =
        db.update("DELETE FROM ausencias WHERE sesion_id=? AND persona_id=?", sessionId, personId);

    audit.write(
        activityId,
        user.accountId(),
        "ASISTENCIA_MANUAL",
        reason,
        Map.of(
            "sesion_id", sessionId, "persona_id", personId, "asistencia", false, "ausente", removed > 0),
        Map.of("asistencia_id", attendance, "forma", "MANUAL", "revision_resuelta", resolved > 0));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("asistencia_id", attendance);
    body.put("attempt_id", attempt);
    body.put("session_id", sessionId);
    body.put("persona_id", personId);
    body.put("forma", "MANUAL");
    body.put("registrada_en", now);
    return new Receipt(201, body);
  }
}
