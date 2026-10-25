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
            call(post("/api/v1/admin/people"), admin, Map.of("cedula", fictionalId(), "nombres", "Cuenta Ficticia"))
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
                        "persona_id", person.get("id").asText(),
                        "correo", mail,
                        "password", OLD,
                        "roles", List.of("ESTUDIANTE")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
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
    return json.readTree(r.andReturn().getResponse().getContentAsString()).get("access_token").asText();
  }

  ResultActions change(String bearer, String current, String next) throws Exception {
    return call(put("/api/v1/me/password"), bearer, Map.of("actual", current, "nueva", next));
  }

  @Test
  void changesPasswordKeepsCurrentSessionAndClosesOthers() throws Exception {
    String mail = mail();
    account(mail);
    String a = tokenOf(login(mail, OLD).andExpect(status().isOk()));
    String b = tokenOf(login(mail, OLD).andExpect(status().isOk()));
    change(a, OLD, "otra-clave-larga-1").andExpect(status().isNoContent());
    call(get("/api/v1/me"), a, null).andExpect(status().isOk());
    call(get("/api/v1/me"), b, null).andExpect(status().isUnauthorized());
    login(mail, OLD).andExpect(status().isUnauthorized());
    login(mail, "otra-clave-larga-1").andExpect(status().isOk());
  }

  @Test
  void weakOrSamePasswordAndWrongCurrentAreRejected() throws Exception {
    String mail = mail();
    account(mail);
    String t = tokenOf(login(mail, OLD).andExpect(status().isOk()));
    change(t, OLD, "corta").andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("PASSWORD_DEBIL"));
    change(t, OLD, OLD).andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("PASSWORD_DEBIL"));
    // Cada intento con actual incorrecta cuenta para el límite de login (5 por minuto, incluido el login inicial).
    for (int i = 0; i < 4; i++)
      change(t, "equivocada-" + i, "otra-clave-larga-1")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("code").value("PASSWORD_ACTUAL_INCORRECTA"));
    change(t, "equivocada-5", "otra-clave-larga-1").andExpect(status().isTooManyRequests());
    login(mail, OLD).andExpect(status().isTooManyRequests());
  }

  @Test
  void adminResetForcesChangeAndBlocksOtherEndpoints() throws Exception {
    String mail = mail();
    String id = account(mail);
    String old = tokenOf(login(mail, OLD).andExpect(status().isOk()));
    String admin = admin();
    JsonNode reset =
        json.readTree(
            call(post("/api/v1/admin/accounts/" + id + "/password-reset"), admin, null)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    String temp = reset.get("password_temporal").asText();
    assertEquals(16, temp.length());
    call(get("/api/v1/me"), old, null).andExpect(status().isUnauthorized());
    assertEquals(
        1L,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_academica WHERE accion='RESTABLECER_PASSWORD' AND entidad_id=?",
            Long.class,
            UUID.fromString(id)));
    assertFalse(
        db.queryForObject(
                "SELECT nuevo::text FROM auditoria_academica WHERE accion='RESTABLECER_PASSWORD' AND entidad_id=?",
                String.class,
                UUID.fromString(id))
            .contains(temp));
    var res = login(mail, temp).andExpect(status().isOk()).andExpect(jsonPath("debe_cambiar_password").value(true));
    String t = tokenOf(res);
    call(get("/api/v1/me"), t, null).andExpect(status().isOk());
    call(post("/api/v1/auth/renew"), t, null)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("code").value("CAMBIO_PASSWORD_REQUERIDO"));
    change(t, temp, "definitiva-larga-1").andExpect(status().isNoContent());
    call(post("/api/v1/admin/accounts/" + id + "/password-reset"), t, null).andExpect(status().isForbidden());
    call(post("/api/v1/auth/renew"), t, null).andExpect(status().isOk());
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
