package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GeographyReviewContractTest extends PostgresFixture {
  @Test
  void geolocationConfigurationIsResponsibleProgrammedAndCapabilityBound() throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    String other = token("otro-docente@example.test");

    api.perform(
            put("/api/v1/sessions/" + ids[1] + "/geolocation")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content("{\"habilitada\":false}"))
        .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));
    api.perform(
            put("/api/v1/sessions/" + ids[1] + "/geolocation")
                .header("Authorization", "Bearer " + other)
                .contentType("application/json")
                .content("{\"habilitada\":false}"))
        .andExpect(result -> assertEquals(403, result.getResponse().getStatus()));
    api.perform(
            put("/api/v1/sessions/" + ids[1] + "/geolocation")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content("{\"habilitada\":true}"))
        .andExpect(result -> assertEquals(400, result.getResponse().getStatus()));
    api.perform(
            put("/api/v1/sessions/" + ids[1] + "/geolocation")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content(
                    "{\"habilitada\":true,\"radio_m\":20,\"precision_max_m\":10,\"antiguedad_max_s\":60}"))
        .andExpect(result -> assertEquals(409, result.getResponse().getStatus()));

    start(ids[1], owner);
    api.perform(
            put("/api/v1/sessions/" + ids[1] + "/geolocation")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content("{\"habilitada\":false}"))
        .andExpect(result -> assertEquals(409, result.getResponse().getStatus()));
  }

  @Test
  void reviewValidatesDecisionReasonAndResponsibility() throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    String director = token(ScenarioFixtures.DIRECTOR);
    start(ids[1], owner);
    UUID attempt = pending(ids[1], ScenarioFixtures.id(102));

    api.perform(
            post("/api/v1/attempts/" + attempt + "/review")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content("{\"decision\":\"APROBAR\",\"razon\":\"   \"}"))
        .andExpect(result -> assertEquals(400, result.getResponse().getStatus()));
    api.perform(
            post("/api/v1/attempts/" + attempt + "/review")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content("{\"decision\":\"OTRA\",\"razon\":\"Dato ficticio\"}"))
        .andExpect(result -> assertEquals(400, result.getResponse().getStatus()));
    api.perform(
            post("/api/v1/attempts/" + attempt + "/review")
                .header("Authorization", "Bearer " + director)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content("{\"decision\":\"RECHAZAR\",\"razon\":\"Dato ficticio\"}"))
        .andExpect(result -> assertEquals(403, result.getResponse().getStatus()));
  }

  private UUID pending(UUID session, UUID person) {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,?,?,?,?,?,?)",
        id,
        session,
        person,
        "Persona pendiente ficticia",
        "PENDIENTE_REVISION",
        "UBICACION_INCIERTA",
        "Revisión necesaria.",
        "CREDENCIALES",
        Timestamp.from(clock.instant()));
    return id;
  }
}
