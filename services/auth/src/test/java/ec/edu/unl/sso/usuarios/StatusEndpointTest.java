package ec.edu.unl.sso.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.sso.soporte.AuthFixture;
import org.junit.jupiter.api.Test;

class StatusEndpointTest extends AuthFixture {
  private static final String URL = "/api/v1/users/status";

  @Test
  void validKeyReturnsActiveFlag() throws Exception {
    mvc.perform(
            get(URL)
                .param("correo", "docente@example.test")
                .header("X-Service-Key", "fixture-service-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activo").value(true));
    mvc.perform(
            get(URL)
                .param("correo", "inactiva@example.test")
                .header("X-Service-Key", "fixture-service-key"))
        .andExpect(jsonPath("$.activo").value(false));
  }

  @Test
  void invalidOrMissingKeyIsUnauthorized() throws Exception {
    mvc.perform(get(URL).param("correo", "docente@example.test"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get(URL).param("correo", "docente@example.test").header("X-Service-Key", "otra"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unknownEmailIsNotFound() throws Exception {
    mvc.perform(
            get(URL)
                .param("correo", "nadie@example.test")
                .header("X-Service-Key", "fixture-service-key"))
        .andExpect(status().isNotFound());
  }
}
