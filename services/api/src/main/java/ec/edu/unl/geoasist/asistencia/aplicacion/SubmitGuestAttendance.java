package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.asistencia.dominio.GeoMeasurement;
import ec.edu.unl.geoasist.asistencia.persistencia.GuestStartRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.*;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.sesiones.persistencia.SessionRepository;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitGuestAttendance {
  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityRepository activities;
  private final SessionRepository sessions;
  private final AttendanceLocks locks;
  private final AttendanceEligibility eligible;
  private final NominalAccessGuard retention;
  private final AttendanceRecording recording;
  private final GuestStartRepository starts;
  private final IdempotencyService replay;

  public SubmitGuestAttendance(
      JdbcTemplate db,
      Clock clock,
      ActivityRepository activities,
      SessionRepository sessions,
      AttendanceLocks locks,
      AttendanceEligibility eligible,
      NominalAccessGuard retention,
      AttendanceRecording recording,
      GuestStartRepository starts,
      IdempotencyService replay) {
    this.db = db;
    this.clock = clock;
    this.activities = activities;
    this.sessions = sessions;
    this.locks = locks;
    this.eligible = eligible;
    this.retention = retention;
    this.recording = recording;
    this.starts = starts;
    this.replay = replay;
  }

  @Transactional
  public Receipt submit(
      String secret,
      String cedula,
      GeoMeasurement location,
      UUID key,
      Object canonical,
      Instant received) {
    var initial = starts.bySecret(secret);
    UUID activityId = activities.activityOfSession(initial.session());
    locks.activity(activityId, false);
    locks.session(initial.session());
    var activity = activities.find(activityId);
    retention.check(activity.anonymizeAt());
    UUID personId = eligible.personId(cedula).orElse(null);
    if (personId != null) locks.person(personId);
    locks.guestStart(initial.id());
    var start = starts.find(initial.id());
    AttendanceEligibility.Person person = null;
    try {
      if (!activity.mode().equals("EVENTO")) throw ApiFailure.forbidden();
      if (received.isBefore(start.began()) || !received.isBefore(start.deadline()))
        throw new ApiFailure(
            410,
            "FORMULARIO_EXPIRADO",
            "El tiempo del formulario ha vencido. Escanea un nuevo QR.");
      var configs =
          db.queryForList(
              "SELECT habilitado,acceso_generacion FROM configuracion_invitados WHERE actividad_id=?",
              activityId);
      if (configs.isEmpty()
          || !Boolean.TRUE.equals(configs.getFirst().get("habilitado"))
          || ((Number) configs.getFirst().get("acceso_generacion")).longValue()
              != start.generation()) throw ApiFailure.forbidden();
      person = eligible.guest(activityId, personId);
      if (start.bound() != null && !start.bound().equals(person.id()))
        throw new ApiFailure(
            409,
            "FORMULARIO_CONSUMIDO",
            "Este formulario ya está vinculado a otra persona. Escanea nuevamente.");
    } catch (ApiFailure e) {
      return recording.reject(start.session(), null, e, "INVITADO_LISTADO", received, start.id());
    }
    AttendanceEligibility.Person authorized = person;
    return replay.run(
        "attendance-guest",
        "guest:" + start.id(),
        key,
        activityId,
        canonical,
        () -> {
          try {
            recording.requireActive(activity, sessions.find(start.session()));
            var receipt =
                recording.record(
                    start.session(),
                    authorized,
                    location,
                    "INVITADO_LISTADO",
                    received,
                    start.id());
            starts.bind(start.id(), authorized.id());
            if (receipt.status() == 201 || receipt.status() == 200) {
              UUID attendance = UUID.fromString(receipt.body().get("asistencia_id").toString());
              starts.consume(start.id(), authorized.id(), attendance, clock.instant());
            }
            return receipt;
          } catch (ApiFailure e) {
            return recording.reject(
                start.session(), authorized, e, "INVITADO_LISTADO", received, start.id());
          }
        });
  }
}
