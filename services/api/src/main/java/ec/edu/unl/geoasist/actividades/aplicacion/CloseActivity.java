package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.compartido.aplicacion.RecordAbsences;
import ec.edu.unl.geoasist.compartido.dominio.RetentionPolicy;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseActivity {
  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityAuthorization scope;
  private final AttendanceLocks locks;
  private final ActivityAuditWriter audit;
  private final IdempotencyService replay;
  private final FindVisibleActivities queries;
  private final RecordAbsences absences;

  public CloseActivity(
      JdbcTemplate db,
      Clock clock,
      ActivityAuthorization scope,
      AttendanceLocks locks,
      ActivityAuditWriter audit,
      IdempotencyService replay,
      FindVisibleActivities queries,
      RecordAbsences absences) {
    this.db = db;
    this.clock = clock;
    this.scope = scope;
    this.locks = locks;
    this.audit = audit;
    this.replay = replay;
    this.queries = queries;
    this.absences = absences;
  }

  @Transactional
  public Receipt close(UserSession user, UUID id, UUID key) {
    scope.write(user, id);
    locks.activity(id, true);
    scope.write(user, id);
    locks.allSessions(id);
    return replay.run(
        "close-activity",
        user.accountId().toString(),
        key,
        id,
        Map.of("activity_id", id),
        () -> {
          var activity = scope.write(user, id);
          if (!activity.state().equals("ABIERTA")) throw ApiFailure.conflict();
          var now = clock.instant();
          db.queryForList(
                  "UPDATE sesiones SET estado='CERRADA',cerrada_en=? WHERE actividad_id=? AND estado NOT IN ('CERRADA','CANCELADA') RETURNING id",
                  UUID.class,
                  java.sql.Timestamp.from(now),
                  id)
              .forEach(absences::record);
          db.update(
              "UPDATE actividades SET estado='CERRADA',cerrada_en=?,anonimizar_en=? WHERE id=?",
              java.sql.Timestamp.from(now),
              java.sql.Timestamp.from(RetentionPolicy.expires(now)),
              id);
          audit.write(
              id,
              user.accountId(),
              "CERRAR_ACTIVIDAD",
              null,
              Map.of("estado", "ABIERTA"),
              Map.of("estado", "CERRADA"));
          return new Receipt(200, queries.get(user, id));
        });
  }
}
