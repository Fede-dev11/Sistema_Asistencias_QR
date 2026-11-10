package ec.edu.unl.sso.compartido;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Contadores en BD por ventana de 1 minuto (tabla contadores_limite). */
@Component
public class RateLimiter {
  private final JdbcClient db;
  private final Clock clock;

  RateLimiter(JdbcClient db, Clock clock) {
    this.db = db;
    this.clock = clock;
  }

  /** Cuenta un intento; devuelve false si supera {@code limit} en la ventana actual. */
  public boolean allow(String scope, String key, int limit) {
    Instant window = clock.instant().truncatedTo(ChronoUnit.MINUTES);
    int count =
        db.sql(
                "INSERT INTO contadores_limite(ambito, ventana, conteo) VALUES (?,?,1) "
                    + "ON CONFLICT (ambito, ventana) DO UPDATE SET conteo = contadores_limite.conteo + 1 "
                    + "RETURNING conteo")
            .param(Secrets.sha256Hex(scope + ":" + key))
            .param(OffsetDateTime.ofInstant(window, ZoneOffset.UTC))
            .query(Integer.class)
            .single();
    return count <= limit;
  }

  /** Segundos hasta que termina la ventana actual. */
  public long retryAfterSeconds() {
    Instant now = clock.instant();
    return Math.max(
        1, Duration.between(now, now.truncatedTo(ChronoUnit.MINUTES).plusSeconds(60)).toSeconds());
  }
}
