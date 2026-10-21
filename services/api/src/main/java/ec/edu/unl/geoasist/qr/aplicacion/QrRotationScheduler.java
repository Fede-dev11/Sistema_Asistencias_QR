package ec.edu.unl.geoasist.qr.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "geoasist.scheduling-enabled", matchIfMissing = true)
public class QrRotationScheduler {
  private final JdbcTemplate db;
  private final IssueSessionQr issuer;

  public QrRotationScheduler(JdbcTemplate db, IssueSessionQr issuer) {
    this.db = db;
    this.issuer = issuer;
  }

  @Scheduled(fixedDelay = 1000)
  public void rotate() {
    for (UUID id :
        db.queryForList(
            "SELECT s.id FROM sesiones s JOIN actividades a ON a.id=s.actividad_id WHERE s.estado='ACTIVA' AND a.estado='ABIERTA'",
            UUID.class)) {
      try {
        issuer.rotate(id);
      } catch (ApiFailure e) {
        if (e.status() != 404) throw e;
      }
    }
    issuer.expireCache();
  }
}
