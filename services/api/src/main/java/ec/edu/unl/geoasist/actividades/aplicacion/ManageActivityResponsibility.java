package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageActivityResponsibility {
  private final JdbcTemplate db;
  private final ActivityAuthorization scope;
  private final ActivityRepository activities;
  private final ActivityAuditWriter audit;

  public ManageActivityResponsibility(
      JdbcTemplate db,
      ActivityAuthorization scope,
      ActivityRepository activities,
      ActivityAuditWriter audit) {
    this.db = db;
    this.scope = scope;
    this.activities = activities;
    this.audit = audit;
  }

  @Transactional
  public Map<String, Object> replace(
      UserSession user, UUID activityId, UUID accountId, String correo) {
    var activity = scope.manage(user, activityId);
    if (!"CLASE".equals(activity.mode())) throw ApiFailure.invalid();

    UUID target = accountId;
    if (target == null && correo != null) {
      target =
          db.query(
                  "SELECT id FROM cuentas WHERE identificador=?",
                  (r, n) -> r.getObject("id", UUID.class),
                  correo.strip().toLowerCase(Locale.ROOT))
              .stream()
              .findFirst()
              .orElseThrow(ApiFailure::missing);
    }
    if (target == null) throw ApiFailure.invalid();
    var account =
        db.query(
                "SELECT activa FROM cuentas WHERE id=?",
                (r, n) -> r.getObject("activa", Boolean.class),
                target)
            .stream()
            .findFirst()
            .orElseThrow(ApiFailure::missing);
    if (!Boolean.TRUE.equals(account))
      throw new ApiFailure(422, "RESPONSABLE_NO_AUTORIZADO", "La cuenta responsable no está activa.");

    var roles =
        db.queryForList(
            "SELECT rol FROM roles_cuenta WHERE cuenta_id=?", String.class, target);
    if (!roles.contains("DOCENTE") && !roles.contains("ENCARGADO") && !roles.contains("DIRECTOR_CARRERA"))
      throw new ApiFailure(422, "RESPONSABLE_NO_AUTORIZADO", "La cuenta no puede asumir responsabilidades académicas.");

    if (roles.contains("DIRECTOR_CARRERA")) {
      boolean authorized =
          Boolean.TRUE.equals(
              db.queryForObject(
                  "SELECT EXISTS("
                      + "SELECT 1 FROM actividad_carreras ac "
                      + "JOIN direcciones_carrera d ON d.carrera_id=ac.carrera_id "
                      + "WHERE ac.actividad_id=? AND d.cuenta_id=? AND d.vigente)",
                  Boolean.class,
                  activityId,
                  target));
      if (!authorized)
        throw new ApiFailure(403, "DIRECTOR_FUERA_DE_CARRERA", "El director no pertenece a la carrera de la clase.");
    } else {
      boolean authorized =
          Boolean.TRUE.equals(
              db.queryForObject(
                  "SELECT EXISTS("
                      + "SELECT 1 FROM actividad_paralelos ap "
                      + "JOIN docentes_paralelo dp ON dp.paralelo_id=ap.paralelo_id "
                      + "WHERE ap.actividad_id=? AND dp.cuenta_id=? AND dp.vigente)",
                  Boolean.class,
                  activityId,
                  target));
      if (!authorized)
        throw new ApiFailure(403, "DOCENTE_FUERA_DE_PARALELO", "El docente no está autorizado en los paralelos de la clase.");
    }

    UUID before = activity.responsible();
    db.update("UPDATE actividades SET responsable_id=? WHERE id=?", target, activityId);
    audit.write(
        activityId,
        user.accountId(),
        "ASIGNAR_RESPONSABLE_CLASE",
        null,
        Map.of("responsable_id", before),
        Map.of("responsable_id", target));
    return Map.of("responsable_id", target);
  }
}
