package ec.edu.unl.geoasist.estadisticas;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.geoasist.soporte.AbsenceSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class AttendanceStatsTest extends AbsenceSupport {
  private JsonNode fetch(String path, String bearer, int status) throws Exception {
    var r =
        api.perform(get(path).header("Authorization", "Bearer " + bearer))
            .andExpect(status().is(status))
            .andReturn();
    String body = r.getResponse().getContentAsString();
    return body.isEmpty() ? null : json.readTree(body);
  }

  /** 3 estudiantes x 10 sesiones: S1 asiste a 10, S2 a 7, S3 a 5 (con 2 justificadas). */
  private UUID classOfTen(String owner, int year) throws Exception {
    var ids = scenario("CLASE", 200);
    db.update(
        "INSERT INTO actividad_paralelos VALUES (?,?,?)",
        ids[0],
        ScenarioFixtures.id(1),
        ScenarioFixtures.id(11));
    db.update("DELETE FROM sesiones WHERE id=?", ids[1]);
    authorize(ids[0], true, S1, S2, S3);
    for (int i = 0; i < 10; i++) {
      UUID s = activeSession(ids[0], day(year, i));
      attend(s, S1);
      if (i < 7) attend(s, S2);
      if (i < 5) attend(s, S3);
      close(s, owner);
    }
    db.update(
        "UPDATE ausencias SET estado='JUSTIFICADA' WHERE persona_id=? AND sesion_id IN (SELECT id FROM sesiones WHERE actividad_id=? ORDER BY ventana_inicio LIMIT 2 OFFSET 5)",
        S3,
        ids[0]);
    // No cuentan: sesión aún activa con asistencia y sesión cancelada.
    attend(activeSession(ids[0], day(year, 20)), S1);
    db.update(
        "INSERT INTO sesiones(id,actividad_id,estado,ventana_inicio,ventana_fin) VALUES (?,?,'CANCELADA',?,?)",
        UUID.randomUUID(),
        ids[0],
        java.sql.Timestamp.from(day(year, 21)),
        java.sql.Timestamp.from(day(year, 21).plusSeconds(60)));
    return ids[0];
  }

  private static JsonNode student(JsonNode stats, UUID person) {
    for (JsonNode n : stats.get("items"))
      if (n.get("persona_id").asText().equals(person.toString())) return n;
    throw new AssertionError("sin " + person);
  }

  @Test
  void threeByTenProducesFixedPercentagesAndThresholdMarks() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    UUID activity = classOfTen(owner, nextYear());
    // US1-3: quitar de la lista a S3 no cambia su historial.
    authorize(activity, false, S3);
    JsonNode stats = fetch("/api/v1/activities/" + activity + "/stats", owner, 200);
    assertEquals(3, stats.get("total").asInt());
    assertEquals(70, stats.get("umbral_asistencia").asInt());

    JsonNode a = student(stats, S1), b = student(stats, S2), c = student(stats, S3);
    assertEquals(10, a.get("sesiones_computables").asInt());
    assertEquals(10, a.get("asistencias").asInt());
    assertEquals(0, a.get("ausencias").asInt());
    assertEquals(100.00, a.get("porcentaje").asDouble());
    assertFalse(a.get("bajo_umbral").asBoolean());

    assertEquals(7, b.get("asistencias").asInt());
    assertEquals(3, b.get("ausencias").asInt());
    assertEquals(70.00, b.get("porcentaje").asDouble());
    assertFalse(b.get("bajo_umbral").asBoolean(), "70 % no es menor al umbral de 70 %");

    assertEquals(10, c.get("sesiones_computables").asInt());
    assertEquals(5, c.get("asistencias").asInt());
    assertEquals(5, c.get("ausencias").asInt());
    assertEquals(2, c.get("ausencias_justificadas").asInt());
    assertEquals(50.00, c.get("porcentaje").asDouble());
    assertTrue(c.get("bajo_umbral").asBoolean());

    JsonNode total = stats.get("resumen");
    assertEquals(30, total.get("sesiones_computables").asInt());
    assertEquals(22, total.get("asistencias").asInt());
    assertEquals(8, total.get("ausencias").asInt());
    assertEquals(73.33, total.get("porcentaje").asDouble());

    // Umbral configurable por actividad.
    db.update("UPDATE actividades SET umbral_asistencia=40 WHERE id=?", activity);
    assertFalse(
        student(fetch("/api/v1/activities/" + activity + "/stats", owner, 200), S3)
            .get("bajo_umbral")
            .asBoolean());
    assertEquals(
        2,
        fetch("/api/v1/activities/" + activity + "/stats?size=2", owner, 200).get("items").size());
    fetch("/api/v1/activities/" + activity + "/stats?size=101", owner, 400);
  }

  @Test
  void activityWithoutClosedSessionsReportsNoDataInsteadOfZero() throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    authorize(ids[0], true, S1);
    start(ids[1], owner);
    attend(ids[1], S1);
    JsonNode stats = fetch("/api/v1/activities/" + ids[0] + "/stats", owner, 200);
    JsonNode s = student(stats, S1);
    assertEquals(0, s.get("sesiones_computables").asInt());
    assertTrue(s.get("porcentaje").isNull());
    assertFalse(s.get("bajo_umbral").asBoolean());
    assertTrue(stats.get("resumen").get("porcentaje").isNull());
  }

  @Test
  void activityStatsAndAbsencesRespectQueryScope() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    UUID activity = classOfTen(owner, nextYear());
    UUID session =
        db.queryForObject(
            "SELECT id FROM sesiones WHERE actividad_id=? AND estado='CERRADA' ORDER BY ventana_inicio DESC LIMIT 1",
            UUID.class,
            activity);
    String stats = "/api/v1/activities/" + activity + "/stats";
    String absences = "/api/v1/sessions/" + session + "/absences";
    fetch(stats, token("director@example.test"), 200);
    fetch(stats, token("director-carrera@example.test"), 200);
    for (String denied : List.of("otro-docente@example.test", ScenarioFixtures.STUDENT)) {
      String t = token(denied);
      fetch(stats, t, 403);
      fetch(absences, t, 403);
    }
    JsonNode abs = fetch(absences, owner, 200);
    assertEquals(2, abs.get("total").asInt());
    assertEquals("AUSENTE", abs.get("items").get(0).get("estado").asText());
    // Fuera de la carrera del director de carrera.
    db.update("DELETE FROM actividad_paralelos WHERE actividad_id=?", activity);
    db.update(
        "UPDATE actividad_carreras SET carrera_id=? WHERE actividad_id=?",
        ScenarioFixtures.id(2),
        activity);
    fetch(stats, token("director-carrera@example.test"), 403);
    fetch(stats, token("director@example.test"), 200);
  }

  @Test
  void parallelAndCareerStatsFollowRoleScopeAndPeriod() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    int year = nextYear();
    classOfTen(owner, year);
    // Otra clase de la carrera 2 con una ausencia, del mismo periodo.
    var other = scenario("CLASE", 205);
    db.update(
        "UPDATE actividad_carreras SET carrera_id=? WHERE actividad_id=?",
        ScenarioFixtures.id(2),
        other[0]);
    db.update(
        "INSERT INTO actividad_paralelos VALUES (?,?,?)",
        other[0],
        ScenarioFixtures.id(2),
        ScenarioFixtures.id(12));
    db.update("DELETE FROM sesiones WHERE id=?", other[1]);
    authorize(other[0], true, S3);
    close(activeSession(other[0], day(year, 3)), token("otro-docente@example.test"));

    String range = "desde=" + day(year, 0) + "&hasta=" + day(year, 100);
    // S1 y S2 (paralelo 11): 17 asistencias, 3 ausencias; S3 (paralelo 12) queda fuera de este.
    JsonNode teacher = fetch("/api/v1/stats/parallels?" + range, owner, 200);
    assertEquals(1, teacher.get("items").size());
    JsonNode p = teacher.get("items").get(0);
    assertEquals(ScenarioFixtures.id(11).toString(), p.get("paralelo_id").asText());
    assertEquals(20, p.get("sesiones_computables").asInt());
    assertEquals(85.00, p.get("porcentaje").asDouble());

    JsonNode careerDirector =
        fetch("/api/v1/stats/careers?" + range, token("director-carrera@example.test"), 200);
    assertEquals(1, careerDirector.get("items").size());
    assertEquals("CARRERA-1", careerDirector.get("items").get(0).get("carrera").asText());
    assertEquals(30, careerDirector.get("items").get(0).get("sesiones_computables").asInt());

    JsonNode institutional =
        fetch("/api/v1/stats/careers?" + range, token("director@example.test"), 200);
    assertEquals(2, institutional.get("items").size());
    assertEquals(
        1,
        fetch(
                "/api/v1/stats/parallels?careerId=" + ScenarioFixtures.id(2) + "&" + range,
                token("director@example.test"),
                200)
            .get("items")
            .size());

    // Periodo sin sesiones; otra carrera pedida por un director; estudiante.
    assertEquals(
        0,
        fetch(
                "/api/v1/stats/careers?desde=" + day(year, 200) + "&hasta=" + day(year, 300),
                owner,
                200)
            .get("items")
            .size());
    fetch(
        "/api/v1/stats/parallels?careerId=" + ScenarioFixtures.id(2) + "&" + range,
        token("director-carrera@example.test"),
        403);
    fetch("/api/v1/stats/careers?" + range, token(ScenarioFixtures.STUDENT), 403);
    fetch("/api/v1/stats/careers?desde=" + day(year, 5) + "&hasta=" + day(year, 1), owner, 400);
  }
}
