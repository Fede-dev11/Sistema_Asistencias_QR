package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.asistencia.dominio.GeoMeasurement;
import ec.edu.unl.geoasist.compartido.aplicacion.*;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.qr.aplicacion.IssueSessionQr;
import ec.edu.unl.geoasist.sesiones.persistencia.SessionRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordAuthenticatedAttendance {
  private final IssueSessionQr qr;
  private final ActivityRepository activities;
  private final SessionRepository sessions;
  private final AttendanceLocks locks;
  private final AttendanceEligibility eligible;
  private final NominalAccessGuard retention;
  private final AttendanceRecording recording;
  private final IdempotencyService replay;

  public RecordAuthenticatedAttendance(
      IssueSessionQr qr,
      ActivityRepository activities,
      SessionRepository sessions,
      AttendanceLocks locks,
      AttendanceEligibility eligible,
      NominalAccessGuard retention,
      AttendanceRecording recording,
      IdempotencyService replay) {
    this.qr = qr;
    this.activities = activities;
    this.sessions = sessions;
    this.locks = locks;
    this.eligible = eligible;
    this.retention = retention;
    this.recording = recording;
    this.replay = replay;
  }

  @Transactional
  public Receipt record(
      UserSession user,
      String token,
      GeoMeasurement location,
      UUID key,
      Object canonical,
      Instant received) {
    var issued = qr.lookup(token);
    UUID activityId = activities.activityOfSession(issued.session());
    locks.activity(activityId, false);
    locks.session(issued.session());
    locks.person(user.personId());
    var activity = activities.find(activityId);
    retention.check(activity.anonymizeAt());
    AttendanceEligibility.Person person;
    try {
      person = eligible.account(user, activity);
    } catch (ApiFailure e) {
      return recording.reject(issued.session(), null, e, "CREDENCIALES", received, null);
    }
    return replay.run(
        "attendance-account",
        user.accountId().toString(),
        key,
        activityId,
        canonical,
        () -> {
          try {
            recording.requireActive(activity, sessions.find(issued.session()));
            IssueSessionQr.validate(issued, received);
            return recording.record(
                issued.session(), person, location, "CREDENCIALES", received, null);
          } catch (ApiFailure e) {
            return recording.reject(issued.session(), person, e, "CREDENCIALES", received, null);
          }
        });
  }
}
