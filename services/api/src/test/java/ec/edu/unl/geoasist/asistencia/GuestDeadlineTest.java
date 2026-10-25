package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GuestDeadlineTest extends PostgresFixture {
  @Test
  void formKeepsItsOriginalDeadlineAfterQrRotationAndConfigurationChange() throws Exception {
    for (int duration : new int[] {41, 120, 301}) {
      String cedula = fictionalId();
      var event = guests(duration, cedula);
      var opened = open(event);
      Instant began = Instant.parse(opened.get("inicio_en").asText()),
          deadline = Instant.parse(opened.get("vence_en").asText());
      assertEquals(began.plusSeconds(duration), deadline);
      api.perform(
              put("/api/v1/activities/" + event.activity() + "/guest-settings")
                  .header("Authorization", "Bearer " + event.owner())
                  .contentType("application/json")
                  .content(
                      json.writeValueAsString(
                          Map.of(
                              "habilitado", true, "tolerancia_invitado_segundos", duration + 600))))
          .andExpect(status().isOk());
      clock.set(began.plusSeconds(30));
      qr(event.session(), event.owner());
      clock.set(deadline.minusNanos(1));
      guestSubmit(opened.get("registro_invitado_token").asText(), cedula, UUID.randomUUID())
          .andExpect(status().isCreated());
      clock.set(deadline);
      guestSubmit(opened.get("registro_invitado_token").asText(), cedula, UUID.randomUUID())
          .andExpect(status().isGone())
          .andExpect(jsonPath("code").value("FORMULARIO_EXPIRADO"));
    }
  }

  @Test
  void expiredQrCannotOpenAFormAndClosureCancelsAnOpenForm() throws Exception {
    String cedula = fictionalId();
    var event = guests(120, cedula);
    var opened = open(event);
    clock.set(clock.instant().plusSeconds(40));
    api.perform(
            post("/api/v1/guest-registrations")
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("qr_token", event.qr()))))
        .andExpect(status().isUnprocessableEntity());
    api.perform(
            post("/api/v1/sessions/" + event.session() + "/close")
                .header("Authorization", "Bearer " + event.owner())
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(status().isOk());
    guestSubmit(opened.get("registro_invitado_token").asText(), cedula, UUID.randomUUID())
        .andExpect(status().isConflict());
  }

  @Test
  void disablingThenReenablingDoesNotReviveAnOldForm() throws Exception {
    String cedula = fictionalId();
    var event = guests(120, cedula);
    String secret = open(event).get("registro_invitado_token").asText();
    for (boolean enabled : new boolean[] {false, true})
      api.perform(
              put("/api/v1/activities/" + event.activity() + "/guest-settings")
                  .header("Authorization", "Bearer " + event.owner())
                  .contentType("application/json")
                  .content(
                      json.writeValueAsString(
                          Map.of("habilitado", enabled, "tolerancia_invitado_segundos", 120))))
          .andExpect(status().isOk());
    guestSubmit(secret, cedula, UUID.randomUUID()).andExpect(status().isForbidden());
  }

  @Test
  void ordinaryTokensUseServerReceptionAndNeverTheGuestAllowance() throws Exception {
    var event = guests(301, fictionalId());
    Instant initial = clock.instant();
    String student = token(ScenarioFixtures.STUDENT);
    clock.set(initial.plusSeconds(39));
    api.perform(
            post("/api/v1/attendance/attempts")
                .header("Authorization", "Bearer " + student)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("qr_token", event.qr(), "dispositivo_id", UUID.randomUUID()))))
        .andExpect(status().isCreated());
    clock.set(initial.plusSeconds(40));
    api.perform(
            post("/api/v1/attendance/attempts")
                .header("Authorization", "Bearer " + student)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("qr_token", event.qr(), "dispositivo_id", UUID.randomUUID()))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("code").value("TOKEN_VENCIDO"));
  }
}
