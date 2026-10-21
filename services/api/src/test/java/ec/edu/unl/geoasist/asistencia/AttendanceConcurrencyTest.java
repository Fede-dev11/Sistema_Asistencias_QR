package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AttendanceConcurrencyTest extends PostgresFixture {
  @org.springframework.beans.factory.annotation.Autowired
  ec.edu.unl.geoasist.identidades.aplicacion.AccountAssociationService accounts;

  @org.springframework.beans.factory.annotation.Autowired
  org.springframework.transaction.PlatformTransactionManager manager;

  private org.springframework.transaction.support.TransactionTemplate transactions() {
    return new org.springframework.transaction.support.TransactionTemplate(manager);
  }

  @Test
  void oneAttendanceAcrossOneHundredRunsOfOneHundredRequests() throws Exception {
    int runs = Integer.getInteger("geoasist.concurrency.runs", 100);
    String owner = token(ScenarioFixtures.TEACHER), student = token(ScenarioFixtures.STUDENT);
    var pool = java.util.concurrent.Executors.newFixedThreadPool(100);
    try {
      for (int round = 0; round < runs; round++) {
        var ids = scenario("CLASE", 200);
        db.update(
            "INSERT INTO estudiantes_autorizados VALUES (?,?,true)",
            ids[0],
            ScenarioFixtures.id(102));
        start(ids[1], owner);
        String qr = qr(ids[1], owner);
        String body = json.writeValueAsString(Map.of("qr_token", qr));
        var ready = new java.util.concurrent.CountDownLatch(1);
        List<java.util.concurrent.Future<Integer>> requests = new ArrayList<>();
        for (int i = 0; i < 100; i++)
          requests.add(
              pool.submit(
                  () -> {
                    ready.await();
                    return api.perform(
                            post("/api/v1/attendance/attempts")
                                .header("Authorization", "Bearer " + student)
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType("application/json")
                                .content(body))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                  }));
        ready.countDown();
        int created = 0;
        for (var future : requests) {
          int status = future.get(60, java.util.concurrent.TimeUnit.SECONDS);
          assertTrue(status == 201 || status == 200, "Estado concurrente: " + status);
          if (status == 201) created++;
        }
        assertEquals(1, created);
        assertEquals(
            1,
            db.queryForObject(
                "SELECT count(*) FROM asistencias WHERE sesion_id=?", Integer.class, ids[1]));
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void oneFormCannotConfirmTwoDifferentPeople() throws Exception {
    String a = fictionalId(), b = fictionalId();
    var event = guests(120, a, b);
    String secret = open(event).get("registro_invitado_token").asText();
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    var ready = new java.util.concurrent.CountDownLatch(1);
    try {
      var first =
          pool.submit(
              () -> {
                ready.await();
                return guestSubmit(secret, a, UUID.randomUUID())
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var second =
          pool.submit(
              () -> {
                ready.await();
                return guestSubmit(secret, b, UUID.randomUUID())
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      ready.countDown();
      var outcomes = List.of(first.get(), second.get());
      assertEquals(1, outcomes.stream().filter(x -> x == 201).count());
      assertTrue(outcomes.contains(409));
      assertEquals(
          1,
          db.queryForObject(
              "SELECT count(*) FROM asistencias WHERE sesion_id=?",
              Integer.class,
              event.session()));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void accountCreationWinningPersonLockBlocksGuestSubmit() throws Exception {
    String cedula = fictionalId();
    var event = guests(120, cedula);
    String secret = open(event).get("registro_invitado_token").asText();
    UUID person =
        db.queryForObject("SELECT id FROM personas WHERE cedula_normalizada=?", UUID.class, cedula);
    var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
    var pending =
        new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<Integer>>();
    try {
      transactions()
          .executeWithoutResult(
              status -> {
                db.queryForList("SELECT id FROM personas WHERE id=? FOR UPDATE", person);
                pending.set(
                    pool.submit(
                        () ->
                            guestSubmit(secret, cedula, UUID.randomUUID())
                                .andReturn()
                                .getResponse()
                                .getStatus()));
                accounts.associate(
                    person, "DEV", "race-" + UUID.randomUUID() + "@example.test", null);
              });
      assertEquals(403, pending.get().get(15, java.util.concurrent.TimeUnit.SECONDS));
      assertEquals(
          0,
          db.queryForObject(
              "SELECT count(*) FROM asistencias WHERE sesion_id=?",
              Integer.class,
              event.session()));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void twoIndependentGuestFormsForOnePersonShareOneAttendance() throws Exception {
    String cedula = fictionalId();
    var event = guests(120, cedula);
    String firstSecret = open(event).get("registro_invitado_token").asText();
    String secondSecret = open(event).get("registro_invitado_token").asText();
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    var ready = new java.util.concurrent.CountDownLatch(1);
    try {
      var first =
          pool.submit(
              () -> {
                ready.await();
                return guestSubmit(firstSecret, cedula, UUID.randomUUID())
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var second =
          pool.submit(
              () -> {
                ready.await();
                return guestSubmit(secondSecret, cedula, UUID.randomUUID())
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      ready.countDown();
      var outcomes = List.of(first.get(), second.get());
      assertEquals(1, outcomes.stream().filter(status -> status == 201).count());
      assertEquals(1, outcomes.stream().filter(status -> status == 200).count());
      assertEquals(
          1,
          db.queryForObject(
              "SELECT count(*) FROM asistencias WHERE sesion_id=?",
              Integer.class,
              event.session()));
      assertEquals(
          1,
          db.queryForObject(
              "SELECT count(DISTINCT recibo_asistencia_id) FROM inicios_invitado WHERE sesion_id=? AND estado='CONSUMIDO'",
              Integer.class,
              event.session()));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void rosterReplacementRacingGuestSubmissionHasOneCoherentWinner() throws Exception {
    String cedula = fictionalId();
    var event = guests(120, cedula);
    String secret = open(event).get("registro_invitado_token").asText();
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    var ready = new java.util.concurrent.CountDownLatch(1);
    try {
      var submit =
          pool.submit(
              () -> {
                ready.await();
                return guestSubmit(secret, cedula, UUID.randomUUID())
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var replace =
          pool.submit(
              () -> {
                ready.await();
                return api.perform(
                        put("/api/v1/activities/" + event.activity() + "/guest-list")
                            .header("Authorization", "Bearer " + event.owner())
                            .contentType("application/json")
                            .content("{\"invitados\":[]}"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      ready.countDown();
      int submitStatus = submit.get();
      assertEquals(200, replace.get());
      assertTrue(submitStatus == 201 || submitStatus == 403, "Estado: " + submitStatus);
      assertEquals(
          submitStatus == 201 ? 1 : 0,
          db.queryForObject(
              "SELECT count(*) FROM asistencias WHERE sesion_id=?",
              Integer.class,
              event.session()));
      assertEquals(
          0,
          db.queryForObject(
              "SELECT count(*) FROM invitados_evento WHERE actividad_id=?",
              Integer.class,
              event.activity()));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void definitiveClosureRacingGuestSubmissionNeverCreatesAfterClosure() throws Exception {
    String cedula = fictionalId();
    var event = guests(120, cedula);
    String secret = open(event).get("registro_invitado_token").asText();
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    var ready = new java.util.concurrent.CountDownLatch(1);
    try {
      var submit =
          pool.submit(
              () -> {
                ready.await();
                return guestSubmit(secret, cedula, UUID.randomUUID())
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var close =
          pool.submit(
              () -> {
                ready.await();
                return api.perform(
                        post("/api/v1/activities/" + event.activity() + "/close")
                            .header("Authorization", "Bearer " + event.owner())
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      ready.countDown();
      int submitStatus = submit.get();
      assertEquals(200, close.get());
      assertTrue(submitStatus == 201 || submitStatus == 409, "Estado: " + submitStatus);
      assertEquals(
          submitStatus == 201 ? 1 : 0,
          db.queryForObject(
              "SELECT count(*) FROM asistencias WHERE sesion_id=?",
              Integer.class,
              event.session()));
      assertEquals(
          "CERRADA",
          db.queryForObject(
              "SELECT estado FROM actividades WHERE id=?", String.class, event.activity()));
      assertEquals(
          "CERRADA",
          db.queryForObject(
              "SELECT estado FROM sesiones WHERE id=?", String.class, event.session()));
    } finally {
      pool.shutdownNow();
    }
  }
}
