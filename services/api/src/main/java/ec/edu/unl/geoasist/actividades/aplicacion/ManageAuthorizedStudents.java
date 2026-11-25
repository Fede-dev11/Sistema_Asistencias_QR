package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageAuthorizedStudents {
  private final JdbcTemplate db;
  private final ActivityAuthorization scope;
  private final AttendanceLocks locks;
  private final ActivityAuditWriter audit;
  private final IdempotencyService replay;

  public ManageAuthorizedStudents(
      JdbcTemplate db,
      ActivityAuthorization scope,
      AttendanceLocks locks,
      ActivityAuditWriter audit,
      IdempotencyService replay) {
    this.db = db;
    this.scope = scope;
    this.locks = locks;
    this.audit = audit;
    this.replay = replay;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> list(UserSession user, UUID id, int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw ApiFailure.invalid();
    scope.read(user, id);
    Long total =
        db.queryForObject(
            "SELECT count(*) FROM estudiantes_autorizados WHERE actividad_id=?", Long.class, id);
    var items =
        db.query(
            "SELECT p.id,p.cedula_normalizada,p.nombres,e.vigente FROM estudiantes_autorizados e JOIN personas p ON p.id=e.persona_id "
                + "WHERE e.actividad_id=? ORDER BY p.nombres,p.id LIMIT ? OFFSET ?",
            (r, n) ->
                Map.of(
                    "persona_id", r.getObject("id", UUID.class),
                    "cedula", r.getString("cedula_normalizada"),
                    "nombres", r.getString("nombres"),
                    "vigente", r.getBoolean("vigente")),
            id,
            size,
            (long) page * size);
    return Map.of("items", items, "page", page, "size", size, "total", total);
  }

  private void lockOpenClass(UserSession user, UUID id) {
    scope.write(user, id);
    locks.activity(id, true);
    var activity = scope.write(user, id);
    if (!activity.mode().equals("CLASE") || !activity.state().equals("ABIERTA"))
      throw ApiFailure.conflict();
  }

  @Transactional
  public Receipt add(UserSession user, UUID id, UUID key, String cedula) {
    return replay.run(
        "add-activity-student",
        user.accountId().toString(),
        key,
        id,
        Map.of("cedula", cedula),
        () -> new Receipt(201, addOnce(user, id, cedula)));
  }

  private Map<String, Object> addOnce(UserSession user, UUID id, String cedula) {
    lockOpenClass(user, id);
    var person =
        db.queryForList(
            "SELECT p.id FROM personas p JOIN cuentas c ON c.persona_id=p.id JOIN roles_cuenta r ON r.cuenta_id=c.id AND r.rol='ESTUDIANTE' WHERE p.cedula_normalizada=?",
            UUID.class,
            cedula);
    if (person.isEmpty())
      throw new ApiFailure(
          400,
          "SOLICITUD_INVALIDA",
          "Revisa los datos enviados.",
          "La cédula no es de un estudiante.");
    db.update(
        "INSERT INTO estudiantes_autorizados VALUES (?,?,true) ON CONFLICT(actividad_id,persona_id) DO UPDATE SET vigente=true",
        id,
        person.getFirst());
    audit.write(
        id,
        user.accountId(),
        "AGREGAR_ESTUDIANTE",
        null,
        Map.of(),
        Map.of("persona_id", person.getFirst()));
    return Map.of("activity_id", id, "persona_id", person.getFirst(), "vigente", true);
  }

  @Transactional
  public void remove(UserSession user, UUID id, UUID person) {
    lockOpenClass(user, id);
    if (db.update(
            "UPDATE estudiantes_autorizados SET vigente=false WHERE actividad_id=? AND persona_id=?",
            id,
            person)
        == 0) throw ApiFailure.missing();
    audit.write(
        id,
        user.accountId(),
        "QUITAR_ESTUDIANTE",
        null,
        Map.of("persona_id", person),
        Map.of("vigente", false));
  }

  /** Quitados siguen existiendo con vigente=false, así que ON CONFLICT no los reincorpora. */
  @Transactional
  public Map<String, Object> sync(UserSession user, UUID id) {
    lockOpenClass(user, id);
    int added =
        db.update(
            "INSERT INTO estudiantes_autorizados SELECT DISTINCT ?,m.persona_id,true FROM matriculas m JOIN actividad_paralelos ap ON ap.paralelo_id=m.paralelo_id "
                + "WHERE ap.actividad_id=? AND m.vigente ON CONFLICT DO NOTHING",
            id,
            id);
    audit.write(
        id,
        user.accountId(),
        "SINCRONIZAR_ESTUDIANTES",
        null,
        Map.of(),
        Map.of("agregados", added));
    return Map.of("activity_id", id, "agregados", added);
  }
}
