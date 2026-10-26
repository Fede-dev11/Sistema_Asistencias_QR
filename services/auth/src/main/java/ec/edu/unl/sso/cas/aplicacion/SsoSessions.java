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

/**
 * Sesión SSO (TGT). Vigente si no cerrada, {@code ultimo_uso_en + 8 h > now()} y {@code
 * vence_absoluto_en > now()}; "now" viene del {@link Clock}.
 */
@Service
public class SsoSessions {
  public record Sesion(UUID id, UUID usuarioId) {}

  private final JdbcClient db;
  private final Clock clock;
  private final Duration idle;
  private final Duration max;

  SsoSessions(
      JdbcClient db,
      Clock clock,
      @Value("${sso.session-idle}") Duration idle,
      @Value("${sso.session-max}") Duration max) {
    this.db = db;
    this.clock = clock;
    this.idle = idle;
    this.max = max;
  }

  /** Crea el TGT y devuelve el secreto para la cookie (en BD solo su hash). */
  public String crear(UUID usuarioId) {
    String secret = Secrets.random();
    OffsetDateTime now = OffsetDateTime.now(clock);
    db.sql(
            "INSERT INTO sesiones_sso(id, usuario_id, secreto_hash, creada_en, ultimo_uso_en, vence_absoluto_en) VALUES (?,?,?,?,?,?)")
        .params(UUID.randomUUID(), usuarioId, Secrets.sha256Hex(secret), now, now, now.plus(max))
        .update();
    return secret;
  }

  /** Devuelve la sesión vigente (usuario activo) y renueva su último uso. */
  public Optional<Sesion> vigente(String secret) {
    if (secret == null || secret.isBlank()) return Optional.empty();
    OffsetDateTime now = OffsetDateTime.now(clock);
    return db.sql(
            "UPDATE sesiones_sso s SET ultimo_uso_en = ? FROM usuarios u "
                + "WHERE s.secreto_hash = ? AND s.usuario_id = u.id AND u.activo AND s.cerrada_en IS NULL "
                + "AND s.ultimo_uso_en > ? AND s.vence_absoluto_en > ? RETURNING s.id, s.usuario_id")
        .params(now, Secrets.sha256Hex(secret), now.minus(idle), now)
        .query((rs, i) -> new Sesion(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)))
        .optional();
  }

  public void cerrar(String secret) {
    if (secret == null) return;
    db.sql("UPDATE sesiones_sso SET cerrada_en = ? WHERE secreto_hash = ? AND cerrada_en IS NULL")
        .params(OffsetDateTime.now(clock), Secrets.sha256Hex(secret))
        .update();
  }

  /** Cierra todas las sesiones del usuario salvo {@code except} (cambio de contraseña). */
  public void cerrarOtras(UUID usuarioId, UUID except) {
    db.sql(
            "UPDATE sesiones_sso SET cerrada_en = ? WHERE usuario_id = ? AND id <> ? AND cerrada_en IS NULL")
        .params(OffsetDateTime.now(clock), usuarioId, except)
        .update();
  }

  /** Usuario activo dueño de una sesión no cerrada (para validar tickets). */
  public Optional<UUID> usuarioActivoDe(UUID sesionId) {
    return db.sql(
            "SELECT s.usuario_id FROM sesiones_sso s JOIN usuarios u ON u.id = s.usuario_id "
                + "WHERE s.id = ? AND s.cerrada_en IS NULL AND u.activo")
        .param(sesionId)
        .query(UUID.class)
        .optional();
  }
}
