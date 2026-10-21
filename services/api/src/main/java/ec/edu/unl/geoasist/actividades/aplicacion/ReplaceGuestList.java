package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplaceGuestList {
  public record Entry(String cedula, String nombres) {}

  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityAuthorization scope;
  private final AttendanceLocks locks;
  private final ActivityAuditWriter audit;

  public ReplaceGuestList(
      JdbcTemplate db,
      Clock clock,
      ActivityAuthorization scope,
      AttendanceLocks locks,
      ActivityAuditWriter audit) {
    this.db = db;
    this.clock = clock;
    this.scope = scope;
    this.locks = locks;
    this.audit = audit;
  }

  @Transactional
  public Map<String, Object> replace(UserSession user, UUID id, List<Entry> entries) {
    scope.write(user, id);
    locks.activity(id, true);
    var activity = scope.write(user, id);
    if (!activity.mode().equals("EVENTO") || !activity.state().equals("ABIERTA"))
      throw ApiFailure.conflict();
    if (entries.size() > 1000) throw ApiFailure.invalid();
    Set<String> seen = new HashSet<>();
    List<Integer> invalid = new ArrayList<>();
    for (int i = 0; i < entries.size(); i++) {
      Entry e = entries.get(i);
      if (e == null
          || e.cedula() == null
          || !e.cedula().matches("[0-9]{10}")
          || e.nombres() == null
          || e.nombres().isBlank()
          || e.nombres().codePointCount(0, e.nombres().length()) > 200
          || !seen.add(e.cedula())) invalid.add(i);
    }
    if (!invalid.isEmpty())
      throw new ApiFailure(
          400,
          "SOLICITUD_INVALIDA",
          "No se reemplazó el listado.",
          "Filas inválidas o duplicadas (índice desde cero): " + invalid);
    Integer before =
        db.queryForObject(
            "SELECT count(*) FROM invitados_evento WHERE actividad_id=?", Integer.class, id);
    Long version =
        db.queryForObject(
            "SELECT COALESCE(max(version),0)+1 FROM invitados_evento WHERE actividad_id=?",
            Long.class,
            id);
    db.update("DELETE FROM invitados_evento WHERE actividad_id=?", id);
    for (Entry e :
        entries.stream().sorted(java.util.Comparator.comparing(Entry::cedula)).toList()) {
      db.update(
          "INSERT INTO personas VALUES (?,?,?,'EXTERNA') ON CONFLICT(cedula_normalizada) DO NOTHING",
          UUID.randomUUID(),
          e.cedula(),
          e.nombres());
      UUID person =
          db.queryForObject(
              "SELECT id FROM personas WHERE cedula_normalizada=?", UUID.class, e.cedula());
      locks.person(person);
      db.update(
          "INSERT INTO invitados_evento VALUES (?,?,?,true,?,?,?)",
          id,
          person,
          e.nombres(),
          user.accountId(),
          java.sql.Timestamp.from(clock.instant()),
          version);
    }
    audit.write(
        id,
        user.accountId(),
        "REEMPLAZAR_INVITADOS",
        null,
        Map.of("cantidad_invitados", before),
        Map.of("cantidad_invitados", entries.size()));
    return Map.of("activity_id", id, "total", entries.size(), "actualizada_en", clock.instant());
  }
}
