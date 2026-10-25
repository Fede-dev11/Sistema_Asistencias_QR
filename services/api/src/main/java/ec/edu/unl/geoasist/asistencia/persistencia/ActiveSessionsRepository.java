package ec.edu.unl.geoasist.asistencia.persistencia;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActiveSessionsRepository {
  private static final String FROM =
      " FROM sesiones s JOIN actividades a ON a.id=s.actividad_id LEFT JOIN espacios e ON e.id=s.espacio_id"
          + " WHERE s.estado='ACTIVA' AND a.estado='ABIERTA' AND s.ventana_inicio<=? AND s.ventana_fin>?"
          + " AND ((a.modalidad='EVENTO' AND (a.acceso='ABIERTO' OR EXISTS(SELECT 1 FROM invitados_evento ie"
          + " WHERE ie.actividad_id=a.id AND ie.persona_id=? AND ie.activo))) OR EXISTS(SELECT 1 FROM estudiantes_autorizados ea"
          + " JOIN roles_cuenta r ON r.cuenta_id=? AND r.rol='ESTUDIANTE'"
          + " WHERE ea.actividad_id=a.id AND ea.persona_id=? AND ea.vigente))";

  private final JdbcTemplate db;

  public ActiveSessionsRepository(JdbcTemplate db) {
    this.db = db;
  }

  public Map<String, Object> list(UUID account, UUID person, Instant now, int page, int size) {
    var at = Timestamp.from(now);
    Long total = db.queryForObject("SELECT count(*)" + FROM, Long.class, at, at, person, account, person);
    var items =
        db.query(
            "SELECT s.id,a.id,a.nombre,a.modalidad,e.id,e.nombre,s.metodo_verificacion,s.ventana_inicio,s.ventana_fin"
                + FROM
                + " ORDER BY s.ventana_inicio,s.id LIMIT ? OFFSET ?",
            (r, n) -> {
              var m = new LinkedHashMap<String, Object>();
              m.put("sesion_id", r.getObject(1, UUID.class));
              m.put(
                  "actividad",
                  Map.of(
                      "id", r.getObject(2, UUID.class),
                      "nombre", r.getString(3),
                      "modalidad", r.getString(4)));
              UUID space = r.getObject(5, UUID.class);
              m.put("espacio", space == null ? null : Map.of("id", space, "nombre", r.getString(6)));
              String method = r.getString(7);
              m.put("metodo_verificacion", method);
              m.put("requiere_ubicacion", method.contains("GPS"));
              m.put("inicio", r.getTimestamp(8).toInstant());
              m.put("fin", r.getTimestamp(9).toInstant());
              return m;
            },
            at,
            at,
            person,
            account,
            person,
            size,
            (long) page * size);
    return Map.of("items", items, "page", page, "size", size, "total", total);
  }
}
