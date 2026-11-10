package ec.edu.unl.geoasist.identidades;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

@TestPropertySource(properties = "geoasist.rate-limit-multiplier=1")
class AccountSelfServiceTest extends PostgresFixture {
  static final String OLD = "fixture-password";

  /** Los contadores viven en la base compartida y el reloj es fijo: otras clases los llenan. */
  @org.junit.jupiter.api.BeforeEach
  void resetRateLimitCounters() {
    db.update("DELETE FROM contadores_limite");
  }

  static String adminToken; // un solo login: el límite por cuenta es de 5 por minuto con reloj fijo

  String admin() throws Exception {
    if (adminToken == null) adminToken = token("admin@dev.geoasist.local");
    return adminToken;
  }

  String mail() {
    return "u" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
  }

  /** Crea persona y cuenta ESTUDIANTE vía API de administración; devuelve id de cuenta. */
  String account(String mail) throws Exception {
    String admin = admin();
    JsonNode person =
        json.readTree(
            call(
                    post("/api/v1/admin/people"),
                    admin,
                    Map.of("cedula", fictionalId(), "nombres", "Cuenta Ficticia"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    JsonNode created =
        json.readTree(
            call(
                    post("/api/v1/admin/accounts"),
                    admin,
                    Map.of(
                        "persona_id",
                        person.get("id").asText(),
                        "correo",
                        mail,
                        "roles",
                        List.of("ESTUDIANTE")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    // Las cuentas creadas por la API son CAS sin contraseña; las pruebas necesitan iniciar sesión
    // local.
    db.update(
        "UPDATE cuentas SET proveedor='DEV',password_hash=(SELECT password_hash FROM cuentas WHERE identificador='docente@example.test') WHERE identificador=?",
        mail);
    return db.queryForObject("SELECT id FROM cuentas WHERE identificador=?", String.class, mail);
  }

  ResultActions call(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b,
      String bearer,
      Object body)
      throws Exception {
    if (bearer != null) b.header("Authorization", "Bearer " + bearer);
    b.header("Idempotency-Key", UUID.randomUUID().toString());
    if (body != null) b.contentType("application/json").content(json.writeValueAsString(body));
    return api.perform(b);
  }

  ResultActions login(String mail, String password) throws Exception {
    return call(post("/api/v1/auth/login"), null, Map.of("correo", mail, "password", password));
  }

  String tokenOf(ResultActions r) throws Exception {
    return json.readTree(r.andReturn().getResponse().getContentAsString())
        .get("access_token")
        .asText();
  }

  @Test
  void profileHasNoSecrets() throws Exception {
    String mail = mail();
    account(mail);
    String t = tokenOf(login(mail, OLD).andExpect(status().isOk()));
    String body =
        call(get("/api/v1/me"), t, null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("correo").value(mail))
            .andExpect(jsonPath("roles[0]").value("ESTUDIANTE"))
            .andExpect(jsonPath("paralelos").isArray())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(body.contains("password_hash"));
    assertFalse(body.contains(t));
    call(get("/api/v1/me"), null, null).andExpect(status().isUnauthorized());
  }
}
