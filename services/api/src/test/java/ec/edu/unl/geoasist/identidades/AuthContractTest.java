package ec.edu.unl.geoasist.identidades;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class AuthContractTest extends PostgresFixture {
  @Autowired MockMvc api;

  @Test
  void loginRequiresRealDevelopmentCredentials() throws Exception {
    api.perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"correo\":\"docente@example.test\",\"password\":\"fixture-password\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("token_type").value("Bearer"))
        .andExpect(jsonPath("access_token").isString())
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void invalidCredentialsHaveProblemDetails() throws Exception {
    api.perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"correo\":\"docente@example.test\",\"password\":\"incorrecto\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("code").value("NO_AUTENTICADO"));
  }

  @Autowired tools.jackson.databind.ObjectMapper json;

  String loginToken() throws Exception {
    String body =
        api.perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(
                        "{\"correo\":\"docente@example.test\",\"password\":\"fixture-password\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body).get("access_token").asText();
  }

  @Test
  void logoutRevokesBearer() throws Exception {
    String token = loginToken();
    api.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.sso_logout_url")
                .value(org.hamcrest.Matchers.containsString("/cas/logout?service=")));
    api.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expiryRevokesBearer() throws Exception {
    String token = loginToken();
    clock.set(clock.instant().plusSeconds(8 * 3600));
    api.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void malformedAndExtraFieldsRejected() throws Exception {
    api.perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(
                    "{\"correo\":\"docente@example.test\",\"password\":\"fixture-password\",\"rol\":\"DIRECTOR_INSTITUCIONAL\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void developmentProviderUnavailableOutsideDev() {
    new org.springframework.boot.test.context.runner.ApplicationContextRunner()
        .withUserConfiguration(
            ec.edu.unl.geoasist.identidades.aplicacion.DevelopmentIdentityProvider.class)
        .withPropertyValues("spring.profiles.active=prod")
        .run(
            context ->
                org.junit.jupiter.api.Assertions.assertTrue(
                    context
                        .getBeansOfType(
                            ec.edu.unl.geoasist.identidades.aplicacion.IdentityProvider.class)
                        .isEmpty()));
  }

  @Test
  void privateRoutesRequireBearer() throws Exception {
    api.perform(get("/api/v1/activities"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("mensaje").isString());
  }
}
