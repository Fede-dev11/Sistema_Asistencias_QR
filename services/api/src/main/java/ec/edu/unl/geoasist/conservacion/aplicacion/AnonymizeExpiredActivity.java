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
    Timestamp cutoff = Timestamp.from(clock.instant());
    db.update(
        "UPDATE intentos_asistencia i SET dispositivo_hash=NULL FROM sesiones s JOIN actividades a ON a.id=s.actividad_id WHERE i.sesion_id=s.id AND a.anonimizar_en<=?",
        cutoff);
    db.update(
        "UPDATE asistencias x SET dispositivo_hash=NULL FROM sesiones s JOIN actividades a ON a.id=s.actividad_id WHERE x.sesion_id=s.id AND a.anonimizar_en<=?",
        cutoff);
    return Objects.requireNonNull(
        db.queryForObject("SELECT anonimizar_vencidas(?,100)", Integer.class, cutoff));
  }
}
