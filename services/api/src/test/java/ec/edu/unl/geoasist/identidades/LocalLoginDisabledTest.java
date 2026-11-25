package ec.edu.unl.geoasist.identidades;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "geoasist.auth.local-login=false")
class LocalLoginDisabledTest extends PostgresFixture {
  @Test
  void loginIsNotFoundWhenLocalLoginIsDisabled() throws Exception {
    api.perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"correo\":\"docente@example.test\",\"password\":\"fixture-password\"}"))
        .andExpect(status().isNotFound());
  }
}
