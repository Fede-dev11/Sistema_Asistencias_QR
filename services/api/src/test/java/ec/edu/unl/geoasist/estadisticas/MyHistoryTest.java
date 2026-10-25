package ec.edu.unl.geoasist.estadisticas;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.geoasist.soporte.AbsenceSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class MyHistoryTest extends AbsenceSupport {
  private JsonNode fetch(String path, String bearer, int status) throws Exception {
    var r =
        api.perform(get(path).header("Authorization", "Bearer " + bearer))
            .andExpect(status().is(status))
            .andReturn();
    return json.readTree(r.getResponse().getContentAsString());
  }

  /** Clase con 4 sesiones cerradas: S1 asiste a 2, falta a una justificada y a una sin justificar. */
  private UUID[] classFor(String owner, int year) throws Exception {
    var ids = scenario("CLASE", 200);
    db.update("DELETE FROM sesiones WHERE id=?", ids[1]);
    authorize(ids[0], true, S1, S3);
    var sessions = new UUID[4];
    for (int i = 0; i < 4; i++) {
      sessions[i] = activeSession(ids[0], day(year, i));
      if (i < 2) attend(sessions[i], S1);
      attend(sessions[i], S3);
      close(sessions[i], owner);
    }
    db.update(
        "UPDATE ausencias SET estado='JUSTIFICADA' WHERE persona_id=? AND sesion_id=?", S1, sessions[2]);
    db.update(
        "INSERT INTO justificaciones(id,sesion_id,persona_id,origen,motivo,estado,decidida_por,razon_decision,solicitada_en,decidida_en)"
            + " VALUES (?,?,?,'ESTUDIANTE','Motivo ficticio','APROBADA',?,'ok',now(),now())",
        UUID.randomUUID(), sessions[2], S1, ScenarioFixtures.id(200));
    db.update(
        "INSERT INTO justificaciones(id,sesion_id,persona_id,origen,motivo,estado,solicitada_en)"
            + " VALUES (?,?,?,'ESTUDIANTE','Motivo ficticio','PENDIENTE',now())",
        UUID.randomUUID(), sessions[3], S1);
    return new UUID[] {ids[0], sessions[0], sessions[2], sessions[3]};
  }

  private static JsonNode row(JsonNode page, UUID session) {
    for (JsonNode n : page.get("items")) if (n.get("sesion_id").asText().equals(session.toString())) return n;
    return null;
  }

  @Test
  void showsOwnResultsAndStatsAndNeverSomeoneElses() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    var c = classFor(owner, nextYear());
    JsonNode mine = fetch("/api/v1/me/attendances?size=100", token(ScenarioFixtures.STUDENT), 200);
    assertEquals("ASISTIO", row(mine, c[1]).get("resultado").asText());
    assertTrue(row(mine, c[1]).get("justificacion").isNull());
    assertEquals("JUSTIFICADA", row(mine, c[2]).get("resultado").asText());
    assertEquals("APROBADA", row(mine, c[2]).get("justificacion").asText());
    assertEquals("AUSENTE", row(mine, c[3]).get("resultado").asText());
    assertEquals("PENDIENTE", row(mine, c[3]).get("justificacion").asText());
    assertEquals("Escenario ficticio", row(mine, c[1]).get("actividad").asText());

    // US3-2: S3 (otro estudiante) asistió a las 4 y no ve nada de S1; cualquier persona_id pedido se ignora.
    JsonNode other = fetch("/api/v1/me/attendances?size=100&persona_id=" + S1, token("otro-estudiante@example.test"), 200);
    assertNotNull(row(other, c[3]));
    assertEquals("ASISTIO", row(other, c[3]).get("resultado").asText());
    assertTrue(row(other, c[3]).get("justificacion").isNull());

    JsonNode st = fetch("/api/v1/me/stats", token(ScenarioFixtures.STUDENT), 200);
    JsonNode a = null;
    for (JsonNode n : st.get("items")) if (n.get("actividad_id").asText().equals(c[0].toString())) a = n;
    assertNotNull(a);
    assertEquals(4, a.get("sesiones_computables").asInt());
    assertEquals(2, a.get("asistencias").asInt());
    assertEquals(1, a.get("ausencias_justificadas").asInt());
    assertEquals(50.00, a.get("porcentaje").asDouble());
    assertTrue(a.get("bajo_umbral").asBoolean());
  }

  @Test
  void pendingReviewAttemptIsReported() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    var c = classFor(owner, nextYear());
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,'PENDIENTE_REVISION','REVISION','Pendiente','CREDENCIALES',now())",
        UUID.randomUUID(), c[3], S1);
    JsonNode mine = fetch("/api/v1/me/attendances?size=100", token(ScenarioFixtures.STUDENT), 200);
    assertEquals("PENDIENTE_REVISION", row(mine, c[3]).get("resultado").asText());
  }

  @Test
  void anonymizedActivityDisappears() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    var c = classFor(owner, nextYear());
    String student = token(ScenarioFixtures.STUDENT);
    assertNotNull(row(fetch("/api/v1/me/attendances?size=100", student, 200), c[1]));
    db.update(
        "UPDATE actividades SET estado='CERRADA',cerrada_en=?,anonimizar_en=? WHERE id=?",
        Timestamp.from(clock.instant().atZone(java.time.ZoneOffset.UTC).minusYears(5).toInstant()),
        Timestamp.from(clock.instant()),
        c[0]);
    db.queryForObject("SELECT anonimizar_vencidas(?,100)", Integer.class, Timestamp.from(clock.instant()));
    assertNull(row(fetch("/api/v1/me/attendances?size=100", student, 200), c[1]));
    for (JsonNode n : fetch("/api/v1/me/stats", student, 200).get("items"))
      assertNotEquals(c[0].toString(), n.get("actividad_id").asText());
  }

  @Test
  void paginatesAndRejectsBadSize() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    classFor(owner, nextYear());
    String student = token(ScenarioFixtures.STUDENT);
    JsonNode p0 = fetch("/api/v1/me/attendances?page=0&size=2", student, 200);
    JsonNode p1 = fetch("/api/v1/me/attendances?page=1&size=2", student, 200);
    assertEquals(2, p0.get("items").size());
    assertTrue(p0.get("total").asInt() >= 4);
    assertNotEquals(p0.get("items").get(0).get("sesion_id"), p1.get("items").get(0).get("sesion_id"));
    fetch("/api/v1/me/attendances?size=101", student, 400);
    fetch("/api/v1/me/attendances?size=0", student, 400);
  }
}
