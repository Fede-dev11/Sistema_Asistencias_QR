package ec.edu.unl.geoasist.conservacion.aplicacion;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** El mismo procedimiento versionado se utiliza en el job y en restauraciones aisladas. */
@Service
public class AnonymizeExpiredActivity {
  private final JdbcTemplate db;
  private final Clock clock;

  public AnonymizeExpiredActivity(JdbcTemplate db, Clock clock) {
    this.db = db;
    this.clock = clock;
  }

  @Transactional
  public int processBatch() {
    return Objects.requireNonNull(
        db.queryForObject(
            "SELECT anonimizar_vencidas(?,100)", Integer.class, Timestamp.from(clock.instant())));
  }
}
