package ec.edu.unl.geoasist.actividades;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ClassSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

class AuthorizedListTest extends ClassSupport {
  private static final String STUDENT_PERSON = ScenarioFixtures.id(102).toString();
  private static final String VALID =
      "SELECT count(*) FROM estudiantes_autorizados WHERE actividad_id=?::uuid AND vigente";

  private int enrolled() {
    return db.queryForObject(
        "SELECT count(*) FROM matriculas WHERE paralelo_id=?::uuid AND vigente",
        Integer.class,
        P11);
  }

  @Test
  void removeAddSyncAreAuditedAndSyncDoesNotRestoreRemoved() throws Exception {
    String teacher = token(ScenarioFixtures.TEACHER);
    String id = createClass(teacher, List.of(P11), null, 201).get("id").asText();
    String base = "/api/v1/activities/" + id + "/students";
    int enrolled = enrolled();
    ok(send("DELETE", base + "/" + STUDENT_PERSON, teacher, null), 204);
    assertEquals(enrolled - 1, db.queryForObject(VALID, Integer.class, id));
    assertEquals(0, ok(send("POST", base + "/sync", teacher, null), 200).get("agregados").asInt());
    assertEquals(enrolled - 1, db.queryForObject(VALID, Integer.class, id));
    String cedula =
        db.queryForObject(
            "SELECT cedula_normalizada FROM personas WHERE id=?::uuid",
            String.class,
            STUDENT_PERSON);
    ok(send("POST", base, teacher, Map.of("cedula", cedula)), 201);
    assertEquals(enrolled, db.queryForObject(VALID, Integer.class, id));
    ok(send("POST", base, teacher, Map.of("cedula", "1234567890")), 400);
    assertEquals(
        enrolled, ok(send("GET", base + "?size=100", teacher, null), 200).get("total").asInt());
    assertEquals(
        3,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=?::uuid AND accion IN ('QUITAR_ESTUDIANTE','AGREGAR_ESTUDIANTE','SINCRONIZAR_ESTUDIANTES')",
            Integer.class,
            id));
    ok(send("DELETE", base + "/" + STUDENT_PERSON, token("otro-docente@example.test"), null), 403);
  }

  @Test
  void syncAddsNewlyEnrolledStudents() throws Exception {
    String teacher = token(ScenarioFixtures.TEACHER);
    String id = createClass(teacher, List.of(P11), null, 201).get("id").asText();
    String person = ScenarioFixtures.id(106).toString();
    db.update(
        "INSERT INTO matriculas VALUES (?::uuid,?::uuid,true) ON CONFLICT DO NOTHING", person, P11);
    try {
      var sync =
          ok(send("POST", "/api/v1/activities/" + id + "/students/sync", teacher, null), 200);
      assertEquals(1, sync.get("agregados").asInt());
    } finally {
      db.update(
          "DELETE FROM matriculas WHERE persona_id=?::uuid AND paralelo_id=?::uuid", person, P11);
    }
  }
}
