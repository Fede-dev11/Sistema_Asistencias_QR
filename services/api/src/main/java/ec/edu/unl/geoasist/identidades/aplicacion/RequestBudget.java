package ec.edu.unl.geoasist.identidades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.aplicacion.OpaqueSecrets;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class RequestBudget {
  private final JdbcTemplate db;
  private final OpaqueSecrets secrets;
  private final Clock clock;
  private final int multiplier;

  public RequestBudget(
      JdbcTemplate db,
      OpaqueSecrets secrets,
      Clock clock,
      @Value("${geoasist.rate-limit-multiplier:1}") int multiplier) {
    this.db = db;
    this.secrets = secrets;
    this.clock = clock;
    this.multiplier = multiplier;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiFailure.class)
  public void check(String scope, int limit) {
    Instant now = clock.instant();
    long window = now.getEpochSecond() / 60;
    Integer count =
        db.queryForObject(
            "INSERT INTO contadores_limite VALUES (?,?,1,?) ON CONFLICT (ambito,ventana) DO UPDATE SET conteo=contadores_limite.conteo+1 RETURNING conteo",
            Integer.class,
            secrets.hmac(scope),
            window,
            java.sql.Timestamp.from(now.plusSeconds(120)));
    if (count != null && count > limit * multiplier)
      throw new ApiFailure(
          429, "LIMITE_SOLICITUDES", "Demasiadas solicitudes. Reintenta en un minuto.");
  }
}
