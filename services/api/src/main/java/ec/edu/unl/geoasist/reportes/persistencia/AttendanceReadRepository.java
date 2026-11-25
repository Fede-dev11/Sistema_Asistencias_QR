package ec.edu.unl.geoasist.reportes.persistencia;

import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AttendanceReadRepository {
  public record Row(
      UUID attendance,
      UUID person,
      String cedula,
      String names,
      String state,
      Instant registered,
      UUID attempt,
      String code,
      String message,
      String form,
      String manualReason) {}

  private static final String PROJECTION =
      """
      SELECT a.id asistencia_id,a.persona_id,p.cedula_normalizada,a.nombres_registro nombres,
             'CONFIRMADA' estado,a.registrada_en,a.intento_id,'ASISTENCIA_CONFIRMADA' code,
             'Asistencia registrada.' mensaje,a.forma,a.razon_manual
        FROM asistencias a JOIN personas p ON p.id=a.persona_id
       WHERE a.sesion_id=?
      UNION ALL
      SELECT NULL,l.persona_id,p.cedula_normalizada,l.nombres_referencia,
             CASE WHEN l.decision='RECHAZAR' OR l.resultado='RECHAZADO'
                  THEN 'RECHAZADA' ELSE 'PENDIENTE_REVISION' END,
             l.recibido_en,l.id,
             CASE WHEN l.decision='RECHAZAR' THEN 'REVISION_RECHAZADA' ELSE l.code END,
             CASE WHEN l.decision='RECHAZAR' THEN 'La revisión responsable rechazó el intento.' ELSE l.mensaje END,
             NULL,NULL
        FROM (
          SELECT DISTINCT ON (i.persona_id)
                 i.id,i.persona_id,i.nombres_referencia,i.resultado,i.code,i.mensaje,
                 i.recibido_en,r.decision
            FROM intentos_asistencia i
            LEFT JOIN revisiones_asistencia r ON r.intento_id=i.id
           WHERE i.sesion_id=? AND i.persona_id IS NOT NULL
             AND NOT EXISTS (
               SELECT 1 FROM asistencias a2
                WHERE a2.sesion_id=i.sesion_id AND a2.persona_id=i.persona_id)
           ORDER BY i.persona_id,i.secuencia DESC
        ) l JOIN personas p ON p.id=l.persona_id
      """;

  private final JdbcTemplate db;

  public AttendanceReadRepository(JdbcTemplate db) {
    this.db = db;
  }

  public List<Row> find(UUID session, String state, int limit, long offset) {
    String sql =
        "SELECT * FROM ("
            + PROJECTION
            + ") q WHERE (CAST(? AS varchar) IS NULL OR estado=?) "
            + "ORDER BY registrada_en DESC,persona_id LIMIT ? OFFSET ?";
    return db.query(sql, this::map, session, session, state, state, limit, offset);
  }

  public long count(UUID session, String state) {
    return Objects.requireNonNull(
        db.queryForObject(
            "SELECT count(*) FROM ("
                + PROJECTION
                + ") q WHERE (CAST(? AS varchar) IS NULL OR estado=?)",
            Long.class,
            session,
            session,
            state,
            state));
  }

  public List<Row> all(UUID session, String state) {
    return db.query(
        "SELECT * FROM ("
            + PROJECTION
            + ") q WHERE (CAST(? AS varchar) IS NULL OR estado=?) ORDER BY registrada_en DESC,persona_id",
        this::map,
        session,
        session,
        state,
        state);
  }

  private Row map(java.sql.ResultSet row, int number) throws java.sql.SQLException {
    return new Row(
        row.getObject("asistencia_id", UUID.class),
        row.getObject("persona_id", UUID.class),
        row.getString("cedula_normalizada"),
        row.getString("nombres"),
        row.getString("estado"),
        row.getTimestamp("registrada_en").toInstant(),
        row.getObject("intento_id", UUID.class),
        row.getString("code"),
        row.getString("mensaje"),
        row.getString("forma"),
        row.getString("razon_manual"));
  }
}
