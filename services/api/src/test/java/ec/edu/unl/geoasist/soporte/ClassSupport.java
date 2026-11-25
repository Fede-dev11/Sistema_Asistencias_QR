package ec.edu.unl.geoasist.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.*;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/** Ayudas comunes de las pruebas de clases (US3): docente 200 con paralelo 11 y espacio 501. */
public abstract class ClassSupport extends PostgresFixture {
  protected static final String SPACE = ScenarioFixtures.id(501).toString();
  protected static final String P11 = ScenarioFixtures.id(11).toString();
  protected static final String P12 = ScenarioFixtures.id(12).toString();

  /** Dentro de la ventana de la primera sesión generada con {@link #schedule}. */
  protected static final java.time.Instant IN_CLASS =
      java.time.Instant.parse("2026-10-20T13:30:00Z");

  protected ResultActions send(String method, String path, String bearer, Object body)
      throws Exception {
    var b =
        switch (method) {
          case "POST" -> post(path).header("Idempotency-Key", UUID.randomUUID().toString());
          case "PUT" -> put(path);
          case "PATCH" -> patch(path);
          case "DELETE" -> delete(path);
          default -> get(path);
        };
    b.header("Authorization", "Bearer " + bearer);
    if (body != null) b.contentType("application/json").content(json.writeValueAsString(body));
    return api.perform(b);
  }

  protected JsonNode ok(ResultActions r, int status) throws Exception {
    r.andExpect(status().is(status));
    String text = r.andReturn().getResponse().getContentAsString();
    return text.isEmpty() ? null : json.readTree(text);
  }

  protected Map<String, Object> schedule(List<Integer> days, String from, String to) {
    return Map.of(
        "dias", days, "hora_inicio", "08:00", "hora_fin", "10:00", "desde", from, "hasta", to);
  }

  protected JsonNode createClass(String bearer, List<String> parallels, Object schedule, int status)
      throws Exception {
    var body = new LinkedHashMap<String, Object>();
    body.put("modalidad", "CLASE");
    body.put("nombre", "Clase de prueba");
    body.put("espacio_id", SPACE);
    body.put("paralelo_ids", parallels);
    if (schedule != null) body.put("horario", schedule);
    return ok(send("POST", "/api/v1/activities", bearer, body), status);
  }
}
