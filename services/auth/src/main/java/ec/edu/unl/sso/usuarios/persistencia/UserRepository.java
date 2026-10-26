package ec.edu.unl.sso.usuarios.persistencia;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  public record Usuario(
      UUID id,
      String correo,
      String cedula,
      String nombres,
      String passwordHash,
      boolean activo,
      boolean debeCambiarPassword,
      List<String> roles,
      List<String> carreras) {}

  private static final String SELECT =
      "SELECT id, correo, cedula, nombres, password_hash, activo, debe_cambiar_password FROM usuarios ";

  private final JdbcClient db;
  private final Clock clock;

  UserRepository(JdbcClient db, Clock clock) {
    this.db = db;
    this.clock = clock;
  }

  /** El correo se busca en minúsculas (data-model: único, minúsculas). */
  public Optional<Usuario> porCorreo(String correo) {
    return db.sql(SELECT + "WHERE correo = ?")
        .param(correo.toLowerCase())
        .query(this::map)
        .optional();
  }

  public Optional<Usuario> porId(UUID id) {
    return db.sql(SELECT + "WHERE id = ?").param(id).query(this::map).optional();
  }

  public void cambiarPassword(UUID id, String hash) {
    db.sql(
            "UPDATE usuarios SET password_hash = ?, debe_cambiar_password = false, actualizado_en = ? WHERE id = ?")
        .param(hash)
        .param(OffsetDateTime.now(clock))
        .param(id)
        .update();
  }

  /** Alta (usada por la semilla de desarrollo y la futura administración). */
  public void insertar(Usuario u) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    int n =
        db.sql(
                "INSERT INTO usuarios(id, correo, cedula, nombres, password_hash, activo, debe_cambiar_password, creado_en, actualizado_en) "
                    + "VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT (correo) DO NOTHING")
            .params(
                u.id(),
                u.correo(),
                u.cedula(),
                u.nombres(),
                u.passwordHash(),
                u.activo(),
                u.debeCambiarPassword(),
                now,
                now)
            .update();
    if (n == 0) return;
    u.roles()
        .forEach(
            r ->
                db.sql("INSERT INTO usuario_roles VALUES (?,?) ON CONFLICT DO NOTHING")
                    .params(u.id(), r)
                    .update());
    u.carreras()
        .forEach(
            c ->
                db.sql("INSERT INTO usuario_carreras VALUES (?,?) ON CONFLICT DO NOTHING")
                    .params(u.id(), c)
                    .update());
  }

  private Usuario map(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
    UUID id = rs.getObject("id", UUID.class);
    return new Usuario(
        id,
        rs.getString("correo"),
        rs.getString("cedula"),
        rs.getString("nombres"),
        rs.getString("password_hash"),
        rs.getBoolean("activo"),
        rs.getBoolean("debe_cambiar_password"),
        db.sql("SELECT rol FROM usuario_roles WHERE usuario_id = ? ORDER BY rol")
            .param(id)
            .query(String.class)
            .list(),
        db.sql("SELECT carrera_codigo FROM usuario_carreras WHERE usuario_id = ? ORDER BY 1")
            .param(id)
            .query(String.class)
            .list());
  }
}
