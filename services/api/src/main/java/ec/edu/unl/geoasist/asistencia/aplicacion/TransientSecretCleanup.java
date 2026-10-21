package ec.edu.unl.geoasist.asistencia.aplicacion;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransientSecretCleanup {
  public record Result(
      int idempotencyReplays,
      int guestStarts,
      int qrTokens,
      int accessSessions,
      int rateCounters) {}

  private static final Duration RETRY_GRACE = Duration.ofHours(25);

  private final JdbcTemplate db;
  private final Clock clock;

  public TransientSecretCleanup(JdbcTemplate db, Clock clock) {
    this.db = db;
    this.clock = clock;
  }

  @Transactional
  public Result cleanup() {
    Instant now = clock.instant();
    Timestamp current = Timestamp.from(now);
    Timestamp retryCutoff = Timestamp.from(now.minus(RETRY_GRACE));

    int replays =
        db.update(
            "DELETE FROM idempotencias i WHERE i.vence_en<=? OR EXISTS (SELECT 1 FROM actividades a WHERE a.id=i.actividad_id AND a.anonimizar_en<=?)",
            current,
            current);
    int starts =
        db.update(
            "DELETE FROM inicios_invitado i WHERE GREATEST(i.vence_en,COALESCE(i.consumido_en,i.vence_en))<=? OR EXISTS (SELECT 1 FROM sesiones s JOIN actividades a ON a.id=s.actividad_id WHERE s.id=i.sesion_id AND a.anonimizar_en<=?)",
            retryCutoff,
            current);
    int qrTokens =
        db.update(
            "DELETE FROM tokens_qr q WHERE q.expira_en<=? OR EXISTS (SELECT 1 FROM sesiones s JOIN actividades a ON a.id=s.actividad_id WHERE s.id=q.sesion_id AND a.anonimizar_en<=?)",
            retryCutoff,
            current);
    int sessions =
        db.update(
            "DELETE FROM sesiones_acceso WHERE vence_en<=? OR (revocada_en IS NOT NULL AND revocada_en<=?)",
            current,
            current);
    int counters = db.update("DELETE FROM contadores_limite WHERE vence_en<=?", current);
    return new Result(replays, starts, qrTokens, sessions, counters);
  }
}

@Component
@ConditionalOnProperty(name = "geoasist.scheduling-enabled", matchIfMissing = true)
class TransientSecretCleanupScheduler {
  private final TransientSecretCleanup cleanup;

  TransientSecretCleanupScheduler(TransientSecretCleanup cleanup) {
    this.cleanup = cleanup;
  }

  @Scheduled(fixedDelayString = "${geoasist.secret-cleanup-delay-ms:3600000}")
  void run() {
    cleanup.cleanup();
  }
}
