package ec.edu.unl.geoasist.actividades;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ClassSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

class GuestReadTest extends ClassSupport {
  @Test
  void listAndSettingsReturnWhatWasSavedWithinScope() throws Exception {
    UUID[] ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    String base = "/api/v1/activities/" + ids[0];
    var empty = ok(send("GET", base + "/guest-list", owner, null), 200);
    assertEquals(0, empty.get("total").asInt());
    assertEquals(0, empty.get("items").size());
    ok(
        send(
            "PUT",
            base + "/guest-list",
            owner,
            Map.of(
                "invitados",
                List.of(
                    Map.of("cedula", "0123456782", "nombres", "Invitado Uno"),
                    Map.of("cedula", "2400000002", "nombres", "Invitado Dos")))),
        200);
    ok(
        send(
            "PUT",
            base + "/guest-settings",
            owner,
            Map.of("habilitado", true, "tolerancia_invitado_segundos", 90)),
        200);
    var list = ok(send("GET", base + "/guest-list?size=1&page=0", owner, null), 200);
    assertEquals(2, list.get("total").asInt());
    assertEquals(0, list.get("page").asInt());
    assertEquals(1, list.get("items").size());
    var row = list.get("items").get(0);
    assertEquals("0123456782", row.get("cedula").asText());
    assertEquals("Invitado Uno", row.get("nombres").asText());
    assertTrue(row.get("activo").asBoolean());
    var settings = ok(send("GET", base + "/guest-settings", owner, null), 200);
    assertTrue(settings.get("habilitado").asBoolean());
    assertEquals(90, settings.get("tolerancia_invitado_segundos").asInt());

    ok(send("GET", base + "/guest-list", token(ScenarioFixtures.DIRECTOR), null), 200);
    ok(send("GET", base + "/guest-list", token("otro-docente@example.test"), null), 403);
    ok(send("GET", base + "/guest-settings", token("otro-docente@example.test"), null), 403);
    ok(send("GET", base + "/guest-list", token(ScenarioFixtures.STUDENT), null), 403);
    for (String q : List.of("size=0", "size=101", "page=-1"))
      ok(send("GET", base + "/guest-list?" + q, owner, null), 400);
    ok(send("GET", "/api/v1/activities/" + UUID.randomUUID() + "/guest-list", owner, null), 404);
  }
}
