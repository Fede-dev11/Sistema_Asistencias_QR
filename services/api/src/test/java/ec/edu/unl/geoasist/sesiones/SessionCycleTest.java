package ec.edu.unl.geoasist.sesiones;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import ec.edu.unl.geoasist.sesiones.aplicacion.SessionCycle;
import ec.edu.unl.geoasist.soporte.AbsenceSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SessionCycleTest extends AbsenceSupport {
  @Autowired SessionCycle cycle;

  private String state(UUID session) {
    return db.queryForObject("SELECT estado FROM sesiones WHERE id=?", String.class, session);
  }

  private int audits(UUID activity, String action) {
    return db.queryForObject(
        "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=? AND accion=?",
        Integer.class,
        activity,
        action);
  }

  private UUID[] cyclic() {
    var ids = scenario("CLASE", 200);
    db.update("UPDATE actividades SET ciclo_automatico=true WHERE id=?", ids[0]);
    // Ventana futura: arranca recién cuando el reloj llega a ella.
    db.update(
        "UPDATE sesiones SET ventana_inicio=?,ventana_fin=? WHERE id=?",
        Timestamp.from(clock.instant().plusSeconds(600)),
        Timestamp.from(clock.instant().plusSeconds(4200)),
        ids[1]);
    return ids;
  }

  @Test
  void startsAndClosesOnWindowWithAbsencesAndSystemAudit() throws Exception {
    var ids = cyclic();
    authorize(ids[0], true, S1, S2);
    Instant start = clock.instant().plusSeconds(600);
    cycle.tick();
    assertEquals("PROGRAMADA", state(ids[1]), "antes de la ventana no inicia");
    clock.set(start);
    cycle.tick();
    assertEquals("ACTIVA", state(ids[1]));
    assertEquals(1, audits(ids[0], "SISTEMA_INICIO"));
    attend(ids[1], S1);
    clock.set(start.plusSeconds(3599));
    cycle.tick();
    assertEquals("ACTIVA", state(ids[1]));
    clock.set(start.plusSeconds(3600));
    cycle.tick();
    cycle.tick();
    assertEquals("CERRADA", state(ids[1]));
    assertEquals(1, absences(ids[1]));
    assertEquals(1, audits(ids[0], "SISTEMA_CIERRE"));
    String owner = token(ScenarioFixtures.TEACHER);
    var items =
        json.readTree(
                api.perform(
                        get("/api/v1/activities/" + ids[0] + "/audit")
                            .header("Authorization", "Bearer " + owner))
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("items");
    assertTrue(items.size() >= 2);
    for (var item : items)
      if (item.get("accion").asText().startsWith("SISTEMA_"))
        assertTrue(item.get("responsable_id").isNull());
  }

  @Test
  void withoutCycleOnlyTheTwoHourSafetyNetClosesSessions() throws Exception {
    var ids = scenario("CLASE", 200);
    authorize(ids[0], true, S1);
    start(ids[1], token(ScenarioFixtures.TEACHER));
    Instant end =
        db.queryForObject("SELECT ventana_fin FROM sesiones WHERE id=?", Timestamp.class, ids[1])
            .toInstant();
    clock.set(end.plusSeconds(7199));
    cycle.tick();
    assertEquals("ACTIVA", state(ids[1]));
    clock.set(end.plusSeconds(7200));
    cycle.tick();
    assertEquals("CERRADA", state(ids[1]));
    assertEquals(1, absences(ids[1]));
    assertEquals(1, audits(ids[0], "SISTEMA_CIERRE_TARDIO"));
  }

  @Test
  void failedStartIsAuditedOnceAndRetriedAfterSixtySeconds() throws Exception {
    var ids = cyclic();
    UUID space = UUID.randomUUID();
    db.update("INSERT INTO espacios(id,nombre,tipo) VALUES (?,'Sin ubicacion','AULA')", space);
    db.update(
        "UPDATE sesiones SET espacio_id=?,metodo_verificacion='SOLO_GPS' WHERE id=?",
        space,
        ids[1]);
    Instant start = clock.instant().plusSeconds(600);
    clock.set(start);
    cycle.tick();
    assertEquals("PROGRAMADA", state(ids[1]));
    assertEquals(1, audits(ids[0], "SISTEMA_INICIO_FALLIDO"));
    assertEquals(
        start.plusSeconds(60),
        db.queryForObject(
                "SELECT siguiente_intento_en FROM sesiones WHERE id=?", Timestamp.class, ids[1])
            .toInstant());
    clock.set(start.plusSeconds(30));
    cycle.tick();
    assertEquals(1, audits(ids[0], "SISTEMA_INICIO_FALLIDO"), "no reintenta antes de 60 s");
    db.update("UPDATE sesiones SET espacio_id=NULL WHERE id=?", ids[1]);
    clock.set(start.plusSeconds(60));
    cycle.tick();
    assertEquals("ACTIVA", state(ids[1]));
  }

  @Test
  void patchSetsCycleAndThresholdAndValidatesRange() throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    var ok =
        api.perform(
                patch("/api/v1/activities/" + ids[0])
                    .header("Authorization", "Bearer " + owner)
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("ciclo_automatico", true, "umbral_asistencia", 85.5))))
            .andReturn()
            .getResponse();
    assertEquals(200, ok.getStatus());
    var body = json.readTree(ok.getContentAsString());
    assertTrue(body.get("ciclo_automatico").asBoolean());
    assertEquals(85.5, body.get("umbral_asistencia").asDouble());
    for (double bad : new double[] {-1, 100.5})
      assertEquals(
          400,
          api.perform(
                  patch("/api/v1/activities/" + ids[0])
                      .header("Authorization", "Bearer " + owner)
                      .contentType("application/json")
                      .content(json.writeValueAsString(Map.of("umbral_asistencia", bad))))
              .andReturn()
              .getResponse()
              .getStatus());
  }
}
