package ec.edu.unl.geoasist.identidades;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "springdoc.swagger-ui.enabled=false")
class SwaggerUiDisabledTest extends PostgresFixture {
  @Test
  void uiIsNotFoundWhenDisabled() throws Exception {
    api.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
  }
}
