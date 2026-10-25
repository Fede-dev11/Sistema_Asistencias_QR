package ec.edu.unl.geoasist.actividades;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import ec.edu.unl.geoasist.soporte.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CreateEventTest extends ClassSupport {
  private Map<String, Object> body(String name) {
    return Map.of(
        "modalidad",
        "EVENTO",
        "nombre",
        name,
        "espacio_id",
        SPACE,
        "acceso",
        "CERRADO",
        "inicio",
        "2026-10-21T13:00:00Z",
        "fin",
        "2026-10-21T15:00:00Z");
  }

  @Test
  void createsClosedEventWithSessionAndDefaultTolerance() throws Exception {
    var created =
        ok(
            send("POST", "/api/v1/activities", token(ScenarioFixtures.TEACHER), body("Evento")),
            201);
    assertEquals("CERRADO", created.get("acceso").asText());
    assertEquals(120, created.get("tolerancia_invitado_segundos").asInt());
    assertEquals(1, created.get("sesiones").size());
  }

  @Test
  void eventCreateReplaysAndConflictsOnDifferentBody() throws Exception {
    String bearer = token(ScenarioFixtures.TEACHER);
    UUID key = UUID.randomUUID();
    var first =
        ok(
            api.perform(
                post("/api/v1/activities")
                    .header("Authorization", "Bearer " + bearer)
                    .header("Idempotency-Key", key)
                    .contentType("application/json")
                    .content(json.writeValueAsString(body("Uno")))),
            201);
    var replay =
        ok(
            api.perform(
                post("/api/v1/activities")
                    .header("Authorization", "Bearer " + bearer)
                    .header("Idempotency-Key", key)
                    .contentType("application/json")
                    .content(json.writeValueAsString(body("Uno")))),
            200);
    assertEquals(first.get("id").asText(), replay.get("id").asText());
    assertEquals(
        "IDEMPOTENCIA_CONFLICTO",
        ok(
                api.perform(
                    post("/api/v1/activities")
                        .header("Authorization", "Bearer " + bearer)
                        .header("Idempotency-Key", key)
                        .contentType("application/json")
                        .content(json.writeValueAsString(body("Dos")))),
                409)
            .get("code")
            .asText());
  }

  @Test
  void accessCanChangeBeforeStartButNotAfterAndEventCanBeCancelledBeforeStart() throws Exception {
    String bearer = token(ScenarioFixtures.TEACHER);
    var started = new HashMap<>(body("Editable"));
    started.put("inicio", "2026-10-19T00:00:00Z");
    started.put("fin", "2026-10-21T00:00:00Z");
    var editable = ok(send("POST", "/api/v1/activities", bearer, started), 201);
    String activity = editable.get("id").asText();
    String session = editable.get("sesiones").get(0).get("id").asText();
    assertEquals(
        "ABIERTO",
        ok(
                send(
                    "PATCH", "/api/v1/activities/" + activity, bearer, Map.of("acceso", "ABIERTO")),
                200)
            .get("acceso")
            .asText());
    start(UUID.fromString(session), bearer);
    ok(send("PATCH", "/api/v1/activities/" + activity, bearer, Map.of("acceso", "CERRADO")), 409);
    ok(
        send("POST", "/api/v1/sessions/" + session + "/close", bearer, null),
        200); // no dejar sesión activa para otras pruebas

    var cancellable = ok(send("POST", "/api/v1/activities", bearer, body("Cancelable")), 201);
    assertEquals(
        "CANCELADA",
        ok(
                send(
                    "POST",
                    "/api/v1/activities/" + cancellable.get("id").asText() + "/cancel",
                    bearer,
                    null),
                200)
            .get("estado")
            .asText());
  }
}
