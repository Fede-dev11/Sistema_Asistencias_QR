package ec.edu.unl.geoasist.estadisticas.persistencia;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas agregadas (R2) sobre asistencias y ausencias de sesiones CERRADAS. */
@Repository
public class StatsRepository {
  public record Counts(int attended, int absent, int justified) {
    public int computable() {
      return attended + absent;
    }
  }

  public record Student(UUID person, String cedula, String names, Counts counts) {}

  public record Group(UUID id, UUID careerId, String careerCode, String code, Counts counts) {}

  public record Absence(UUID person, String cedula, String names, String state) {}

  /** Visibilidad por fila: institucional, responsable de la actividad o director de la carrera. */
  public record Viewer(UUID account, boolean institutional, boolean responsible, boolean director) {}

  /** Una fila por (sesión, persona) con su resultado A=asistió, F=ausente, J=justificada. */
  private static final String ROWS =
      """
      SELECT s.actividad_id,s.id AS sesion_id,t.persona_id,'A' AS k
        FROM asistencias t JOIN sesiones s ON s.id=t.sesion_id WHERE s.estado='CERRADA' %1$s
      UNION ALL
      SELECT s.actividad_id,s.id,u.persona_id,CASE u.estado WHEN 'JUSTIFICADA' THEN 'J' ELSE 'F' END
        FROM ausencias u JOIN sesiones s ON s.id=u.sesion_id WHERE s.estado='CERRADA' %1$s
      """;

  private static final String COUNTS =
      "count(*) FILTER (WHERE r.k='A'),count(*) FILTER (WHERE r.k='F'),count(*) FILTER (WHERE r.k='J')";

  private final JdbcTemplate db;

  public StatsRepository(JdbcTemplate db) {
    this.db = db;
  }

  public java.math.BigDecimal threshold(UUID activity) {
    return db.queryForObject(
        "SELECT umbral_asistencia FROM actividades WHERE id=?",
        java.math.BigDecimal.class,
        activity);
  }

  private static Counts counts(java.sql.ResultSet r, int from) throws java.sql.SQLException {
    return new Counts(r.getInt(from), r.getInt(from + 1) + r.getInt(from + 2), r.getInt(from + 2));
  }

  /** Estudiantes autorizados vigentes y quienes tengan datos, paginados por nombre. */
  public List<Student> byStudent(UUID activity, int limit, long offset) {
    return db.query(
        "SELECT p.id,p.cedula_normalizada,p.nombres,"
            + COUNTS
            + " FROM ("
            + ROWS.formatted("AND s.actividad_id=?")
            + " UNION ALL SELECT e.actividad_id,NULL::uuid,e.persona_id,'N' FROM estudiantes_autorizados e"
            + " WHERE e.actividad_id=? AND e.vigente) r JOIN personas p ON p.id=r.persona_id"
            + " GROUP BY p.id ORDER BY p.nombres,p.id LIMIT ? OFFSET ?",
        (r, n) ->
            new Student(
                r.getObject(1, UUID.class), r.getString(2), r.getString(3), counts(r, 4)),
        activity,
        activity,
        activity,
        limit,
        offset);
  }

  public long countStudents(UUID activity) {
    return db.queryForObject(
        "SELECT count(*) FROM (SELECT persona_id FROM estudiantes_autorizados WHERE actividad_id=? AND vigente"
            + " UNION SELECT t.persona_id FROM asistencias t JOIN sesiones s ON s.id=t.sesion_id WHERE s.actividad_id=? AND s.estado='CERRADA'"
            + " UNION SELECT u.persona_id FROM ausencias u JOIN sesiones s ON s.id=u.sesion_id WHERE s.actividad_id=? AND s.estado='CERRADA') x",
        Long.class,
        activity,
        activity,
        activity);
  }

  public Counts totals(UUID activity) {
    return db.queryForObject(
        "SELECT " + COUNTS + " FROM (" + ROWS.formatted("AND s.actividad_id=?") + ") r",
        (r, n) -> counts(r, 1),
        activity,
        activity);
  }

  public List<Group> byParallel(Viewer v, UUID career, Instant from, Instant to) {
    var args = new ArrayList<Object>();
    String dates = dates(from, to, args);
    args.addAll(List.copyOf(args));
    String where = visibility(v, "p.carrera_id", args);
    if (career != null) {
      where += " AND p.carrera_id=?";
      args.add(career);
    }
    return db.query(
        "SELECT p.id,p.carrera_id,c.codigo,p.codigo,"
            + COUNTS
            + " FROM ("
            + ROWS.formatted(dates)
            + ") r JOIN actividades a ON a.id=r.actividad_id AND a.modalidad='CLASE'"
            + " JOIN actividad_paralelos ap ON ap.actividad_id=a.id"
            + " JOIN paralelos p ON p.id=ap.paralelo_id JOIN carreras c ON c.id=p.carrera_id"
            + " JOIN matriculas m ON m.paralelo_id=p.id AND m.persona_id=r.persona_id AND m.vigente"
            + " WHERE "
            + where
            + " GROUP BY p.id,c.codigo ORDER BY c.codigo,p.codigo",
        (r, n) ->
            new Group(
                r.getObject(1, UUID.class),
                r.getObject(2, UUID.class),
                r.getString(3),
                r.getString(4),
                counts(r, 5)),
        args.toArray());
  }

  public List<Group> byCareer(Viewer v, Instant from, Instant to) {
    var args = new ArrayList<Object>();
    String dates = dates(from, to, args);
    args.addAll(List.copyOf(args));
    String where = visibility(v, "ac.carrera_id", args);
    return db.query(
        "SELECT c.id,c.id,c.codigo,c.nombre,"
            + COUNTS
            + " FROM ("
            + ROWS.formatted(dates)
            + ") r JOIN actividades a ON a.id=r.actividad_id AND a.modalidad='CLASE'"
            + " JOIN actividad_carreras ac ON ac.actividad_id=a.id JOIN carreras c ON c.id=ac.carrera_id"
            + " WHERE "
            + where
            + " GROUP BY c.id ORDER BY c.codigo",
        (r, n) ->
            new Group(
                r.getObject(1, UUID.class),
                r.getObject(2, UUID.class),
                r.getString(3),
                r.getString(4),
                counts(r, 5)),
        args.toArray());
  }

  /** Filtro de fechas sobre el inicio de ventana; se usa dos veces en ROWS, de ahí la copia de args. */
  private static String dates(Instant from, Instant to, List<Object> args) {
    var sql = new StringBuilder();
    if (from != null) {
      sql.append(" AND s.ventana_inicio>=?");
      args.add(Timestamp.from(from));
    }
    if (to != null) {
      sql.append(" AND s.ventana_inicio<?");
      args.add(Timestamp.from(to));
    }
    return sql.toString();
  }

  private static String visibility(Viewer v, String careerColumn, List<Object> args) {
    if (v.institutional()) return "TRUE";
    var parts = new ArrayList<String>();
    if (v.responsible()) {
      parts.add("a.responsable_id=?");
      args.add(v.account());
    }
    if (v.director()) {
      parts.add(
          careerColumn
              + " IN (SELECT carrera_id FROM direcciones_carrera WHERE cuenta_id=? AND vigente)");
      args.add(v.account());
    }
    return parts.isEmpty() ? "FALSE" : "(" + String.join(" OR ", parts) + ")";
  }

  public boolean directsCareer(UUID account, UUID career) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM direcciones_carrera WHERE cuenta_id=? AND carrera_id=? AND vigente)",
            Boolean.class,
            account,
            career));
  }

  public List<Absence> absences(UUID session, int limit, long offset) {
    return db.query(
        "SELECT p.id,p.cedula_normalizada,p.nombres,u.estado FROM ausencias u JOIN personas p ON p.id=u.persona_id"
            + " WHERE u.sesion_id=? ORDER BY p.nombres,p.id LIMIT ? OFFSET ?",
        (r, n) ->
            new Absence(
                r.getObject(1, UUID.class), r.getString(2), r.getString(3), r.getString(4)),
        session,
        limit,
        offset);
  }

  public long countAbsences(UUID session) {
    return db.queryForObject(
        "SELECT count(*) FROM ausencias WHERE sesion_id=?", Long.class, session);
  }
}
