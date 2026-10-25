package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.asistencia.persistencia.GuestStartRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.*;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.qr.aplicacion.IssueSessionQr;
import ec.edu.unl.geoasist.sesiones.persistencia.SessionRepository;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StartGuestRegistration {
  private final JdbcTemplate db;
  private final Clock clock;
  private final OpaqueSecrets secrets;
  private final IssueSessionQr qr;
  private final ActivityRepository activities;
  private final SessionRepository sessions;
  private final AttendanceLocks locks;
  private final NominalAccessGuard retention;
  private final AttendanceRecording recording;
  private final GuestStartRepository starts;

  public StartGuestRegistration(
      JdbcTemplate db,
      Clock clock,
      OpaqueSecrets secrets,
      IssueSessionQr qr,
      ActivityRepository activities,
      SessionRepository sessions,
      AttendanceLocks locks,
      NominalAccessGuard retention,
      AttendanceRecording recording,
      GuestStartRepository starts) {
    this.db = db;
    this.clock = clock;
    this.secrets = secrets;
    this.qr = qr;
    this.activities = activities;
    this.sessions = sessions;
    this.locks = locks;
    this.retention = retention;
    this.recording = recording;
    this.starts = starts;
  }

  @Transactional
  public Map<String, Object> open(String value, UUID sessionId, Instant received) {
    var token = value == null || sessionId != null ? null : qr.lookup(value);
    UUID session = sessionId != null ? sessionId : token.session();
    UUID activityId = activities.activityOfSession(session);
    locks.activity(activityId, false);
    locks.session(session);
    AttendanceRecording.requireChannel(sessions.method(session), value != null, sessionId != null);
    var activity = activities.find(activityId);
    retention.check(activity.anonymizeAt());
    recording.requireActive(activity, sessions.find(session));
    if (token != null) IssueSessionQr.validate(token, received);
    if (!activity.mode().equals("EVENTO")) throw ApiFailure.forbidden();
    var config =
        db.queryForList(
            "SELECT habilitado,tolerancia_invitado_segundos,acceso_generacion FROM configuracion_invitados WHERE actividad_id=?",
            activityId);
    if (config.isEmpty() || !Boolean.TRUE.equals(config.getFirst().get("habilitado")))
      throw ApiFailure.forbidden();
    var row = config.getFirst();
    int duration = ((Number) row.get("tolerancia_invitado_segundos")).intValue();
    long generation = ((Number) row.get("acceso_generacion")).longValue();
    String secret = secrets.issue();
    Instant now = clock.instant();
    starts.create(
        UUID.randomUUID(), session, OpaqueSecrets.hash(secret), now, duration, generation);
    return Map.of(
        "registro_invitado_token", secret, "inicio_en", now, "vence_en", now.plusSeconds(duration));
  }
}
