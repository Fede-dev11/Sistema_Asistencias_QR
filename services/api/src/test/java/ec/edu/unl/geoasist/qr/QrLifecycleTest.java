package ec.edu.unl.geoasist.qr;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class QrLifecycleTest extends PostgresFixture {
  @org.springframework.beans.factory.annotation.Autowired
  ec.edu.unl.geoasist.qr.aplicacion.IssueSessionQr issuer;

  @Test
  void rotatesAtThirtyExpiresAtFortyAndDoesNotExposeIdentity() throws Exception {
    UUID[] ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    start(ids[1], owner);
    Instant initial = clock.instant();
    String path = "/api/v1/sessions/" + ids[1] + "/qr";
    var first =
        json.readTree(
            api.perform(get(path).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    String token = first.get("qr_token").asText();
    assertTrue(token.length() >= 32);
    assertEquals(initial.plusSeconds(40), Instant.parse(first.get("vence_en").asText()));
    assertEquals(30, first.get("rotacion_segundos").asInt());
    assertEquals(40, first.get("vigencia_segundos").asInt());
    assertNull(first.get("cedula"));
    clock.set(initial.plusSeconds(29));
    api.perform(get(path).header("Authorization", "Bearer " + owner))
        .andExpect(jsonPath("qr_token").value(token));
    clock.set(initial.plusSeconds(30));
    var second =
        json.readTree(
            api.perform(get(path).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertNotEquals(token, second.get("qr_token").asText());
    var resolved = issuer.lookup(token);
    ec.edu.unl.geoasist.qr.aplicacion.IssueSessionQr.validate(resolved, initial.plusSeconds(39));
    var expired =
        assertThrows(
            ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure.class,
            () ->
                ec.edu.unl.geoasist.qr.aplicacion.IssueSessionQr.validate(
                    resolved, initial.plusSeconds(40)));
    assertEquals("TOKEN_VENCIDO", expired.code());
    assertEquals(
        2,
        db.queryForObject(
            "SELECT count(*) FROM tokens_qr WHERE sesion_id=?", Integer.class, ids[1]));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM tokens_qr WHERE secreto_hash=?", Integer.class, token));
  }
}
