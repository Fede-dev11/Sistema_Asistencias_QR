package ec.edu.unl.geoasist.compartido.aplicacion;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regla única de ausencias (FR-001): la invocan todos los cierres. Vive en compartido porque
 * sesiones y actividades la llaman y asistencia ya depende de ellas (sin ciclos entre módulos).
 */
@Component
public class RecordAbsences {
  private static final String SQL =
      """
      INSERT INTO ausencias(sesion_id,persona_id,registrada_en)
      SELECT s.id,e.persona_id,?
      FROM sesiones s
      JOIN actividades a ON a.id=s.actividad_id AND a.modalidad='CLASE'
      JOIN estudiantes_autorizados e ON e.actividad_id=a.id AND e.vigente
      WHERE s.estado='CERRADA' AND s.id=?
        AND NOT EXISTS(SELECT 1 FROM asistencias t WHERE t.sesion_id=s.id AND t.persona_id=e.persona_id)
      ON CONFLICT DO NOTHING
      """;

  private final JdbcTemplate db;
  private final Clock clock;

  public RecordAbsences(JdbcTemplate db, Clock clock) {
    this.db = db;
    this.clock = clock;
  }

  /** Dentro de la transacción del cierre; idempotente por PK(sesion_id, persona_id). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void record(UUID session) {
    db.update(SQL, Timestamp.from(clock.instant()), session);
  }
}
