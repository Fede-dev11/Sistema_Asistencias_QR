package ec.edu.unl.geoasist.identidades.persistencia;

import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdentityRepository {
  private final JdbcTemplate db;

  public IdentityRepository(JdbcTemplate db) {
    this.db = db;
  }

  public Optional<Map<String, Object>> account(String email) {
    return db
        .queryForList(
            "SELECT id,persona_id,proveedor,password_hash,activa FROM cuentas WHERE identificador=?",
            email.toLowerCase(Locale.ROOT))
        .stream()
        .findFirst();
  }

  public Set<String> roles(UUID id) {
    return new HashSet<>(
        db.queryForList("SELECT rol FROM roles_cuenta WHERE cuenta_id=?", String.class, id));
  }

  public void createSession(UUID id, UUID account, String hash, Instant now, Instant expiry) {
    db.update(
        "INSERT INTO sesiones_acceso VALUES (?,?,?,?,?,NULL)",
        id,
        account,
        hash,
        java.sql.Timestamp.from(now),
        java.sql.Timestamp.from(expiry));
  }

  public Optional<UserSession> session(String hash, Instant now) {
    record SessionRow(UUID id, UUID account, UUID person) {}
    var rows =
        db.query(
            "SELECT s.id,c.id cuenta_id,c.persona_id FROM sesiones_acceso s JOIN cuentas c ON c.id=s.cuenta_id WHERE s.secreto_hash=? AND s.revocada_en IS NULL AND s.vence_en>? AND c.activa",
            (r, n) ->
                new SessionRow(
                    r.getObject("id", UUID.class),
                    r.getObject("cuenta_id", UUID.class),
                    r.getObject("persona_id", UUID.class)),
            hash,
            java.sql.Timestamp.from(now));
    if (rows.isEmpty()) return Optional.empty();
    SessionRow row = rows.getFirst();
    return Optional.of(
        new UserSession(row.id(), row.account(), row.person(), roles(row.account())));
  }

  public void revoke(UUID id, Instant now) {
    db.update(
        "UPDATE sesiones_acceso SET revocada_en=? WHERE id=?", java.sql.Timestamp.from(now), id);
  }
}
