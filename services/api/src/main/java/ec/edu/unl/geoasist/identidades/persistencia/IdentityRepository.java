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

  public void createSession(
      UUID id, UUID account, String hash, Instant now, Instant expiry, Instant loginOriginal) {
    db.update(
        "INSERT INTO sesiones_acceso(id,cuenta_id,secreto_hash,creada_en,vence_en,login_original_en) VALUES (?,?,?,?,?,?)",
        id,
        account,
        hash,
        java.sql.Timestamp.from(now),
        java.sql.Timestamp.from(expiry),
        java.sql.Timestamp.from(loginOriginal));
  }

  public Instant loginOriginal(UUID session) {
    return db.queryForObject(
            "SELECT login_original_en FROM sesiones_acceso WHERE id=?",
            java.sql.Timestamp.class,
            session)
        .toInstant();
  }

  public boolean mustChangePassword(UUID account) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT debe_cambiar_password FROM cuentas WHERE id=?", Boolean.class, account));
  }

  public Map<String, Object> accountRow(UUID account) {
    return db.queryForMap(
        "SELECT c.identificador,c.proveedor,c.password_hash,c.persona_id,p.nombres FROM cuentas c JOIN personas p ON p.id=c.persona_id WHERE c.id=?",
        account);
  }

  public List<Map<String, Object>> parallels(UUID account, UUID person) {
    return db.queryForList(
        "SELECT p.id paralelo_id,p.codigo,c.id carrera_id,c.codigo carrera_codigo,c.nombre carrera_nombre FROM paralelos p JOIN carreras c ON c.id=p.carrera_id "
            + "WHERE p.id IN (SELECT paralelo_id FROM matriculas WHERE persona_id=? AND vigente "
            + "UNION SELECT paralelo_id FROM docentes_paralelo WHERE cuenta_id=? AND vigente) ORDER BY c.codigo,p.codigo",
        person,
        account);
  }

  public Optional<UserSession> session(String hash, Instant now) {
    record SessionRow(UUID id, UUID account, UUID person, boolean must) {}
    var rows =
        db.query(
            "SELECT s.id,c.id cuenta_id,c.persona_id,c.debe_cambiar_password FROM sesiones_acceso s JOIN cuentas c ON c.id=s.cuenta_id WHERE s.secreto_hash=? AND s.revocada_en IS NULL AND s.vence_en>? AND c.activa",
            (r, n) ->
                new SessionRow(
                    r.getObject("id", UUID.class),
                    r.getObject("cuenta_id", UUID.class),
                    r.getObject("persona_id", UUID.class),
                    r.getBoolean("debe_cambiar_password")),
            hash,
            java.sql.Timestamp.from(now));
    if (rows.isEmpty()) return Optional.empty();
    SessionRow row = rows.getFirst();
    return Optional.of(
        new UserSession(row.id(), row.account(), row.person(), roles(row.account()), row.must()));
  }

  public void revoke(UUID id, Instant now) {
    db.update(
        "UPDATE sesiones_acceso SET revocada_en=? WHERE id=?", java.sql.Timestamp.from(now), id);
  }
}
