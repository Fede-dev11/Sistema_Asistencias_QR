package ec.edu.unl.geoasist.academico;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

class ParallelReadTest extends AdminSupport {
  private static final String P11 = ScenarioFixtures.id(11).toString();

  @Test
  void enrollmentsAndTeachersArePagedWithVigente() throws Exception {
    String admin = admin();
    var teachers =
        ok(send("GET", "/api/v1/admin/parallels/" + P11 + "/teachers", admin, null), 200);
    assertEquals(
        db.queryForObject(
            "SELECT count(*) FROM docentes_paralelo WHERE paralelo_id=?::uuid", Integer.class, P11),
        teachers.get("total").asInt());
    if (teachers.get("total").asInt() > 0) {
      var t = teachers.get("items").get(0);
      assertTrue(t.get("correo").asText().contains("@"));
      assertFalse(t.get("nombres").asText().isBlank());
      assertTrue(t.has("vigente"));
    }

    int enrolled =
        db.queryForObject(
            "SELECT count(*) FROM matriculas WHERE paralelo_id=?::uuid", Integer.class, P11);
    var page =
        ok(send("GET", "/api/v1/admin/parallels/" + P11 + "/enrollments?size=1", admin, null), 200);
    assertEquals(enrolled, page.get("total").asInt());
    assertEquals(Math.min(1, enrolled), page.get("items").size());
    if (enrolled > 0) {
      var e = page.get("items").get(0);
      assertFalse(e.get("cedula").asText().isBlank());
      assertTrue(e.has("vigente"));
    }

    String student = token(ScenarioFixtures.STUDENT);
    ok(send("GET", "/api/v1/admin/parallels/" + P11 + "/enrollments", student, null), 403);
    ok(send("GET", "/api/v1/admin/parallels/" + P11 + "/teachers", student, null), 403);
    ok(send("GET", "/api/v1/admin/parallels/" + UUID.randomUUID() + "/teachers", admin, null), 404);
    ok(send("GET", "/api/v1/admin/parallels/" + P11 + "/enrollments?size=101", admin, null), 400);
  }
}
