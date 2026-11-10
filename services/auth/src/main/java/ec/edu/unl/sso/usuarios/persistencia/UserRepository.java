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

  /** Búsqueda por fragmento de correo o cédula (vacío = todos), paginada. */
  public List<Usuario> buscar(String q, int limit, int offset) {
    return db.sql(SELECT + "WHERE correo ILIKE ? OR cedula LIKE ? ORDER BY correo LIMIT ? OFFSET ?")
        .params(like(q), like(q), limit, offset)
        .query(this::map)
        .list();
  }

  public int contar(String q) {
    return db.sql("SELECT count(*) FROM usuarios WHERE correo ILIKE ? OR cedula LIKE ?")
        .params(like(q), like(q))
        .query(Integer.class)
        .single();
  }

  private static String like(String q) {
    String v =
        q == null ? "" : q.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + v + "%";
  }

  public Optional<Usuario> porCedula(String cedula) {
    return db.sql(SELECT + "WHERE cedula = ?").param(cedula).query(this::map).optional();
  }

  /** Reemplaza datos editables, roles y carreras. */
  public void actualizar(Usuario u) {
    db.sql(
            "UPDATE usuarios SET correo = ?, cedula = ?, nombres = ?, actualizado_en = ? WHERE id = ?")
        .params(u.correo(), u.cedula(), u.nombres(), OffsetDateTime.now(clock), u.id())
        .update();
    db.sql("DELETE FROM usuario_roles WHERE usuario_id = ?").param(u.id()).update();
    db.sql("DELETE FROM usuario_carreras WHERE usuario_id = ?").param(u.id()).update();
    u.roles()
        .forEach(r -> db.sql("INSERT INTO usuario_roles VALUES (?,?)").params(u.id(), r).update());
    u.carreras()
        .forEach(
            c -> db.sql("INSERT INTO usuario_carreras VALUES (?,?)").params(u.id(), c).update());
  }

  public void fijarActivo(UUID id, boolean activo) {
    db.sql("UPDATE usuarios SET activo = ?, actualizado_en = ? WHERE id = ?")
        .params(activo, OffsetDateTime.now(clock), id)
        .update();
  }

  /** Contraseña temporal: obliga a cambiarla en el siguiente ingreso. */
  public void fijarPasswordTemporal(UUID id, String hash) {
    db.sql(
            "UPDATE usuarios SET password_hash = ?, debe_cambiar_password = true, actualizado_en = ? WHERE id = ?")
        .params(hash, OffsetDateTime.now(clock), id)
        .update();
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
