package ec.edu.unl.sso.compartido;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Auditoría de acciones administrativas. Los llamadores nunca pasan contraseñas ni hashes. */
@Component
public class AdminAudit {
  private final JdbcClient db;
  private final Clock clock;
  private final ObjectMapper mapper;

  AdminAudit(JdbcClient db, Clock clock, ObjectMapper mapper) {
    this.db = db;
    this.clock = clock;
    this.mapper = mapper;
  }

  public void registrar(
      UUID actor, String accion, UUID objetivo, Map<String, ?> anterior, Map<String, ?> nuevo) {
    db.sql(
            "INSERT INTO auditoria_admin(id, actor_id, accion, objetivo_id, anterior, nuevo, en) "
                + "VALUES (?,?,?,?,?::jsonb,?::jsonb,?)")
        .params(
            UUID.randomUUID(),
            actor,
            accion,
            objetivo,
            anterior == null ? null : mapper.writeValueAsString(anterior),
            nuevo == null ? null : mapper.writeValueAsString(nuevo),
            OffsetDateTime.now(clock))
        .update();
  }
}
