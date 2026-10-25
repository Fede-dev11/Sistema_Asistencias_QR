package ec.edu.unl.geoasist.espacios.persistencia;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class SpaceRepository {
  private static final String COLUMNS =
      "id,codigo,nombre,tipo,latitud,longitud,radio_defecto_m,precision_defecto_m,creado_por";
  private static final String MATCH =
      " WHERE codigo_normalizado LIKE ? ESCAPE '\\' OR nombre ILIKE ? ESCAPE '\\'";
  private final JdbcTemplate db;
  private final Clock clock;
  private final ObjectMapper json;

  public SpaceRepository(JdbcTemplate db, Clock clock, ObjectMapper json) {
    this.db = db;
    this.clock = clock;
    this.json = json;
  }

  public record Row(
      UUID id,
      String code,
      String name,
      String type,
      Double latitude,
      Double longitude,
      double radius,
      double precision,
      UUID creator) {}

  private static Row row(java.sql.ResultSet r, int n) throws java.sql.SQLException {
    return new Row(
        r.getObject("id", UUID.class),
        r.getString("codigo"),
        r.getString("nombre"),
        r.getString("tipo"),
        (Double) r.getObject("latitud"),
        (Double) r.getObject("longitud"),
        r.getDouble("radio_defecto_m"),
        r.getDouble("precision_defecto_m"),
        r.getObject("creado_por", UUID.class));
  }

  public Optional<Row> find(UUID id) {
    return db
        .query("SELECT " + COLUMNS + " FROM espacios WHERE id=?", SpaceRepository::row, id)
        .stream()
        .findFirst();
  }

  public Optional<UUID> idByCode(String normalized) {
    return db
        .queryForList("SELECT id FROM espacios WHERE codigo_normalizado=?", UUID.class, normalized)
        .stream()
        .findFirst();
  }

  public void insert(Row v, String normalized) {
    db.update(
        "INSERT INTO espacios(id,codigo,codigo_normalizado,nombre,tipo,latitud,longitud,radio_defecto_m,precision_defecto_m,creado_por) VALUES (?,?,?,?,?,?,?,?,?,?)",
        v.id(),
        v.code(),
        normalized,
        v.name(),
        v.type(),
        v.latitude(),
        v.longitude(),
        v.radius(),
        v.precision(),
        v.creator());
  }

  public void update(Row v) {
    db.update(
        "UPDATE espacios SET nombre=?,tipo=?,latitud=?,longitud=?,radio_defecto_m=?,precision_defecto_m=? WHERE id=?",
        v.name(),
        v.type(),
        v.latitude(),
        v.longitude(),
        v.radius(),
        v.precision(),
        v.id());
  }

  public long count(String codeLike, String nameLike) {
    return db.queryForObject(
        "SELECT count(*) FROM espacios" + MATCH, Long.class, codeLike, nameLike);
  }

  public List<Row> search(String codeLike, String nameLike, int size, long offset) {
    return db.query(
        "SELECT " + COLUMNS + " FROM espacios" + MATCH
            + " ORDER BY codigo_normalizado,id LIMIT ? OFFSET ?",
        SpaceRepository::row,
        codeLike,
        nameLike,
        size,
        offset);
  }

  public void audit(
      UUID space, UUID actor, String action, Map<String, ?> before, Map<String, ?> after) {
    db.update(
        "INSERT INTO auditoria_espacios VALUES (?,?,?,?,?,?,?::jsonb,?::jsonb)",
        UUID.randomUUID(),
        space,
        actor,
        action,
        Timestamp.from(clock.instant()),
        ec.edu.unl.geoasist.compartido.aplicacion.RequestCorrelation.id(),
        json.writeValueAsString(before),
        json.writeValueAsString(after));
  }
}
