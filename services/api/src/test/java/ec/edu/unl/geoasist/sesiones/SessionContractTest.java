package ec.edu.unl.geoasist.sesiones;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SessionContractTest extends PostgresFixture {
  @Test
  void ownerReadsStartsReplaysAndCloses() throws Exception {
    UUID[] ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    String key = UUID.randomUUID().toString();
    api.perform(get("/api/v1/activities/" + ids[0]).header("Authorization", "Bearer " + owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("responsable_id").value(ScenarioFixtures.id(200).toString()))
        .andExpect(jsonPath("cerrada_en").isEmpty());
    api.perform(get("/api/v1/activities").header("Authorization", "Bearer " + owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("items").isArray());
    var start =
        post("/api/v1/sessions/" + ids[1] + "/start")
            .header("Authorization", "Bearer " + owner)
            .header("Idempotency-Key", key);
    api.perform(start).andExpect(status().isOk()).andExpect(jsonPath("estado").value("ACTIVA"));
    api.perform(start).andExpect(status().isOk()).andExpect(jsonPath("estado").value("ACTIVA"));
    api.perform(
            post("/api/v1/sessions/" + ids[1] + "/close")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("estado").value("CERRADA"));
    api.perform(
            get("/api/v1/sessions/" + ids[1] + "/qr").header("Authorization", "Bearer " + owner))
        .andExpect(status().isConflict());
  }

  @Test
  void readersAndOtherOwnersCannotWriteOrDisplayQr() throws Exception {
    UUID[] ids = scenario("CLASE", 200);
    String director = token(ScenarioFixtures.DIRECTOR), other = token("otro-docente@example.test");
    api.perform(get("/api/v1/activities/" + ids[0]).header("Authorization", "Bearer " + director))
        .andExpect(status().isOk());
    for (String reader : List.of(director, other)) {
      api.perform(
              post("/api/v1/sessions/" + ids[1] + "/start")
                  .header("Authorization", "Bearer " + reader)
                  .header("Idempotency-Key", UUID.randomUUID().toString()))
          .andExpect(status().isForbidden());
      api.perform(
              get("/api/v1/sessions/" + ids[1] + "/qr").header("Authorization", "Bearer " + reader))
          .andExpect(status().isForbidden());
    }
    api.perform(
            get("/api/v1/activities")
                .param("size", "101")
                .header("Authorization", "Bearer " + director))
        .andExpect(status().isBadRequest());
  }

  @Test
  void missingKeyAndBadIdAreProblems() throws Exception {
    UUID[] ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    api.perform(
            post("/api/v1/sessions/" + ids[1] + "/start")
                .header("Authorization", "Bearer " + owner))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("code").isString());
    api.perform(get("/api/v1/activities/not-a-uuid").header("Authorization", "Bearer " + owner))
        .andExpect(status().isBadRequest());
  }
}
