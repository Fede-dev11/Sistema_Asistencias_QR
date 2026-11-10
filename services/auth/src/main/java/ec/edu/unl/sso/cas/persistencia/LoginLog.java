package ec.edu.unl.sso.cas.persistencia;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** registro_ingresos: nunca contraseñas ni tickets (FR-006). */
@Repository
public class LoginLog {
  public enum Resultado {
    OK,
    CREDENCIALES,
    INACTIVO,
    BLOQUEADO
  }

  private final JdbcClient db;
  private final Clock clock;

  LoginLog(JdbcClient db, Clock clock) {
    this.db = db;
    this.clock = clock;
  }

  public void registrar(String correo, Resultado resultado, String origen, UUID servicioId) {
    String c = correo.length() > 254 ? correo.substring(0, 254) : correo;
    String o = origen.length() > 64 ? origen.substring(0, 64) : origen;
    db.sql(
            "INSERT INTO registro_ingresos(id, correo, resultado, origen, servicio_id, en) VALUES (?,?,?,?,?,?)")
        .params(UUID.randomUUID(), c, resultado.name(), o, servicioId, OffsetDateTime.now(clock))
        .update();
  }
}
