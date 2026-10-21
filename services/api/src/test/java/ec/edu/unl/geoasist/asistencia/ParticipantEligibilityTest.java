package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ParticipantEligibilityTest extends PostgresFixture {
  @Test
  void classesExcludeUnauthorizedAccountsAndGuests() throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER), student = token("otro-estudiante@example.test");
    start(ids[1], owner);
    String qr = qr(ids[1], owner);
    api.perform(
            post("/api/v1/attendance/attempts")
                .header("Authorization", "Bearer " + student)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("qr_token", qr))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("code").value("NO_AUTORIZADO"));
    api.perform(
            post("/api/v1/guest-registrations")
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("qr_token", qr))))
        .andExpect(status().isForbidden());
  }

  @Test
  void anyAccountEvenInactiveMustUseCredentialsAndUnknownIdsCannotSelfRegister() throws Exception {
    String allowed = fictionalId(), unknown = fictionalId();
    var event = guests(120, allowed, "0000000003", "0000000008");
    for (String registered : List.of("0000000003", "0000000008"))
      guestSubmit(
              open(event).get("registro_invitado_token").asText(), registered, UUID.randomUUID())
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("code").value("USAR_CREDENCIALES"));
    guestSubmit(open(event).get("registro_invitado_token").asText(), unknown, UUID.randomUUID())
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("code").value("NO_AUTORIZADO"));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM personas WHERE cedula_normalizada=?", Integer.class, unknown));
    String student = token(ScenarioFixtures.STUDENT);
    api.perform(
            post("/api/v1/attendance/attempts")
                .header("Authorization", "Bearer " + student)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("qr_token", event.qr()))))
        .andExpect(status().isCreated());
  }
}
