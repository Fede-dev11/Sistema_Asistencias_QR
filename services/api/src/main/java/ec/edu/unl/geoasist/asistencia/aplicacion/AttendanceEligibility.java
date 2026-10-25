package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.actividades.dominio.Activity;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.dominio.Cedula;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AttendanceEligibility {
  public record Person(UUID id, String name) {}

  private final JdbcTemplate db;

  public AttendanceEligibility(JdbcTemplate db) {
    this.db = db;
  }

  public Person account(UserSession user, Activity activity) {
    Person person =
        db
            .query(
                "SELECT p.id,p.nombres FROM personas p JOIN cuentas c ON c.persona_id=p.id WHERE c.id=? AND c.persona_id=? AND c.activa",
                (r, n) -> new Person(r.getObject(1, UUID.class), r.getString(2)),
                user.accountId(),
                user.personId())
            .stream()
            .findFirst()
            .orElseThrow(ApiFailure::unauthenticated);
    if (activity.mode().equals("CLASE")
        && !Boolean.TRUE.equals(
            db.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM estudiantes_autorizados e JOIN roles_cuenta r ON r.cuenta_id=? AND r.rol='ESTUDIANTE' WHERE e.actividad_id=? AND e.persona_id=? AND e.vigente)",
                Boolean.class,
                user.accountId(),
                activity.id(),
                person.id()))) throw ApiFailure.forbidden();
    if (activity.mode().equals("EVENTO")) {
      String access =
          db.queryForObject(
              "SELECT acceso FROM actividades WHERE id=?", String.class, activity.id());
      if (!"ABIERTO".equals(access)
          && !Boolean.TRUE.equals(
              db.queryForObject(
                  "SELECT EXISTS(SELECT 1 FROM invitados_evento WHERE actividad_id=? AND persona_id=? AND activo)",
                  Boolean.class,
                  activity.id(),
                  person.id()))) throw ApiFailure.forbidden();
    }
    return person;
  }

  public Person selfRegistered(Activity activity, String rawCedula, String rawNames) {
    String access =
        db.queryForObject("SELECT acceso FROM actividades WHERE id=?", String.class, activity.id());
    if (!activity.mode().equals("EVENTO") || !"ABIERTO".equals(access))
      throw ApiFailure.forbidden();
    String cedula = Cedula.of(rawCedula).value();
    String names = rawNames == null ? "" : rawNames.strip();
    if (names.isEmpty() || names.codePointCount(0, names.length()) > 200)
      throw ApiFailure.invalid();
    var found =
        db.queryForList("SELECT id FROM personas WHERE cedula_normalizada=?", UUID.class, cedula);
    UUID person;
    if (found.isEmpty()) {
      person = UUID.randomUUID();
      db.update(
          "INSERT INTO personas(id,cedula_normalizada,nombres,tipo) VALUES (?,?,?,'EXTERNA') ON CONFLICT(cedula_normalizada) DO NOTHING",
          person,
          cedula,
          names);
      person =
          db.queryForObject(
              "SELECT id FROM personas WHERE cedula_normalizada=?", UUID.class, cedula);
    } else {
      person = found.getFirst();
    }
    if (Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM cuentas WHERE persona_id=?)", Boolean.class, person)))
      throw new ApiFailure(
          403, "USAR_CREDENCIALES", "La cédula tiene cuenta. Debes ingresar con credenciales.");
    return new Person(person, names);
  }

  public Optional<UUID> personId(String cedula) {
    return db
        .queryForList("SELECT id FROM personas WHERE cedula_normalizada=?", UUID.class, cedula)
        .stream()
        .findFirst();
  }

  public Person guest(UUID activity, UUID person) {
    if (person == null) throw ApiFailure.forbidden();
    if (Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM cuentas WHERE persona_id=?)", Boolean.class, person)))
      throw new ApiFailure(
          403, "USAR_CREDENCIALES", "La cédula tiene cuenta. Debes ingresar con credenciales.");
    return db
        .query(
            "SELECT persona_id,nombres FROM invitados_evento WHERE actividad_id=? AND persona_id=? AND activo",
            (r, n) -> new Person(r.getObject(1, UUID.class), r.getString(2)),
            activity,
            person)
        .stream()
        .findFirst()
        .orElseThrow(ApiFailure::forbidden);
  }
}
