package ec.edu.unl.geoasist.sesiones.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.*;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.sesiones.persistencia.*;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionLifecycle {
  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityRepository activities;
  private final ActivityAuthorization scope;
  private final AttendanceLocks locks;
  private final SessionRepository sessions;
  private final ActivityAuditWriter audit;
  private final IdempotencyService replay;

  public SessionLifecycle(
      JdbcTemplate db,
      Clock clock,
      ActivityRepository activities,
      ActivityAuthorization scope,
      AttendanceLocks locks,
      SessionRepository sessions,
      ActivityAuditWriter audit,
      IdempotencyService replay) {
    this.db = db;
    this.clock = clock;
    this.activities = activities;
    this.scope = scope;
    this.locks = locks;
    this.sessions = sessions;
    this.audit = audit;
    this.replay = replay;
  }

  @Transactional
  public Receipt change(UserSession user, UUID session, UUID key, boolean start) {
    UUID id = activities.activityOfSession(session);
    scope.write(user, id);
    locks.activity(id, false);
    locks.session(session);
    var activity = scope.write(user, id);
    return replay.run(
        start ? "start-session" : "close-session",
        user.accountId().toString(),
        key,
        id,
        Map.of("session_id", session),
        () -> {
          var row = sessions.find(session);
          if (!activity.state().equals("ABIERTA")) throw ApiFailure.conflict();
          if (start) {
            if (!row.state().equals("PROGRAMADA")
                || clock.instant().isBefore(row.windowStart())
                || !clock.instant().isBefore(row.windowEnd())) throw ApiFailure.conflict();
            String method = sessions.method(session);
            if (method.contains("GPS")
                && !Boolean.TRUE.equals(
                    db.queryForObject(
                        "SELECT COALESCE((SELECT habilitada FROM configuracion_geografica WHERE sesion_id=?),false)",
                        Boolean.class,
                        session)))
              throw new ApiFailure(
                  409,
                  "CONFIGURACION_GPS_REQUERIDA",
                  "El método incluye GPS: configura la geolocalización antes de iniciar.");
            db.update(
                "UPDATE sesiones SET estado='ACTIVA',iniciada_en=? WHERE id=?",
                java.sql.Timestamp.from(clock.instant()),
                session);
          } else {
            if (row.state().equals("CERRADA") || row.state().equals("CANCELADA"))
              throw ApiFailure.conflict();
            db.update(
                "UPDATE sesiones SET estado='CERRADA',cerrada_en=? WHERE id=?",
                java.sql.Timestamp.from(clock.instant()),
                session);
          }
          audit.write(
              id,
              user.accountId(),
              start ? "INICIAR_SESION" : "CERRAR_SESION",
              null,
              Map.of("estado", row.state()),
              Map.of("estado", start ? "ACTIVA" : "CERRADA"));
          return new Receipt(200, SessionRepository.dto(sessions.find(session)));
        });
  }
}
