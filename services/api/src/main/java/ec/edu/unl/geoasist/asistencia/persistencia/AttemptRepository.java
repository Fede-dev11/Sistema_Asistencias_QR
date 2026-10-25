package ec.edu.unl.geoasist.asistencia.persistencia;

import ec.edu.unl.geoasist.asistencia.aplicacion.AttendanceEligibility.Person;
import ec.edu.unl.geoasist.asistencia.dominio.AttendanceOutcome;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AttemptRepository {
  public record Attendance(
      UUID id, UUID session, UUID person, UUID attempt, Instant registered, String name) {}

  private final JdbcTemplate db;

  public AttemptRepository(JdbcTemplate db) {
    this.db = db;
  }

  public UUID record(
      UUID session,
      Person person,
      AttendanceOutcome outcome,
      String deviceHash,
      String channel,
      Instant received,
      UUID start) {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,dispositivo_hash,canal,recibido_en,distancia_m,precision_m,inicio_invitado_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
        id,
        session,
        person == null ? null : person.id(),
        person == null ? null : person.name(),
        outcome.result().name(),
        outcome.code(),
        outcome.message(),
        deviceHash,
        channel,
        java.sql.Timestamp.from(received),
        rounded(outcome.distance()),
        rounded(outcome.precision()),
        start);
    return id;
  }

  private static Double rounded(Double value) {
    return value == null || !Double.isFinite(value) || value < 0 ? null : Math.rint(value);
  }

  public Optional<Attendance> existing(UUID session, UUID person) {
    return db
        .query(
            "SELECT id,sesion_id,persona_id,intento_id,registrada_en,nombres_registro FROM asistencias WHERE sesion_id=? AND persona_id=?",
            (r, n) ->
                new Attendance(
                    r.getObject(1, UUID.class),
                    r.getObject(2, UUID.class),
                    r.getObject(3, UUID.class),
                    r.getObject(4, UUID.class),
                    r.getTimestamp(5).toInstant(),
                    r.getString(6)),
            session,
            person)
        .stream()
        .findFirst();
  }

  public boolean confirm(UUID session, Person person, UUID attempt, Instant received, String form) {
    return confirm(session, person, attempt, received, form, null);
  }

  public boolean confirm(
      UUID session, Person person, UUID attempt, Instant received, String form, String deviceHash) {
    return !db.queryForList(
            "INSERT INTO asistencias(id,sesion_id,persona_id,intento_id,registrada_en,forma,nombres_registro,dispositivo_hash) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING RETURNING id",
            UUID.class,
            UUID.randomUUID(),
            session,
            person.id(),
            attempt,
            java.sql.Timestamp.from(received),
            form,
            person.name(),
            deviceHash)
        .isEmpty();
  }

  public Optional<Attendance> existingDevice(UUID session, String deviceHash) {
    return db.query(
            "SELECT id,sesion_id,persona_id,intento_id,registrada_en,nombres_registro FROM asistencias WHERE sesion_id=? AND dispositivo_hash=?",
            (r, n) -> new Attendance(r.getObject(1, UUID.class), r.getObject(2, UUID.class),
                r.getObject(3, UUID.class), r.getObject(4, UUID.class),
                r.getTimestamp(5).toInstant(), r.getString(6)), session, deviceHash)
        .stream().findFirst();
  }

  public int consecutiveUncertainty(UUID session, UUID person) {
    int count = 0;
    for (String code :
        db.queryForList(
            "SELECT code FROM intentos_asistencia WHERE sesion_id=? AND persona_id=? ORDER BY secuencia DESC LIMIT 2",
            String.class,
            session,
            person)) {
      if (Set.of(
              "UBICACION_REQUERIDA",
              "UBICACION_INVALIDA",
              "PRECISION_INSUFICIENTE",
              "UBICACION_ANTIGUA",
              "UBICACION_INCIERTA")
          .contains(code)) count++;
      else break;
    }
    return count;
  }

  public static Map<String, Object> receipt(Attendance attendance, String message) {
    return Map.of(
        "asistencia_id",
        attendance.id(),
        "attempt_id",
        attendance.attempt(),
        "session_id",
        attendance.session(),
        "resultado",
        "CONFIRMADA",
        "registrada_en",
        attendance.registered(),
        "mensaje",
        message);
  }
}
