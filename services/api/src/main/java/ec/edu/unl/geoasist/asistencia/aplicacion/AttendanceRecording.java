package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.actividades.dominio.Activity;
import ec.edu.unl.geoasist.asistencia.aplicacion.AttendanceEligibility.Person;
import ec.edu.unl.geoasist.asistencia.dominio.*;
import ec.edu.unl.geoasist.asistencia.persistencia.AttemptRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.*;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.sesiones.persistencia.SessionRepository.Session;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AttendanceRecording {
  private final AttemptRepository attempts;
  private final AttendanceGeography geography;
  private final Clock clock;

  public AttendanceRecording(
      AttemptRepository attempts, AttendanceGeography geography, Clock clock) {
    this.attempts = attempts;
    this.geography = geography;
    this.clock = clock;
  }

  public void requireActive(Activity activity, Session session) {
    Instant now = clock.instant();
    if (!activity.state().equals("ABIERTA")
        || !session.state().equals("ACTIVA")
        || now.isBefore(session.windowStart())
        || !now.isBefore(session.windowEnd()))
      throw new ApiFailure(409, "SESION_INACTIVA", "La sesión no admite nuevos registros.");
  }

  public Receipt reject(
      UUID session, Person person, ApiFailure error, String channel, Instant received, UUID start) {
    UUID attempt =
        attempts.record(
            session,
            person,
            AttendanceOutcome.rejected(error.code(), error.getMessage()),
            channel,
            received,
            start);
    Map<String, Object> body = new LinkedHashMap<>(ProblemDetails.of(error));
    body.put("attempt_id", attempt);
    return new Receipt(error.status(), body);
  }

  public Receipt record(
      UUID session,
      Person person,
      GeoMeasurement location,
      String channel,
      Instant received,
      UUID start) {
    geography.validatePayload(session, location);
    var existing = attempts.existing(session, person.id());
    if (existing.isPresent()) {
      attempts.record(session, person, AttendanceOutcome.duplicate(), channel, received, start);
      return new Receipt(
          200, AttemptRepository.receipt(existing.get(), "La asistencia ya estaba registrada."));
    }
    AttendanceOutcome outcome =
        geography.evaluate(
            session, location, attempts.consecutiveUncertainty(session, person.id()), received);
    UUID attempt = attempts.record(session, person, outcome, channel, received, start);
    if (outcome.result() == AttendanceOutcome.Result.ACEPTADO) {
      boolean created = attempts.confirm(session, person, attempt, received, channel);
      var attendance = attempts.existing(session, person.id()).orElseThrow();
      return new Receipt(
          created ? 201 : 200,
          AttemptRepository.receipt(
              attendance, created ? outcome.message() : "La asistencia ya estaba registrada."));
    }
    if (outcome.result() == AttendanceOutcome.Result.RECHAZADO) {
      Map<String, Object> body =
          new LinkedHashMap<>(
              ProblemDetails.of(new ApiFailure(422, outcome.code(), outcome.message())));
      body.put("attempt_id", attempt);
      return new Receipt(422, body);
    }
    return new Receipt(
        202,
        Map.of(
            "attempt_id",
            attempt,
            "resultado",
            outcome.result().name(),
            "code",
            outcome.code(),
            "mensaje",
            outcome.message()));
  }
}
