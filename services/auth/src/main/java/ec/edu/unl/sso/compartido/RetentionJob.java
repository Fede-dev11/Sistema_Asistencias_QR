package ec.edu.unl.sso.compartido;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Limpieza diaria: ingresos de más de 90 días y tickets vencidos o usados. */
@Component
public class RetentionJob {
  private final JdbcClient db;
  private final Clock clock;

  RetentionJob(JdbcClient db, Clock clock) {
    this.db = db;
    this.clock = clock;
  }

  /** El cron "-" (pruebas) desactiva la ejecución programada. */
  @Scheduled(cron = "${sso.retention-cron}")
  public void purgar() {
    OffsetDateTime now = OffsetDateTime.now(clock);
    db.sql("DELETE FROM registro_ingresos WHERE en < ?").param(now.minusDays(90)).update();
    db.sql("DELETE FROM tickets WHERE vence_en < ? OR usado_en IS NOT NULL").param(now).update();
  }
}
