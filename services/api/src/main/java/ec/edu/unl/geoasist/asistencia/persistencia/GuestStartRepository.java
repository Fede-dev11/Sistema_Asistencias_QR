package ec.edu.unl.geoasist.asistencia.persistencia;

import ec.edu.unl.geoasist.compartido.aplicacion.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class GuestStartRepository {
  public record Start(
      UUID id,
      UUID session,
      Instant began,
      Instant deadline,
      long generation,
      String state,
      UUID bound,
      UUID consumed,
      UUID receipt) {}

  private final JdbcTemplate db;

  public GuestStartRepository(JdbcTemplate db) {
    this.db = db;
  }

  private final RowMapper<Start> mapper =
      (r, n) ->
          new Start(
              r.getObject("id", UUID.class),
              r.getObject("sesion_id", UUID.class),
              r.getTimestamp("inicio_en").toInstant(),
              r.getTimestamp("vence_en").toInstant(),
              r.getLong("acceso_generacion"),
              r.getString("estado"),
              r.getObject("persona_vinculada_id", UUID.class),
              r.getObject("persona_consumida_id", UUID.class),
              r.getObject("recibo_asistencia_id", UUID.class));

  public Start bySecret(String secret) {
    return db
        .query(
            "SELECT * FROM inicios_invitado WHERE secreto_hash=?",
            mapper,
            OpaqueSecrets.hash(secret))
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new ApiFailure(
                    401,
                    "NO_AUTENTICADO",
                    "El formulario no está disponible. Escanea un QR vigente."));
  }

  public Start find(UUID id) {
    return db.query("SELECT * FROM inicios_invitado WHERE id=?", mapper, id).stream()
        .findFirst()
        .orElseThrow(ApiFailure::missing);
  }

  public void create(
      UUID id, UUID session, String hash, Instant now, int duration, long generation) {
    db.update(
        "INSERT INTO inicios_invitado(id,sesion_id,secreto_hash,inicio_en,vence_en,tolerancia_segundos,acceso_generacion) VALUES (?,?,?,?,?,?,?)",
        id,
        session,
        hash,
        java.sql.Timestamp.from(now),
        java.sql.Timestamp.from(now.plusSeconds(duration)),
        duration,
        generation);
  }

  public void bind(UUID id, UUID person) {
    db.update(
        "UPDATE inicios_invitado SET persona_vinculada_id=? WHERE id=? AND persona_vinculada_id IS NULL",
        person,
        id);
  }

  public void consume(UUID id, UUID person, UUID attendance, Instant now) {
    db.update(
        "UPDATE inicios_invitado SET estado='CONSUMIDO',persona_consumida_id=?,recibo_asistencia_id=?,consumido_en=COALESCE(consumido_en,?) WHERE id=?",
        person,
        attendance,
        java.sql.Timestamp.from(now),
        id);
  }
}
