package ec.edu.unl.geoasist.academico.aplicacion;

import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;

/** Quién, qué y cuándo de cada cambio administrativo (Principio V). Nunca recibe contraseñas. */
@Component
public class AcademicAudit {
  private final JdbcTemplate db;
  private final Clock clock;
  private final ObjectMapper json;

  public AcademicAudit(JdbcTemplate db, Clock clock, ObjectMapper json) {
    this.db = db;
    this.clock = clock;
    this.json = json;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void write(
      UUID actor,
      String action,
      String entity,
      UUID entityId,
      Map<String, ?> before,
      Map<String, ?> after) {
    db.update(
        "INSERT INTO auditoria_academica VALUES (?,?,?,?,?,?,?,?::jsonb,?::jsonb)",
        UUID.randomUUID(),
        actor,
        action,
        entity,
        entityId,
        java.sql.Timestamp.from(clock.instant()),
        ec.edu.unl.geoasist.compartido.aplicacion.RequestCorrelation.id(),
        json.writeValueAsString(before),
        json.writeValueAsString(after));
  }
}
