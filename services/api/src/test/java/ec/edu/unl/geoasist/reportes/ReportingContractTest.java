package ec.edu.unl.geoasist.reportes;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReportingContractTest extends PostgresFixture {
  @Test
  void listsProjectsExportsAndAuditsWithContractHeaders() throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    seedRows(ids[1]);

    var page =
        api.perform(
                get("/api/v1/sessions/" + ids[1] + "/attendances?page=0&size=2")
                    .header("Authorization", "Bearer " + owner))
            .andExpect(result -> assertEquals(200, result.getResponse().getStatus()))
            .andExpect(
                result -> assertEquals("no-store", result.getResponse().getHeader("Cache-Control")))
            .andReturn();
    var body = json.readTree(page.getResponse().getContentAsString());
    assertEquals(2, body.get("items").size());
    assertEquals(3, body.get("total").asInt());
    assertTrue(body.get("items").get(0).has("asistencia_id"));
    assertTrue(body.get("items").get(0).has("attempt_id"));
    assertTrue(body.get("items").get(0).has("code"));
    assertTrue(body.get("items").get(0).has("mensaje"));

    api.perform(
            get("/api/v1/sessions/" + ids[1] + "/attendances?estado=PENDIENTE_REVISION")
                .header("Authorization", "Bearer " + owner))
        .andExpect(
            result ->
                assertEquals(
                    1,
                    json.readTree(result.getResponse().getContentAsString()).get("total").asInt()));
    api.perform(
            get("/api/v1/sessions/" + ids[1] + "/attendances?size=101")
                .header("Authorization", "Bearer " + owner))
        .andExpect(result -> assertEquals(400, result.getResponse().getStatus()));
    api.perform(
            get("/api/v1/sessions/" + ids[1] + "/attendances?estado=OTRO")
                .header("Authorization", "Bearer " + owner))
        .andExpect(result -> assertEquals(400, result.getResponse().getStatus()));

    String csv =
        api.perform(
                get("/api/v1/sessions/" + ids[1] + "/attendances/export")
                    .header("Authorization", "Bearer " + owner))
            .andExpect(result -> assertEquals(200, result.getResponse().getStatus()))
            .andExpect(
                result -> assertEquals("no-store", result.getResponse().getHeader("Cache-Control")))
            .andExpect(
                result ->
                    assertEquals(
                        "attachment; filename=\"asistencias.csv\"",
                        result.getResponse().getHeader("Content-Disposition")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(
        csv.startsWith(
            "asistencia_id,persona_id,cedula,nombres,estado,registrada_en,attempt_id,code,mensaje,forma,razon_manual\n"));
    assertTrue(csv.contains("'=PELIGRO"));

    var audit =
        api.perform(
                get("/api/v1/activities/" + ids[0] + "/audit?page=0&size=100")
                    .header("Authorization", "Bearer " + owner))
            .andExpect(result -> assertEquals(200, result.getResponse().getStatus()))
            .andReturn();
    assertTrue(
        json.readTree(audit.getResponse().getContentAsString())
            .get("items")
            .toString()
            .contains("EXPORTAR_ASISTENCIAS"));
  }

  @Test
  void missingUnauthorizedAndExpiredResourcesHaveExpectedStatus() throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    api.perform(get("/api/v1/sessions/" + ids[1] + "/attendances"))
        .andExpect(result -> assertEquals(401, result.getResponse().getStatus()));
    api.perform(
            get("/api/v1/sessions/" + UUID.randomUUID() + "/attendances")
                .header("Authorization", "Bearer " + owner))
        .andExpect(result -> assertEquals(404, result.getResponse().getStatus()));

    db.update(
        "UPDATE actividades SET estado='CERRADA',cerrada_en=?,anonimizar_en=? WHERE id=?",
        Timestamp.from(clock.instant().minusSeconds(200)),
        Timestamp.from(clock.instant().minusSeconds(100)),
        ids[0]);
    api.perform(
            get("/api/v1/sessions/" + ids[1] + "/attendances")
                .header("Authorization", "Bearer " + owner))
        .andExpect(result -> assertEquals(410, result.getResponse().getStatus()));
    api.perform(
            get("/api/v1/sessions/" + ids[1] + "/attendances/export")
                .header("Authorization", "Bearer " + owner))
        .andExpect(result -> assertEquals(410, result.getResponse().getStatus()));
    api.perform(
            get("/api/v1/activities/" + ids[0] + "/audit")
                .header("Authorization", "Bearer " + owner))
        .andExpect(result -> assertEquals(410, result.getResponse().getStatus()));
  }

  private void seedRows(UUID session) {
    confirmed(session, ScenarioFixtures.id(102), "=PELIGRO");
    attempt(session, ScenarioFixtures.id(106), "PENDIENTE_REVISION", "UBICACION_INCIERTA");
    attempt(session, ScenarioFixtures.id(107), "RECHAZADO", "NO_AUTORIZADO");
  }

  private void confirmed(UUID session, UUID person, String name) {
    UUID attempt = attempt(session, person, "ACEPTADO", "ASISTENCIA_CONFIRMADA");
    db.update(
        "INSERT INTO asistencias VALUES (?,?,?,?,?,'CREDENCIALES',?)",
        UUID.randomUUID(),
        session,
        person,
        attempt,
        Timestamp.from(clock.instant()),
        name);
  }

  private UUID attempt(UUID session, UUID person, String result, String code) {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,?,?,?,?,?,?)",
        id,
        session,
        person,
        db.queryForObject("SELECT nombres FROM personas WHERE id=?", String.class, person),
        result,
        code,
        "Motivo ficticio",
        "CREDENCIALES",
        Timestamp.from(clock.instant()));
    return id;
  }
}
