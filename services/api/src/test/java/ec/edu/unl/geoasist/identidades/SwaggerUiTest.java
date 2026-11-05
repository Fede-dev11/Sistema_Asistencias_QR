package ec.edu.unl.geoasist.identidades;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import org.junit.jupiter.api.Test;

class SwaggerUiTest extends PostgresFixture {
  @Test
  void contractAndUiArePublic() throws Exception {
    api.perform(get("/openapi.yaml")).andExpect(status().isOk());
    api.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }
}
