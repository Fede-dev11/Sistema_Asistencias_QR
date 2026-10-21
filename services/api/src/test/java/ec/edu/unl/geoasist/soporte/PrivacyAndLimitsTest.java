package ec.edu.unl.geoasist.soporte;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "geoasist.rate-limit-multiplier=1")
@ExtendWith(OutputCaptureExtension.class)
class PrivacyAndLimitsTest extends PostgresFixture {
  @Test
  void leadingZeroMinimalReceiptStrictInputAndRealRateLimitHaveNoRawSecrets(CapturedOutput logs)
      throws Exception {
    // Ventana propia: otras suites usan un multiplicador de laboratorio y el mismo PostgreSQL.
    clock.set(clock.instant().plusSeconds(86400));
    String cedula = fictionalId();
    assertEquals('0', cedula.charAt(0));
    var event = guests(120, cedula);
    var opened = open(event);
    String secret = opened.get("registro_invitado_token").asText();
    UUID key = UUID.randomUUID();
    var result = guestSubmit(secret, cedula, key).andReturn();
    assertEquals(201, result.getResponse().getStatus());
    String receipt = result.getResponse().getContentAsString();
    assertFalse(receipt.contains(cedula));
    assertFalse(receipt.contains(secret));
    assertFalse(receipt.contains("nombres"));
    for (int i = 0; i < 5; i++)
      assertEquals(200, guestSubmit(secret, cedula, key).andReturn().getResponse().getStatus());
    var limited = guestSubmit(secret, cedula, key).andReturn();
    assertEquals(429, limited.getResponse().getStatus());
    assertEquals("60", limited.getResponse().getHeader("Retry-After"));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE sesion_id=?", Integer.class, event.session()));
    assertFalse(logs.getAll().contains(secret));
    assertFalse(logs.getAll().contains(event.qr()));
    assertFalse(logs.getAll().contains(event.owner()));
    assertFalse(logs.getAll().contains(cedula));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM contadores_limite WHERE ambito::text LIKE '%127.0.0.1%'",
            Integer.class));
    api.perform(
            post("/api/v1/guest-registrations")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        java.util.Map.of(
                            "qr_token", event.qr(), "nombres", "Dato que no debe aceptarse"))))
        .andExpect(r -> assertEquals(400, r.getResponse().getStatus()));
  }
}
