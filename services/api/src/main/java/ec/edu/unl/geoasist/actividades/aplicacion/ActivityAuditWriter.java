package ec.edu.unl.geoasist.actividades.aplicacion;

import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;

@Component
public class ActivityAuditWriter {
  private final JdbcTemplate db;
  private final Clock clock;
  private final ObjectMapper json;

  public ActivityAuditWriter(JdbcTemplate db, Clock clock, ObjectMapper json) {
    this.db = db;
    this.clock = clock;
    this.json = json;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void write(
      UUID activity,
      UUID actor,
      String action,
      String reason,
      Map<String, ?> before,
      Map<String, ?> after) {
    db.update(
        "INSERT INTO auditoria_actividad VALUES (?,?,?,?,?,?,?,?::jsonb,?::jsonb)",
        UUID.randomUUID(),
        activity,
        actor,
        action,
        java.sql.Timestamp.from(clock.instant()),
        ec.edu.unl.geoasist.compartido.aplicacion.RequestCorrelation.id(),
        reason,
        json.writeValueAsString(before),
        json.writeValueAsString(after));
  }
}
