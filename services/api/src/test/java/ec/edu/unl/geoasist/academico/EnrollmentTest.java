package ec.edu.unl.geoasist.academico;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.*;
import org.junit.jupiter.api.Test;

class EnrollmentTest extends AdminSupport {
  @Test
  void replacesEnrollmentsAndTeachersAndTeacherSeesCounts() throws Exception {
    String a = admin();
    String code = "T" + UUID.randomUUID().toString().substring(0, 8);
    var career = ok(send("POST", "/api/v1/admin/careers", a, Map.of("codigo", code, "nombre", "Carrera de prueba")), 201);
    send("POST", "/api/v1/admin/careers", a, Map.of("codigo", code, "nombre", "Repetida")).andExpect(status().isConflict());
    String careerId = career.get("id").asText();
    var parallel = ok(send("POST", "/api/v1/admin/careers/" + careerId + "/parallels", a, Map.of("codigo", "B")), 201);
    send("POST", "/api/v1/admin/careers/" + careerId + "/parallels", a, Map.of("codigo", "B")).andExpect(status().isConflict());
    String pid = parallel.get("id").asText();

    String s1 = fictionalId(), s2 = fictionalId(), nonStudent = fictionalId();
    for (String c : List.of(s1, s2)) {
      var p = createPerson(a, c, "Estudiante " + c);
      createAccount(a, p.get("id").asText(), "e" + c + "@example.test", List.of("ESTUDIANTE"));
    }
    var np = createPerson(a, nonStudent, "Sin rol");
    createAccount(a, np.get("id").asText(), "n" + nonStudent + "@example.test", List.of("DOCENTE"));

    var res = ok(send("PUT", "/api/v1/admin/parallels/" + pid + "/enrollments", a, Map.of("cedulas", List.of(s1, s2, nonStudent, "0100000018"))), 200);
    assertEquals(2, res.get("matriculados").asInt());
    assertEquals(2, res.get("rechazadas").size());

    var t = ok(send("PUT", "/api/v1/admin/parallels/" + pid + "/teachers", a, Map.of("correos", List.of("docente@example.test", "estudiante@example.test"))), 200);
    assertEquals(1, t.get("docentes").asInt());
    assertEquals(1, t.get("rechazadas").size());

    String teacher = token("docente@example.test");
    JsonNodeFinder.assertCount(ok(send("GET", "/api/v1/me/parallels", teacher, null), 200), pid, 2);

    ok(send("PUT", "/api/v1/admin/parallels/" + pid + "/enrollments", a, Map.of("cedulas", List.of(s1))), 200);
    JsonNodeFinder.assertCount(ok(send("GET", "/api/v1/me/parallels", teacher, null), 200), pid, 1);
    ok(send("PUT", "/api/v1/admin/parallels/" + pid + "/teachers", a, Map.of("correos", List.of())), 200);
    assertFalse(ok(send("GET", "/api/v1/me/parallels", teacher, null), 200).toString().contains(pid));

    send("GET", "/api/v1/me/parallels", token("estudiante@example.test"), null).andExpect(status().isForbidden());
    send("PUT", "/api/v1/admin/parallels/" + UUID.randomUUID() + "/enrollments", a, Map.of("cedulas", List.of())).andExpect(status().isNotFound());
  }

  static final class JsonNodeFinder {
    static void assertCount(tools.jackson.databind.JsonNode body, String parallel, int expected) {
      for (var item : body.get("items"))
        if (item.get("paralelo_id").asText().equals(parallel)) {
          assertEquals(expected, item.get("matriculados").asInt());
          return;
        }
      fail("paralelo no listado");
    }
  }
}
