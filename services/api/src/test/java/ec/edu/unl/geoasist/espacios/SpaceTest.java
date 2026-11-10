package ec.edu.unl.geoasist.espacios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class SpaceTest extends PostgresFixture {
  private ResultActions submit(String bearer, String body) throws Exception {
    return api.perform(
        post("/api/v1/spaces")
            .header("Authorization", "Bearer " + bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private String create(String bearer, String code) throws Exception {
    String body =
        submit(
                bearer,
                "{\"codigo\":\""
                    + code
                    + "\",\"nombre\":\"Aula "
                    + code
                    + "\",\"tipo\":\"AULA\",\"latitud\":-4.03,\"longitud\":-79.2}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body).get("id").asText();
  }

  @Test
  void createSearchDuplicateAndRanges() throws Exception {
    String teacher = token(ScenarioFixtures.TEACHER), other = token("otro-docente@example.test");
    String code = "T" + System.nanoTime() % 100000;
    String id = create(teacher, code + "-X");
    submit(other, "{\"codigo\":\"" + code.toLowerCase() + " x\",\"nombre\":\"Otra\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("code").value("ESPACIO_CODIGO_EXISTE"))
        .andExpect(jsonPath("espacio_id").value(id));
    submit(other, "{\"codigo\":\"R1" + code + "\",\"nombre\":\"N\",\"latitud\":91,\"longitud\":0}")
        .andExpect(status().isBadRequest());
    submit(other, "{\"codigo\":\"R2" + code + "\",\"nombre\":\"N\",\"latitud\":1}")
        .andExpect(status().isBadRequest());
    submit(other, "{\"codigo\":\"R3" + code + "\",\"nombre\":\"N\",\"radio_defecto_m\":0}")
        .andExpect(status().isBadRequest());
    api.perform(
            get("/api/v1/spaces")
                .param("q", code.toLowerCase() + "x")
                .header("Authorization", "Bearer " + other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("total").value(1))
        .andExpect(jsonPath("items[0].id").value(id))
        .andExpect(jsonPath("items[0].editable").value(false));
    api.perform(
            get("/api/v1/spaces")
                .param("q", "AULA " + code)
                .header("Authorization", "Bearer " + other))
        .andExpect(jsonPath("total").value(1));
  }

  @Test
  void onlyCreatorOrAdministratorEditsAndStudentsCannotUse() throws Exception {
    String teacher = token(ScenarioFixtures.TEACHER);
    String other = token("otro-docente@example.test");
    String admin = token("admin@dev.geoasist.local");
    String student = token(ScenarioFixtures.STUDENT);
    String id = create(teacher, "E" + System.nanoTime() % 100000);
    String patch = "{\"radio_defecto_m\":35}";
    for (var c : new String[][] {{other, "403"}, {teacher, "200"}, {admin, "200"}})
      api.perform(
              patch("/api/v1/spaces/" + id)
                  .header("Authorization", "Bearer " + c[0])
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(patch))
          .andExpect(status().is(Integer.parseInt(c[1])));
    submit(student, "{\"codigo\":\"EST1\",\"nombre\":\"N\"}").andExpect(status().isForbidden());
    api.perform(get("/api/v1/spaces").header("Authorization", "Bearer " + student))
        .andExpect(status().isForbidden());
    org.junit.jupiter.api.Assertions.assertEquals(
        2,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_espacios WHERE accion='EDITAR_ESPACIO' AND espacio_id=?::uuid",
            Integer.class,
            id));
  }
}
