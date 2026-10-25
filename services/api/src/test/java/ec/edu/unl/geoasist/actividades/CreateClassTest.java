package ec.edu.unl.geoasist.actividades;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ClassSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class CreateClassTest extends ClassSupport {
  private int count(String sql, String id) {
    return db.queryForObject(sql, Integer.class, id);
  }

  @Test
  void twoWeekdaysForFourWeeksMakeEightSessionsAndEnrolledList() throws Exception {
    String teacher = token(ScenarioFixtures.TEACHER);
    var out =
        createClass(
            teacher, List.of(P11), schedule(List.of(1, 3), "2026-11-02", "2026-11-27"), 201);
    assertEquals(8, out.get("sesiones").size());
    assertEquals("SOLO_QR", out.get("metodo_verificacion").asText());
    assertEquals("PROGRAMADA", out.get("sesiones").get(0).get("estado").asText());
    String id = out.get("id").asText();
    assertEquals(
        db.queryForObject(
            "SELECT count(*) FROM matriculas WHERE paralelo_id=?::uuid AND vigente",
            Integer.class,
            P11),
        count(
            "SELECT count(*) FROM estudiantes_autorizados WHERE actividad_id=?::uuid AND vigente",
            id));
    assertEquals(
        1, count("SELECT count(*) FROM actividad_paralelos WHERE actividad_id=?::uuid", id));
    assertEquals(
        1, count("SELECT count(*) FROM actividad_carreras WHERE actividad_id=?::uuid", id));
    assertEquals(
        8,
        count(
            "SELECT count(*) FROM sesiones WHERE actividad_id=?::uuid AND metodo_verificacion='SOLO_QR'",
            id));
    assertEquals(
        1,
        count(
            "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=?::uuid AND accion='CREAR_ACTIVIDAD'",
            id));
    var page = ok(send("GET", "/api/v1/activities/" + id + "/sessions?size=5", teacher, null), 200);
    assertEquals(8, page.get("total").asInt());
    assertEquals(5, page.get("items").size());
  }

  @Test
  void unassignedParallelStudentsAndEventsAreRejected() throws Exception {
    String teacher = token(ScenarioFixtures.TEACHER);
    var denied =
        ok(
            send(
                "POST",
                "/api/v1/activities",
                teacher,
                Map.of(
                    "modalidad",
                    "CLASE",
                    "nombre",
                    "X",
                    "espacio_id",
                    SPACE,
                    "paralelo_ids",
                    List.of(P12))),
            403);
    assertEquals("PARALELO_NO_ASIGNADO", denied.get("code").asText());
    createClass(token(ScenarioFixtures.STUDENT), List.of(P11), null, 403);
    ok(
        send(
            "POST",
            "/api/v1/activities",
            teacher,
            Map.of("modalidad", "EVENTO", "nombre", "X", "espacio_id", SPACE)),
        400);
    var e =
        createClass(
            teacher,
            List.of(P11),
            schedule(List.of(1, 2, 3, 4, 5, 6, 7), "2026-01-01", "2026-12-31"),
            400);
    assertEquals("HORARIO_DEMASIADO_LARGO", e.get("code").asText());
  }

  @Test
  void classWithoutScheduleAndGpsMethodNeedsLocatedSpace() throws Exception {
    String teacher = token(ScenarioFixtures.TEACHER);
    assertEquals(0, createClass(teacher, List.of(P11), null, 201).get("sesiones").size());
    String space = UUID.randomUUID().toString();
    db.update(
        "INSERT INTO espacios(id,nombre,tipo) VALUES (?::uuid,'Sin ubicacion','AULA')", space);
    var e =
        ok(
            send(
                "POST",
                "/api/v1/activities",
                teacher,
                Map.of(
                    "modalidad", "CLASE",
                    "nombre", "X",
                    "espacio_id", space,
                    "paralelo_ids", List.of(P11),
                    "metodo_verificacion", "SOLO_GPS")),
            409);
    assertEquals("ESPACIO_SIN_UBICACION", e.get("code").asText());
  }

  @Test
  void createIsIdempotentPerActorAndRejectsChangedPayload() throws Exception {
    String teacher = token(ScenarioFixtures.TEACHER);
    UUID key = UUID.randomUUID();
    var body = Map.of("modalidad", "CLASE", "nombre", "Idempotente", "espacio_id", SPACE, "paralelo_ids", List.of(P11));
    var first = ok(api.perform(post("/api/v1/activities").header("Authorization", "Bearer " + teacher)
        .header("Idempotency-Key", key).contentType("application/json").content(json.writeValueAsString(body))), 201);
    var replay = ok(api.perform(post("/api/v1/activities").header("Authorization", "Bearer " + teacher)
        .header("Idempotency-Key", key).contentType("application/json").content(json.writeValueAsString(body))), 200);
    assertEquals(first.get("id").asText(), replay.get("id").asText());
    var changed = new LinkedHashMap<String, Object>(body); changed.put("nombre", "Distinta");
    assertEquals("IDEMPOTENCIA_CONFLICTO", ok(api.perform(post("/api/v1/activities").header("Authorization", "Bearer " + teacher)
        .header("Idempotency-Key", key).contentType("application/json").content(json.writeValueAsString(changed))), 409).get("code").asText());
  }

  @Test
  void looseSessionAndStudentCreatesAlsoReplayPerActor() throws Exception {
    String teacher=token(ScenarioFixtures.TEACHER);
    String activity=createClass(teacher,List.of(P11),null,201).get("id").asText();
    UUID sessionKey=UUID.randomUUID();
    var sessionBody=Map.of("inicio","2026-11-02T13:00:00Z","fin","2026-11-02T15:00:00Z");
    var first=ok(api.perform(post("/api/v1/activities/"+activity+"/sessions").header("Authorization","Bearer "+teacher)
        .header("Idempotency-Key",sessionKey).contentType("application/json").content(json.writeValueAsString(sessionBody))),201);
    var replay=ok(api.perform(post("/api/v1/activities/"+activity+"/sessions").header("Authorization","Bearer "+teacher)
        .header("Idempotency-Key",sessionKey).contentType("application/json").content(json.writeValueAsString(sessionBody))),200);
    assertEquals(first.get("id").asText(),replay.get("id").asText());

    UUID studentKey=UUID.randomUUID(); var studentBody=Map.of("cedula","0100000074");
    var added=ok(api.perform(post("/api/v1/activities/"+activity+"/students").header("Authorization","Bearer "+teacher)
        .header("Idempotency-Key",studentKey).contentType("application/json").content(json.writeValueAsString(studentBody))),201);
    var addedReplay=ok(api.perform(post("/api/v1/activities/"+activity+"/students").header("Authorization","Bearer "+teacher)
        .header("Idempotency-Key",studentKey).contentType("application/json").content(json.writeValueAsString(studentBody))),200);
    assertEquals(added.get("persona_id").asText(),addedReplay.get("persona_id").asText());
  }
}
