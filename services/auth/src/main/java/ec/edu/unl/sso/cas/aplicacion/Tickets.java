package ec.edu.unl.sso.cas.aplicacion;

import ec.edu.unl.sso.compartido.Secrets;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Tickets de servicio (ST): {@code ST-} + 32 bytes aleatorios; en BD solo su hash. */
@Service
public class Tickets {
  public record Consumido(UUID sesionId, UUID servicioId, String service) {}

  private final JdbcClient db;
  private final Clock clock;
  private final Duration ttl;

  Tickets(JdbcClient db, Clock clock, @Value("${sso.ticket-ttl}") Duration ttl) {
    this.db = db;
    this.clock = clock;
    this.ttl = ttl;
  }

  public String emitir(UUID sesionId, UUID servicioId, String service) {
    String ticket = "ST-" + Secrets.random();
    OffsetDateTime now = OffsetDateTime.now(clock);
    db.sql(
            "INSERT INTO tickets(id, hash, sesion_id, servicio_id, service, emitido_en, vence_en) VALUES (?,?,?,?,?,?,?)")
        .params(
            UUID.randomUUID(),
            Secrets.sha256Hex(ticket),
            sesionId,
            servicioId,
            service,
            now,
            now.plus(ttl))
        .update();
    return ticket;
  }

  /**
   * Consumo atómico de un solo uso: {@code UPDATE ... WHERE usado_en IS NULL AND vence_en > ahora
   * RETURNING}; "ahora" es el Clock inyectado, no now() de la BD. Dos validaciones simultáneas
   * nunca tienen éxito a la vez.
   */
  public Optional<Consumido> consumir(String ticket) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    return db.sql(
            "UPDATE tickets SET usado_en = ? WHERE hash = ? AND usado_en IS NULL AND vence_en > ? "
                + "RETURNING sesion_id, servicio_id, service")
        .params(now, Secrets.sha256Hex(ticket), now)
        .query(
            (rs, i) ->
                new Consumido(
                    rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3)))
        .optional();
  }
}
