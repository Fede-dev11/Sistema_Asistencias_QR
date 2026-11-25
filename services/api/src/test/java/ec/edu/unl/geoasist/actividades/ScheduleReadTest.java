package ec.edu.unl.geoasist.actividades;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ClassSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

class ScheduleReadTest extends ClassSupport {
  @Test
  void returnsScheduleOrSinHorario() throws Exception {
    String teacher = token(ScenarioFixtures.TEACHER);
    String id =
        createClass(teacher, List.of(P11), schedule(List.of(1, 3), "2026-10-20", "2026-11-20"), 201)
            .get("id")
            .asText();
    String path = "/api/v1/activities/" + id + "/schedule";
    var s = ok(send("GET", path, teacher, null), 200);
    assertEquals(1, s.get("dias").get(0).asInt());
    assertEquals(3, s.get("dias").get(1).asInt());
    assertEquals("08:00", s.get("hora_inicio").asText());
    assertEquals("10:00", s.get("hora_fin").asText());
    assertEquals("2026-10-20", s.get("desde").asText());
    assertEquals("2026-11-20", s.get("hasta").asText());
    assertEquals(SPACE, s.get("espacio_id").asText());
    ok(send("GET", path, token(ScenarioFixtures.DIRECTOR), null), 200);
    ok(send("GET", path, token("otro-docente@example.test"), null), 403);

    String bare = createClass(teacher, List.of(P11), null, 201).get("id").asText();
    var missing = ok(send("GET", "/api/v1/activities/" + bare + "/schedule", teacher, null), 404);
    assertEquals("SIN_HORARIO", missing.get("code").asText());
  }
}
