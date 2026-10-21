package ec.edu.unl.geoasist.conservacion.aplicacion;

import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La verificación de archivos e inventario se hace por herramientas operativas, sin endpoint
 * público.
 */
@Service
public class CopySanitizationService {
  private final JdbcTemplate db;

  public CopySanitizationService(JdbcTemplate db) {
    this.db = db;
  }

  @Transactional
  public int completeVerifiedTreatments() {
    return Objects.requireNonNull(
        db.queryForObject("SELECT completar_tratamientos()", Integer.class));
  }

  public long pendingTreatments() {
    return Objects.requireNonNull(
        db.queryForObject(
            "SELECT count(*) FROM tratamientos_conservacion WHERE fase<>'COMPLETADO'", Long.class));
  }
}
