package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.actividades.dominio.WeeklySchedule;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplaceSchedule {
  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityAuthorization scope;
  private final AttendanceLocks locks;
  private final ActivityAuditWriter audit;

  public ReplaceSchedule(
      JdbcTemplate db,
      Clock clock,
      ActivityAuthorization scope,
      AttendanceLocks locks,
      ActivityAuditWriter audit) {
    this.db = db;
    this.clock = clock;
    this.scope = scope;
    this.locks = locks;
    this.audit = audit;
  }

  /** Inserta el horario y sus sesiones PROGRAMADA; devuelve la cantidad generada. */
  @Transactional(propagation = Propagation.MANDATORY)
  public int generate(UUID activity, WeeklySchedule s, UUID space, String method) {
    var windows = s.expand();
    UUID schedule = UUID.randomUUID();
    db.update(
        "INSERT INTO horarios_clase(id,actividad_id,dias,hora_inicio,hora_fin,fecha_desde,fecha_hasta,espacio_id,zona) VALUES (?,?,?::smallint[],?,?,?,?,?,?)",
        schedule,
        activity,
        "{" + String.join(",", s.days().stream().map(String::valueOf).toList()) + "}",
        java.sql.Time.valueOf(s.start()),
        java.sql.Time.valueOf(s.end()),
        java.sql.Date.valueOf(s.from()),
        java.sql.Date.valueOf(s.to()),
        space,
        s.zone().getId());
    for (var w : windows)
      db.update(
          "INSERT INTO sesiones(id,actividad_id,espacio_id,ventana_inicio,ventana_fin,metodo_verificacion,horario_id) VALUES (?,?,?,?,?,?,?)",
          UUID.randomUUID(),
          activity,
          space,
          Timestamp.from(w.start()),
          Timestamp.from(w.end()),
          method,
          schedule);
    return windows.size();
  }

  @Transactional
  public Map<String, Object> replace(UserSession user, UUID id, WeeklySchedule schedule) {
    scope.write(user, id);
    locks.activity(id, true);
    var activity = scope.write(user, id);
    locks.allSessions(id);
    if (!activity.mode().equals("CLASE") || !activity.state().equals("ABIERTA"))
      throw ApiFailure.conflict();
    var row =
        db.queryForMap("SELECT espacio_id,metodo_verificacion FROM actividades WHERE id=?", id);
    // Solo se regeneran las programadas futuras sin rastro de asistencia; el resto se conserva.
    int removed =
        db.update(
            "DELETE FROM sesiones s WHERE s.actividad_id=? AND s.horario_id IS NOT NULL AND s.estado='PROGRAMADA' AND s.ventana_inicio>? "
                + "AND NOT EXISTS(SELECT 1 FROM intentos_asistencia i WHERE i.sesion_id=s.id) "
                + "AND NOT EXISTS(SELECT 1 FROM tokens_qr t WHERE t.sesion_id=s.id)",
            id,
            Timestamp.from(clock.instant()));
    db.update("DELETE FROM horarios_clase WHERE actividad_id=?", id);
    int created =
        generate(
            id, schedule, (UUID) row.get("espacio_id"), (String) row.get("metodo_verificacion"));
    audit.write(
        id,
        user.accountId(),
        "REEMPLAZAR_HORARIO",
        null,
        Map.of("sesiones_eliminadas", removed),
        Map.of(
            "sesiones_generadas",
            created,
            "dias",
            schedule.days(),
            "desde",
            schedule.from().toString(),
            "hasta",
            schedule.to().toString()));
    return Map.of("activity_id", id, "eliminadas", removed, "generadas", created);
  }
}
