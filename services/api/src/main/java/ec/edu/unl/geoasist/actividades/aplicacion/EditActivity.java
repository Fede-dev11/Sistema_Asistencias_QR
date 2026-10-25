package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.dominio.RetentionPolicy;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditActivity {
  public record Changes(
      String nombre, UUID espacio_id, String metodo_verificacion, String acceso) {}

  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityAuthorization scope;
  private final AttendanceLocks locks;
  private final ActivityAuditWriter audit;
  private final SpaceMethodRules rules;
  private final FindVisibleActivities queries;

  public EditActivity(
      JdbcTemplate db,
      Clock clock,
      ActivityAuthorization scope,
      AttendanceLocks locks,
      ActivityAuditWriter audit,
      SpaceMethodRules rules,
      FindVisibleActivities queries) {
    this.db = db;
    this.clock = clock;
    this.scope = scope;
    this.locks = locks;
    this.audit = audit;
    this.rules = rules;
    this.queries = queries;
  }

  private Map<String, Object> lockOpen(UserSession user, UUID id) {
    scope.write(user, id);
    locks.activity(id, true);
    var activity = scope.write(user, id);
    locks.allSessions(id);
    if (!activity.state().equals("ABIERTA")) throw ApiFailure.conflict();
    return db.queryForMap(
        "SELECT nombre,espacio_id,metodo_verificacion,acceso FROM actividades WHERE id=?", id);
  }

  /** Cambia los valores por defecto de la actividad; las sesiones ya creadas no se tocan. */
  @Transactional
  public Map<String, Object> edit(UserSession user, UUID id, Changes c) {
    var before = lockOpen(user, id);
    String name = c.nombre() == null ? (String) before.get("nombre") : c.nombre().strip();
    if (name.isBlank() || name.length() > 200) throw ApiFailure.invalid();
    UUID space = c.espacio_id() == null ? (UUID) before.get("espacio_id") : c.espacio_id();
    String method =
        c.metodo_verificacion() == null
            ? (String) before.get("metodo_verificacion")
            : c.metodo_verificacion();
    rules.check(space, method);
    String access = c.acceso() == null ? (String) before.get("acceso") : c.acceso();
    if (c.acceso() != null && !("CERRADO".equals(c.acceso()) || "ABIERTO".equals(c.acceso())))
      throw ApiFailure.invalid();
    if (c.acceso() != null && !Objects.equals(c.acceso(), before.get("acceso"))) {
      Integer started =
          db.queryForObject(
              "SELECT count(*) FROM sesiones WHERE actividad_id=? AND estado<>'PROGRAMADA'",
              Integer.class,
              id);
      if (started != null && started > 0) throw ApiFailure.conflict();
    }
    db.update(
        "UPDATE actividades SET nombre=?,espacio_id=?,metodo_verificacion=?,acceso=? WHERE id=?",
        name,
        space,
        method,
        access,
        id);
    audit.write(
        id,
        user.accountId(),
        "EDITAR_ACTIVIDAD",
        null,
        Map.of(
            "nombre",
            before.get("nombre"),
            "metodo_verificacion",
            before.get("metodo_verificacion")),
        Map.of("nombre", name, "metodo_verificacion", method));
    return queries.get(user, id);
  }

  /** Solo si ninguna sesión salió de PROGRAMADA; las programadas pasan a CANCELADA. */
  @Transactional
  public Map<String, Object> cancel(UserSession user, UUID id) {
    lockOpen(user, id);
    Integer started =
        db.queryForObject(
            "SELECT count(*) FROM sesiones WHERE actividad_id=? AND estado NOT IN ('PROGRAMADA','CANCELADA')",
            Integer.class,
            id);
    if (started != null && started > 0) throw ApiFailure.conflict();
    var now = clock.instant();
    db.update(
        "UPDATE sesiones SET estado='CANCELADA' WHERE actividad_id=? AND estado='PROGRAMADA'", id);
    db.update(
        "UPDATE actividades SET estado='CANCELADA',cerrada_en=?,anonimizar_en=? WHERE id=?",
        Timestamp.from(now),
        Timestamp.from(RetentionPolicy.expires(now)),
        id);
    audit.write(
        id,
        user.accountId(),
        "CANCELAR_ACTIVIDAD",
        null,
        Map.of("estado", "ABIERTA"),
        Map.of("estado", "CANCELADA"));
    return queries.get(user, id);
  }
}
