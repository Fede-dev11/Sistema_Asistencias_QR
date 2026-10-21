package ec.edu.unl.geoasist.conservacion.aplicacion;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Solo celdas ya selladas por construir_estadisticas dentro de la transacción de conservación. */
@Service
public class BuildAnonymousStatistics {
  public record Cell(int year, String mode, long participants) {}

  private final JdbcTemplate db;

  public BuildAnonymousStatistics(JdbcTemplate db) {
    this.db = db;
  }

  @Transactional(readOnly = true)
  public List<Cell> sealedCells() {
    return db.query(
        "SELECT anio,modalidad,participantes FROM estadisticas_anonimas ORDER BY anio,modalidad",
        (row, number) -> new Cell(row.getInt(1), row.getString(2), row.getLong(3)));
  }
}
