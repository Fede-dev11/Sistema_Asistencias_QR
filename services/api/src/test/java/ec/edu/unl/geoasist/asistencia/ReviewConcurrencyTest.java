package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class ReviewConcurrencyTest extends PostgresFixture {
  @org.springframework.beans.factory.annotation.Autowired
  ec.edu.unl.geoasist.compartido.aplicacion.OpaqueSecrets secrets;

  @Test
  void approvalRejectsDeviceAlreadyUsedByAnotherPerson() throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    start(ids[1], owner);
    UUID personA = db.queryForObject(
        "SELECT persona_id FROM cuentas WHERE identificador=?", UUID.class,
        ScenarioFixtures.STUDENT);
    UUID personB = db.queryForObject(
        "SELECT persona_id FROM cuentas WHERE identificador=?", UUID.class,
        ScenarioFixtures.DIRECTOR);
    String deviceHash = secrets.hmac("device:" + UUID.randomUUID());
    UUID accepted = UUID.randomUUID(), attendance = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,dispositivo_hash,canal,recibido_en) VALUES (?,?,?,'Persona A','ACEPTADO','ASISTENCIA_CONFIRMADA','Registrada',?,'CREDENCIALES',?)",
        accepted, ids[1], personA, deviceHash, Timestamp.from(clock.instant()));
    db.update(
        "INSERT INTO asistencias(id,sesion_id,persona_id,intento_id,registrada_en,forma,nombres_registro,dispositivo_hash) VALUES (?,?,?,?,?,'CREDENCIALES','Persona A',?)",
        attendance, ids[1], personA, accepted, Timestamp.from(clock.instant()), deviceHash);
    UUID pending = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,dispositivo_hash,canal,recibido_en) VALUES (?,?,?,'Persona B','PENDIENTE_REVISION','UBICACION_INCIERTA','Pendiente',?,'CREDENCIALES',?)",
        pending, ids[1], personB, deviceHash, Timestamp.from(clock.instant()));

    Response response = reviewResult(pending, owner, "APROBAR", UUID.randomUUID());
    assertEquals(409, response.status());
    assertTrue(response.body().contains("DISPOSITIVO_YA_REGISTRADO"));
    assertEquals(0, db.queryForObject(
        "SELECT count(*) FROM asistencias WHERE intento_id=?", Integer.class, pending));
  }

  @Test
  void concurrentReviewersCreateOneImmutableDecisionAndAtMostOneAttendance() throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    start(ids[1], owner);
    UUID attempt = pending(ids[1]);
    var pool = Executors.newFixedThreadPool(2);
    var ready = new CountDownLatch(1);
    try {
      var approve = pool.submit(() -> review(attempt, owner, "APROBAR", UUID.randomUUID(), ready));
      var reject = pool.submit(() -> review(attempt, owner, "RECHAZAR", UUID.randomUUID(), ready));
      ready.countDown();
      var statuses = List.of(approve.get(), reject.get());
      assertEquals(1, statuses.stream().filter(status -> status == 200).count());
      assertEquals(1, statuses.stream().filter(status -> status == 409).count());
      assertEquals(
          1,
          db.queryForObject(
              "SELECT count(*) FROM revisiones_asistencia WHERE intento_id=?",
              Integer.class,
              attempt));
      String decision =
          db.queryForObject(
              "SELECT decision FROM revisiones_asistencia WHERE intento_id=?",
              String.class,
              attempt);
      assertEquals(
          decision.equals("APROBAR") ? 1 : 0,
          db.queryForObject(
              "SELECT count(*) FROM asistencias WHERE intento_id=?", Integer.class, attempt));
      assertEquals(
          1,
          db.queryForObject(
              "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=? AND accion='REVISAR_INTENTO'",
              Integer.class,
              ids[0]));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void replayIsStableAndDifferentPayloadConflicts() throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    start(ids[1], owner);
    UUID attempt = pending(ids[1]);
    UUID key = UUID.randomUUID();
    var first = reviewResult(attempt, owner, "APROBAR", key);
    var replay = reviewResult(attempt, owner, "APROBAR", key);
    assertEquals(200, first.status());
    assertEquals(200, replay.status());
    assertEquals(json.readTree(first.body()), json.readTree(replay.body()));
    assertEquals(409, reviewResult(attempt, owner, "RECHAZAR", key).status());
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE intento_id=?", Integer.class, attempt));
  }

  @Test
  void afterCloseApprovalWithinTheCorrectionWindowCreatesAttendanceAndRejectionDoesNot()
      throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    start(ids[1], owner);
    UUID approval = pending(ids[1]);
    UUID rejection = pending(ids[1]);
    api.perform(
            post("/api/v1/sessions/" + ids[1] + "/close")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

    // 003 US1-2: dentro del plazo de corrección la aprobación posterior al cierre ya es válida.
    assertEquals(200, reviewResult(rejection, owner, "RECHAZAR", UUID.randomUUID()).status());
    assertEquals(200, reviewResult(approval, owner, "APROBAR", UUID.randomUUID()).status());
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE intento_id=?", Integer.class, approval));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE intento_id=?", Integer.class, rejection));
  }

  private int review(UUID attempt, String owner, String decision, UUID key, CountDownLatch ready)
      throws Exception {
    ready.await();
    return reviewResult(attempt, owner, decision, key).status();
  }

  @Test
  void approvingGuestConsumesItsBoundCapabilityAndRechecksAccountAndList() throws Exception {
    String cedula = fictionalId();
    var event = guests(120, cedula);
    var opened = open(event);
    UUID start =
        db.queryForObject(
            "SELECT id FROM inicios_invitado WHERE sesion_id=?", UUID.class, event.session());
    UUID person =
        db.queryForObject("SELECT id FROM personas WHERE cedula_normalizada=?", UUID.class, cedula);
    db.update("UPDATE inicios_invitado SET persona_vinculada_id=? WHERE id=?", person, start);
    UUID attempt = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en,inicio_invitado_id) VALUES (?,?,?,'Invitado ficticio','PENDIENTE_REVISION','UBICACION_INCIERTA','Pendiente','INVITADO_LISTADO',?,?)",
        attempt,
        event.session(),
        person,
        Timestamp.from(clock.instant()),
        start);
    assertEquals(200, reviewResult(attempt, event.owner(), "APROBAR", UUID.randomUUID()).status());
    assertEquals(
        "CONSUMIDO",
        db.queryForObject("SELECT estado FROM inicios_invitado WHERE id=?", String.class, start));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE sesion_id=? AND persona_id=?",
            Integer.class,
            event.session(),
            person));
    assertEquals(
        200,
        guestSubmit(opened.get("registro_invitado_token").asText(), cedula, UUID.randomUUID())
            .andReturn()
            .getResponse()
            .getStatus());
  }

  private Response reviewResult(UUID attempt, String owner, String decision, UUID key)
      throws Exception {
    var result =
        api.perform(
                post("/api/v1/attempts/" + attempt + "/review")
                    .header("Authorization", "Bearer " + owner)
                    .header("Idempotency-Key", key.toString())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("decision", decision, "razon", "Revisión ficticia"))))
            .andReturn();
    return new Response(
        result.getResponse().getStatus(), result.getResponse().getContentAsString());
  }

  private UUID pending(UUID session) {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,?,?,?,?,?,?)",
        id,
        session,
        ScenarioFixtures.id(102),
        "Persona pendiente ficticia",
        "PENDIENTE_REVISION",
        "UBICACION_INCIERTA",
        "Revisión necesaria.",
        "CREDENCIALES",
        Timestamp.from(clock.instant()));
    return id;
  }

  private record Response(int status, String body) {}
}
