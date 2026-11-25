package ec.edu.unl.geoasist.qr.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuthorization;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.aplicacion.OpaqueSecrets;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.sesiones.persistencia.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

@Service
public class IssueSessionQr {
  public record Token(UUID id, UUID session, Instant issued, Instant expires) {}

  private record Cached(String value, Instant issued, Instant expires) {}

  private final JdbcTemplate db;
  private final Clock clock;
  private final OpaqueSecrets secrets;
  private final ActivityRepository activities;
  private final ActivityAuthorization scope;
  private final AttendanceLocks locks;
  private final SessionRepository sessions;
  private final ConcurrentHashMap<UUID, Cached> cache = new ConcurrentHashMap<>();

  public IssueSessionQr(
      JdbcTemplate db,
      Clock clock,
      OpaqueSecrets secrets,
      ActivityRepository activities,
      ActivityAuthorization scope,
      AttendanceLocks locks,
      SessionRepository sessions) {
    this.db = db;
    this.clock = clock;
    this.secrets = secrets;
    this.activities = activities;
    this.scope = scope;
    this.locks = locks;
    this.sessions = sessions;
  }

  @Transactional
  public Map<String, Object> get(UserSession user, UUID session) {
    UUID activity = activities.activityOfSession(session);
    scope.write(user, activity);
    locks.activity(activity, false);
    locks.session(session);
    scope.write(user, activity);
    if (!active(session, activity)) throw ApiFailure.conflict();
    return issue(session);
  }

  @Transactional
  public void rotate(UUID session) {
    UUID activity = activities.activityOfSession(session);
    locks.activity(activity, false);
    locks.session(session);
    if (active(session, activity)) issue(session);
  }

  private boolean active(UUID session, UUID activity) {
    var a = activities.find(activity);
    var s = sessions.find(session);
    var now = clock.instant();
    return a.state().equals("ABIERTA")
        && SessionRepository.usesQr(sessions.method(session))
        && s.state().equals("ACTIVA")
        && !now.isBefore(s.windowStart())
        && now.isBefore(s.windowEnd());
  }

  private Map<String, Object> issue(UUID session) {
    Instant now = clock.instant();
    Cached qr = cache.get(session);
    if (qr == null || !now.isBefore(qr.issued().plusSeconds(30)) || now.isBefore(qr.issued())) {
      qr = new Cached(secrets.issue(), now, now.plusSeconds(40));
      db.update(
          "INSERT INTO tokens_qr VALUES (?,?,?,?,?)",
          UUID.randomUUID(),
          session,
          OpaqueSecrets.hash(qr.value()),
          java.sql.Timestamp.from(now),
          java.sql.Timestamp.from(qr.expires()));
      cache.put(session, qr);
      Cached current = qr;
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status != STATUS_COMMITTED) cache.remove(session, current);
            }
          });
    }
    return Map.of(
        "qr_token",
        qr.value(),
        "emitido_en",
        qr.issued(),
        "vence_en",
        qr.expires(),
        "rotacion_segundos",
        30,
        "vigencia_segundos",
        40);
  }

  public Token lookup(String value) {
    return db
        .query(
            "SELECT id,sesion_id,emitido_en,expira_en FROM tokens_qr WHERE secreto_hash=?",
            (r, n) ->
                new Token(
                    r.getObject(1, UUID.class),
                    r.getObject(2, UUID.class),
                    r.getTimestamp(3).toInstant(),
                    r.getTimestamp(4).toInstant()),
            OpaqueSecrets.hash(value))
        .stream()
        .findFirst()
        .orElseThrow(
            () -> new ApiFailure(422, "TOKEN_INVALIDO", "El QR no es válido. Escanea el actual."));
  }

  public static void validate(Token token, Instant received) {
    if (received.isBefore(token.issued()) || !received.isBefore(token.expires()))
      throw new ApiFailure(422, "TOKEN_VENCIDO", "El QR ha vencido. Escanea el actual.");
  }

  public void expireCache() {
    Instant now = clock.instant();
    cache.entrySet().removeIf(e -> !now.isBefore(e.getValue().expires()));
  }
}
