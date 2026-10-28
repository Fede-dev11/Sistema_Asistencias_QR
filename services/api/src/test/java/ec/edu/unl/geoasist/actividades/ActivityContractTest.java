package ec.edu.unl.geoasist.actividades;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.*;
import java.io.FileReader;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;

class ActivityContractTest extends ClassSupport {
  private static final String YAML =
      "../../specs/001-asistencia-qr-dinamico/contracts/openapi.yaml";

  @SuppressWarnings("unchecked")
  private static Map<String, Object> schema() throws Exception {
    try (var in = new FileReader(YAML)) {
      Map<String, Object> doc = new Yaml().load(in);
      var components = (Map<String, Object>) doc.get("components");
      var schemas = (Map<String, Object>) components.get("schemas");
      return (Map<String, Object>) schemas.get("Activity");
    }
  }

  private static Set<String> keys(JsonNode n) {
    Set<String> k = new TreeSet<>();
    n.propertyNames().forEach(k::add);
    return k;
  }

  @SuppressWarnings("unchecked")
  private void check(JsonNode a, boolean event) throws Exception {
    var props = (Map<String, Object>) schema().get("properties");
    assertEquals(new TreeSet<>(props.keySet()), keys(a));
    if (event) assertTrue(Set.of("CERRADO", "ABIERTO").contains(a.get("acceso").asText()));
    else assertTrue(a.get("acceso").isNull());
    assertTrue(a.get("espacio_id").isString());
    assertTrue(
        List.of("QR_Y_GPS", "SOLO_QR", "SOLO_GPS", "NINGUNO")
            .contains(a.get("metodo_verificacion").asText()));
    assertTrue(a.get("ciclo_automatico").isBoolean());
    assertTrue(a.get("umbral_asistencia").isNumber());
  }

  @Test
  void getAndListItemsMatchActivitySchemaForClassAndEvent() throws Exception {
    String t = token(ScenarioFixtures.TEACHER);
    var clazz =
        createClass(t, List.of(P11), schedule(List.of(1, 3), "2026-11-02", "2026-11-27"), 201);
    var event =
        ok(
            send(
                "POST",
                "/api/v1/activities",
                t,
                Map.of(
                    "modalidad", "EVENTO",
                    "nombre", "Evento",
                    "espacio_id", SPACE,
                    "acceso", "CERRADO",
                    "inicio", "2026-10-21T13:00:00Z",
                    "fin", "2026-10-21T15:00:00Z")),
            201);
    for (var created : List.of(clazz, event)) {
      String id = created.get("id").asText();
      boolean isEvent = created == event;
      check(ok(send("GET", "/api/v1/activities/" + id, t, null), 200), isEvent);
      // La base es compartida: según el orden, la actividad puede no estar en la primera página.
      boolean found = false;
      for (int page = 0; !found; page++) {
        var items =
            ok(send("GET", "/api/v1/activities?size=100&page=" + page, t, null), 200).get("items");
        if (items.isEmpty()) break;
        for (var item : items)
          if (item.get("id").asText().equals(id)) {
            check(item, isEvent);
            found = true;
          }
      }
      assertTrue(found);
    }
  }
}
