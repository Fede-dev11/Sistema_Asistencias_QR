package ec.edu.unl.geoasist.sesiones;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ClassSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

class SessionEditingTest extends ClassSupport {
  private String teacherInClass() throws Exception {
    clock.set(IN_CLASS);
    return token(ScenarioFixtures.TEACHER);
  }

  @Test
  void editCancelAndNonEditableStates() throws Exception {
    String teacher = teacherInClass();
    var all = List.of(1, 2, 3, 4, 5, 6, 7);
    var created =
        createClass(teacher, List.of(P11), schedule(all, "2026-10-20", "2026-10-21"), 201);
    String first = created.get("sesiones").get(0).get("id").asText();
    String second = created.get("sesiones").get(1).get("id").asText();
    var edited =
        ok(
            send(
                "PATCH",
                "/api/v1/sessions/" + first,
                teacher,
                Map.of("inicio", "2026-10-20T13:10:00Z", "fin", "2026-10-20T15:00:00Z")),
            200);
    assertEquals("2026-10-20T13:10:00Z", edited.get("inicio").asText());
    ok(
        send(
            "PATCH",
            "/api/v1/sessions/" + first,
            teacher,
            Map.of("inicio", "2026-10-20T16:00:00Z", "fin", "2026-10-20T15:00:00Z")),
        400);
    ok(send("POST", "/api/v1/sessions/" + second + "/cancel", teacher, null), 200);
    assertEquals(
        "SESION_NO_EDITABLE",
        ok(
                send(
                    "PATCH",
                    "/api/v1/sessions/" + second,
                    teacher,
                    Map.of("fin", "2026-10-21T20:00:00Z")),
                409)
            .get("code")
            .asText());
    ok(send("POST", "/api/v1/sessions/" + first + "/start", teacher, null), 200);
    assertEquals(
        "SESION_NO_EDITABLE",
        ok(send("POST", "/api/v1/sessions/" + first + "/cancel", teacher, null), 409)
            .get("code")
            .asText());
    ok(send("POST", "/api/v1/sessions/" + second + "/start", teacher, null), 409);
    ok(send("POST", "/api/v1/sessions/" + second + "/close", teacher, null), 409);
    var loose =
        ok(
            send(
                "POST",
                "/api/v1/activities/" + created.get("id").asText() + "/sessions",
                teacher,
                Map.of("inicio", "2026-12-01T14:00:00Z", "fin", "2026-12-01T15:00:00Z")),
            201);
    assertTrue(loose.get("horario_id").isNull());
    ok(
        send("PATCH", "/api/v1/sessions/" + first, token("otro-docente@example.test"), Map.of()),
        403);
  }

  @Test
  void replaceScheduleRegeneratesOnlyUntouchedFutureSessions() throws Exception {
    String teacher = teacherInClass();
    var created =
        createClass(
            teacher,
            List.of(P11),
            schedule(List.of(1, 2, 3, 4, 5, 6, 7), "2026-10-20", "2026-10-22"),
            201);
    String id = created.get("id").asText();
    var s = created.get("sesiones");
    db.update(
        "INSERT INTO tokens_qr VALUES (gen_random_uuid(),?::uuid,repeat('a',64),now(),now()+interval '40 seconds')",
        s.get(1).get("id").asText());
    ok(
        send("POST", "/api/v1/sessions/" + s.get(0).get("id").asText() + "/start", teacher, null),
        200);
    var out =
        ok(
            send(
                "PUT",
                "/api/v1/activities/" + id + "/schedule",
                teacher,
                schedule(List.of(1, 2, 3, 4, 5, 6, 7), "2026-10-23", "2026-10-25")),
            200);
    assertEquals(1, out.get("eliminadas").asInt());
    assertEquals(3, out.get("generadas").asInt());
    assertEquals(
        5,
        db.queryForObject(
            "SELECT count(*) FROM sesiones WHERE actividad_id=?::uuid", Integer.class, id));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM horarios_clase WHERE actividad_id=?::uuid", Integer.class, id));
  }

  @Test
  void cancelActivityOnlyBeforeAnySessionStarted() throws Exception {
    String teacher = teacherInClass();
    var all = List.of(1, 2, 3, 4, 5, 6, 7);
    var created =
        createClass(teacher, List.of(P11), schedule(all, "2026-10-20", "2026-10-21"), 201);
    String id = created.get("id").asText();
    var out = ok(send("POST", "/api/v1/activities/" + id + "/cancel", teacher, null), 200);
    assertEquals("CANCELADA", out.get("estado").asText());
    assertFalse(out.get("anonimizar_en").isNull());
    assertEquals(
        2,
        db.queryForObject(
            "SELECT count(*) FROM sesiones WHERE actividad_id=?::uuid AND estado='CANCELADA'",
            Integer.class,
            id));
    ok(send("POST", "/api/v1/activities/" + id + "/close", teacher, null), 409);
    ok(
        send(
            "PUT",
            "/api/v1/activities/" + id + "/schedule",
            teacher,
            schedule(all, "2026-11-02", "2026-11-03")),
        409);

    var started =
        createClass(teacher, List.of(P11), schedule(all, "2026-10-20", "2026-10-21"), 201);
    ok(
        send(
            "POST",
            "/api/v1/sessions/" + started.get("sesiones").get(0).get("id").asText() + "/start",
            teacher,
            null),
        200);
    String sid = started.get("id").asText();
    ok(send("POST", "/api/v1/activities/" + sid + "/cancel", teacher, null), 409);
    var patch =
        ok(
            send("PATCH", "/api/v1/activities/" + sid, teacher, Map.of("nombre", "Nuevo nombre")),
            200);
    assertEquals("Nuevo nombre", patch.get("nombre").asText());
  }
}
