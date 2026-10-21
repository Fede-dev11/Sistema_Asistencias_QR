package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.unl.geoasist.asistencia.aplicacion.TransientSecretCleanup;
import ec.edu.unl.geoasist.soporte.PostgresFixture;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TransientSecretCleanupTest extends PostgresFixture {
  @Autowired TransientSecretCleanup cleanup;

  @Test
  void removesExpiredSecretsButPreservesAttendanceAndAttemptEvidence() throws Exception {
    String cedula = fictionalId();
    var event = guests(41, cedula);
    String secret = open(event).get("registro_invitado_token").asText();
    guestSubmit(secret, cedula, UUID.randomUUID())
        .andExpect(result -> assertEquals(201, result.getResponse().getStatus()));

    UUID start =
        db.queryForObject(
            "SELECT id FROM inicios_invitado WHERE sesion_id=?", UUID.class, event.session());
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM idempotencias WHERE inicio_id=?", Integer.class, start));

    clock.set(clock.instant().plusSeconds(26 * 60 * 60));
    var result = cleanup.cleanup();

    assertTrue(result.guestStarts() >= 1);
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM inicios_invitado WHERE id=?", Integer.class, start));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM idempotencias WHERE inicio_id=?", Integer.class, start));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE sesion_id=?", Integer.class, event.session()));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM intentos_asistencia WHERE sesion_id=?",
            Integer.class,
            event.session()));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM intentos_asistencia WHERE sesion_id=? AND inicio_invitado_id IS NULL",
            Integer.class,
            event.session()));
  }
}
