package ec.edu.unl.geoasist.compartido.persistencia;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
public class AttendanceLocks {
  private final JdbcTemplate db;

  public AttendanceLocks(JdbcTemplate db) {
    this.db = db;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void activity(UUID id, boolean exclusive) {
    if (db.queryForList(
            "SELECT id FROM actividades WHERE id=? " + (exclusive ? "FOR UPDATE" : "FOR SHARE"), id)
        .isEmpty()) throw ApiFailure.missing();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void session(UUID id) {
    if (db.queryForList("SELECT id FROM sesiones WHERE id=? FOR UPDATE", id).isEmpty())
      throw ApiFailure.missing();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void person(UUID id) {
    if (db.queryForList("SELECT id FROM personas WHERE id=? FOR UPDATE", id).isEmpty())
      throw ApiFailure.missing();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void guestStart(UUID id) {
    if (db.queryForList("SELECT id FROM inicios_invitado WHERE id=? FOR UPDATE", id).isEmpty())
      throw ApiFailure.missing();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void attempt(UUID id) {
    if (db.queryForList("SELECT id FROM intentos_asistencia WHERE id=? FOR UPDATE", id).isEmpty())
      throw ApiFailure.missing();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void allSessions(UUID activity) {
    db.queryForList(
        "SELECT id FROM sesiones WHERE actividad_id=? ORDER BY id FOR UPDATE", activity);
  }
}
