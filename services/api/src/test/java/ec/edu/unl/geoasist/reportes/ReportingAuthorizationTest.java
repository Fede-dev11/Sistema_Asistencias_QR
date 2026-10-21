package ec.edu.unl.geoasist.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportingAuthorizationTest extends PostgresFixture {
  @Test
  void multiCareerUnlinkedEventsAndDirectCommandsShareTheSameScopeRules() throws Exception {
    var multi = scenario("EVENTO", 201);
    db.update("INSERT INTO actividad_carreras VALUES (?,?)", multi[0], ScenarioFixtures.id(2));
    db.update(
        "INSERT INTO actividad_paralelos VALUES (?,?,?)",
        multi[0],
        ScenarioFixtures.id(1),
        ScenarioFixtures.id(11));
    var unlinked = scenario("EVENTO", 201);
    db.update("DELETE FROM actividad_carreras WHERE actividad_id=?", unlinked[0]);
    String owner = token("encargado@example.test");
    String institution = token("director@example.test");
    String career = token("director-carrera@example.test");
    String other = token("otro-docente@example.test");
    for (String reader : java.util.List.of(institution, career)) {
      expect(200, "/api/v1/activities/" + multi[0], reader);
      expect(200, "/api/v1/sessions/" + multi[1] + "/attendances", reader);
      expect(200, "/api/v1/activities/" + multi[0] + "/audit", reader);
      expect(403, "/api/v1/sessions/" + multi[1] + "/qr", reader);
      api.perform(
              put("/api/v1/activities/" + multi[0] + "/guest-settings")
                  .header("Authorization", "Bearer " + reader)
                  .contentType("application/json")
                  .content("{\"habilitado\":false,\"tolerancia_invitado_segundos\":120}"))
          .andExpect(r -> assertEquals(403, r.getResponse().getStatus()));
      api.perform(
              post("/api/v1/sessions/" + multi[1] + "/start")
                  .header("Authorization", "Bearer " + reader)
                  .header("Idempotency-Key", UUID.randomUUID().toString()))
          .andExpect(r -> assertEquals(403, r.getResponse().getStatus()));
    }
    expect(403, "/api/v1/activities/" + multi[0], other);
    expect(403, "/api/v1/activities/" + unlinked[0], career);
    expect(403, "/api/v1/sessions/" + unlinked[1] + "/attendances", career);
    expect(200, "/api/v1/activities/" + unlinked[0], institution);
    expect(200, "/api/v1/activities/" + unlinked[0], owner);
  }

  @Test
  void readScopeAndExportRemainSeparateForEveryRole() throws Exception {
    var linked = scenario("EVENTO", 201);
    var otherCareer = scenario("EVENTO", 205);
    db.update("DELETE FROM actividad_carreras WHERE actividad_id=?", otherCareer[0]);
    db.update(
        "INSERT INTO actividad_carreras VALUES (?,?)", otherCareer[0], ScenarioFixtures.id(2));

    String owner = token("encargado@example.test");
    String institutional = token("director@example.test");
    String career = token("director-carrera@example.test");
    String other = token("otro-docente@example.test");

    expect(200, "/api/v1/sessions/" + linked[1] + "/attendances", owner);
    expect(200, "/api/v1/sessions/" + linked[1] + "/attendances", institutional);
    expect(200, "/api/v1/sessions/" + linked[1] + "/attendances", career);
    expect(403, "/api/v1/sessions/" + linked[1] + "/attendances", other);
    expect(403, "/api/v1/sessions/" + otherCareer[1] + "/attendances", career);
    expect(200, "/api/v1/sessions/" + otherCareer[1] + "/attendances", institutional);

    expect(200, "/api/v1/sessions/" + linked[1] + "/attendances/export", owner);
    expect(403, "/api/v1/sessions/" + linked[1] + "/attendances/export", institutional);
    expect(403, "/api/v1/sessions/" + linked[1] + "/attendances/export", career);

    expect(200, "/api/v1/activities/" + linked[0] + "/audit", owner);
    expect(200, "/api/v1/activities/" + linked[0] + "/audit", institutional);
    expect(200, "/api/v1/activities/" + linked[0] + "/audit", career);
    expect(403, "/api/v1/activities/" + linked[0] + "/audit", other);

    db.update(
        "INSERT INTO roles_cuenta VALUES (?, 'DIRECTOR_INSTITUCIONAL') ON CONFLICT DO NOTHING",
        ScenarioFixtures.id(201));
    String multiRole = token("encargado@example.test");
    expect(200, "/api/v1/sessions/" + linked[1] + "/attendances/export", multiRole);
  }

  private void expect(int status, String path, String token) throws Exception {
    api.perform(get(path).header("Authorization", "Bearer " + token))
        .andExpect(result -> assertEquals(status, result.getResponse().getStatus()));
  }
}
