package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigureGuests {
  private final JdbcTemplate db;
  private final ActivityAuthorization scope;
  private final AttendanceLocks locks;
  private final ActivityAuditWriter audit;

  public ConfigureGuests(
      JdbcTemplate db,
      ActivityAuthorization scope,
      AttendanceLocks locks,
      ActivityAuditWriter audit) {
    this.db = db;
    this.scope = scope;
    this.locks = locks;
    this.audit = audit;
  }

  @Transactional
  public Map<String, Object> set(UserSession user, UUID id, boolean enabled, int duration) {
    scope.write(user, id);
    locks.activity(id, true);
    var activity = scope.write(user, id);
    if (!activity.mode().equals("EVENTO") || !activity.state().equals("ABIERTA"))
      throw ApiFailure.conflict();
    if (duration <= 40) throw ApiFailure.invalid();
    var previous =
        db.queryForList(
            "SELECT habilitado,tolerancia_invitado_segundos FROM configuracion_invitados WHERE actividad_id=?",
            id);
    Map<String, Object> next =
        Map.of("habilitado", enabled, "tolerancia_invitado_segundos", duration);
    db.update(
        "INSERT INTO configuracion_invitados(actividad_id,habilitado,tolerancia_invitado_segundos,version) VALUES (?,?,?,1) ON CONFLICT(actividad_id) DO UPDATE SET habilitado=EXCLUDED.habilitado,tolerancia_invitado_segundos=EXCLUDED.tolerancia_invitado_segundos,version=configuracion_invitados.version+1,acceso_generacion=configuracion_invitados.acceso_generacion+CASE WHEN configuracion_invitados.habilitado AND NOT EXCLUDED.habilitado THEN 1 ELSE 0 END",
        id,
        enabled,
        duration);
    audit.write(
        id,
        user.accountId(),
        "CONFIGURAR_INVITADOS",
        null,
        previous.isEmpty() ? Map.of() : Map.of("configuracion_invitados", previous.getFirst()),
        Map.of("configuracion_invitados", next));
    return next;
  }
}
