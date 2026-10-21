package ec.edu.unl.geoasist.actividades;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ActivityClosureTest extends PostgresFixture {
  @Test
  void definitiveClosureIsImmutableAndClosesEverySession() throws Exception {
    UUID[] ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    start(ids[1], owner);
    Instant at = clock.instant();
    String key = UUID.randomUUID().toString();
    api.perform(
            post("/api/v1/activities/" + ids[0] + "/close")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", key))
        .andExpect(status().isOk())
        .andExpect(jsonPath("estado").value("CERRADA"))
        .andExpect(jsonPath("anonimizar_en").value("2031-10-20T01:00:00Z"));
    assertEquals(
        "CERRADA",
        db.queryForObject("SELECT estado FROM sesiones WHERE id=?", String.class, ids[1]));
    clock.set(at.plusSeconds(20));
    api.perform(
            post("/api/v1/activities/" + ids[0] + "/close")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", key))
        .andExpect(status().isOk())
        .andExpect(jsonPath("cerrada_en").value(at.toString()));
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () ->
            db.update(
                "UPDATE actividades SET cerrada_en=cerrada_en+INTERVAL '1 day' WHERE id=?",
                ids[0]));
  }

  @Test
  void closingOneSessionDoesNotStartActivityRetention() throws Exception {
    UUID[] ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    start(ids[1], owner);
    api.perform(
            post("/api/v1/sessions/" + ids[1] + "/close")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(status().isOk());
    assertNull(
        db.queryForObject(
            "SELECT anonimizar_en FROM actividades WHERE id=?", java.sql.Timestamp.class, ids[0]));
  }

  @Test
  void activityClosureCompetesWithSessionStart() throws Exception {
    UUID[] ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    var ready = new java.util.concurrent.CountDownLatch(1);
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var starting =
          pool.submit(
              () -> {
                ready.await();
                return api.perform(
                        post("/api/v1/sessions/" + ids[1] + "/start")
                            .header("Authorization", "Bearer " + owner)
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var closing =
          pool.submit(
              () -> {
                ready.await();
                return api.perform(
                        post("/api/v1/activities/" + ids[0] + "/close")
                            .header("Authorization", "Bearer " + owner)
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      ready.countDown();
      assertTrue(
          Set.of(200, 409).contains(starting.get(15, java.util.concurrent.TimeUnit.SECONDS)));
      assertEquals(200, closing.get(15, java.util.concurrent.TimeUnit.SECONDS));
      assertEquals(
          "CERRADA",
          db.queryForObject("SELECT estado FROM sesiones WHERE id=?", String.class, ids[1]));
      assertEquals(
          "CERRADA",
          db.queryForObject("SELECT estado FROM actividades WHERE id=?", String.class, ids[0]));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void failedSessionUpdateRollsBackEntireActivityClose() throws Exception {
    UUID[] ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    start(ids[1], owner);
    String fn = "fixture_failure_" + UUID.randomUUID().toString().replace("-", "");
    db.execute(
        "CREATE FUNCTION "
            + fn
            + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.id='"
            + ids[1]
            + "'::uuid THEN RAISE EXCEPTION 'Fallo ficticio'; END IF; RETURN NEW; END $$");
    db.execute(
        "CREATE TRIGGER "
            + fn
            + " BEFORE UPDATE ON sesiones FOR EACH ROW EXECUTE FUNCTION "
            + fn
            + "()");
    try {
      assertThrows(
          Exception.class,
          () ->
              api.perform(
                  post("/api/v1/activities/" + ids[0] + "/close")
                      .header("Authorization", "Bearer " + owner)
                      .header("Idempotency-Key", UUID.randomUUID().toString())));
      assertEquals(
          "ABIERTA",
          db.queryForObject("SELECT estado FROM actividades WHERE id=?", String.class, ids[0]));
      assertEquals(
          "ACTIVA",
          db.queryForObject("SELECT estado FROM sesiones WHERE id=?", String.class, ids[1]));
      assertEquals(
          0,
          db.queryForObject(
              "SELECT count(*) FROM idempotencias WHERE actividad_id=? AND operacion='close-activity'",
              Integer.class,
              ids[0]));
    } finally {
      db.execute("DROP TRIGGER " + fn + " ON sesiones");
      db.execute("DROP FUNCTION " + fn + "()");
    }
  }
}
