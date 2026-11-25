package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ClassSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

class ActiveSessionsTest extends ClassSupport {
  private String session(String teacher, String name, String method, boolean start)
      throws Exception {
    var body = new LinkedHashMap<String, Object>();
    body.put("modalidad", "CLASE");
    body.put("nombre", name);
    body.put("espacio_id", SPACE);
    body.put("paralelo_ids", List.of(P11));
    body.put("metodo_verificacion", method);
    body.put("horario", schedule(List.of(1, 2, 3, 4, 5, 6, 7), "2026-10-20", "2026-10-21"));
    String id =
        ok(send("POST", "/api/v1/activities", teacher, body), 201)
            .get("sesiones")
            .get(0)
            .get("id")
            .asText();
    if (start) {
      if (method.contains("GPS"))
        ec.edu.unl.geoasist.sesiones.VerificationMethodTest.enableGps(db, id);
      ok(send("POST", "/api/v1/sessions/" + id + "/start", teacher, null), 200);
    }
    return id;
  }

  @Test
  void listsOnlyActiveSessionsWhereTheParticipantMayRegister() throws Exception {
    clock.set(IN_CLASS);
    clock.set(IN_CLASS);
    String teacher = token(ScenarioFixtures.TEACHER);
    String student = token(ScenarioFixtures.STUDENT);
    String gps = session(teacher, "Activa GPS", "SOLO_GPS", true);
    String none = session(teacher, "Activa sin método", "NINGUNO", true);
    String scheduled = session(teacher, "Programada", "SOLO_QR", false);
    String closed = session(teacher, "Cerrada", "NINGUNO", true);
    ok(send("POST", "/api/v1/sessions/" + closed + "/close", teacher, null), 200);
    String cancelled = session(teacher, "Cancelada", "SOLO_QR", false);
    ok(send("POST", "/api/v1/sessions/" + cancelled + "/cancel", teacher, null), 200);

    // La base es compartida entre clases de prueba: otras dejan eventos abiertos activos.
    var byId = all(student);
    assertTrue(byId.keySet().containsAll(Set.of(gps, none)));
    for (String excluded : List.of(scheduled, closed, cancelled))
      assertFalse(byId.containsKey(excluded), excluded);
    assertTrue(byId.get(gps).get("requiere_ubicacion").asBoolean());
    assertEquals("SOLO_GPS", byId.get(gps).get("metodo_verificacion").asText());
    assertFalse(byId.get(none).get("requiere_ubicacion").asBoolean());

    // El docente no está autorizado como estudiante en esas clases.
    var forTeacher = all(teacher).keySet();
    assertFalse(forTeacher.contains(gps) || forTeacher.contains(none));
    ok(send("GET", "/api/v1/me/active-sessions?size=0", student, null), 400);
  }

  @Test
  void closedEventsAreListedOnlyForActiveGuestsAndOpenEventsForEveryone() throws Exception {
    clock.set(IN_CLASS);
    String student = token(ScenarioFixtures.STUDENT);
    String teacher = token(ScenarioFixtures.TEACHER);
    String studentId =
        db.queryForObject(
            "SELECT p.cedula_normalizada FROM personas p JOIN cuentas c ON c.persona_id=p.id WHERE c.identificador=?",
            String.class,
            ScenarioFixtures.STUDENT);
    var onList = guests(120, studentId);
    var offList = guests(120, fictionalId()); // cerrado, sin la persona
    var open = guests(120, fictionalId());
    db.update("UPDATE actividades SET acceso='ABIERTO' WHERE id=?", open.activity());

    var forStudent = all(student).keySet();
    assertTrue(forStudent.contains(onList.session().toString()));
    assertTrue(forStudent.contains(open.session().toString()));
    assertFalse(forStudent.contains(offList.session().toString()));
    var forTeacher = all(teacher).keySet();
    assertTrue(forTeacher.contains(open.session().toString()));
    assertFalse(forTeacher.contains(onList.session().toString()));
    assertFalse(forTeacher.contains(offList.session().toString()));

    db.update("UPDATE invitados_evento SET activo=false WHERE actividad_id=?", onList.activity());
    var after = all(student).keySet();
    assertTrue(after.contains(open.session().toString()));
    assertFalse(after.contains(onList.session().toString()));
  }

  /** Recorre todas las páginas: la base es compartida y otras clases dejan sesiones activas. */
  private Map<String, tools.jackson.databind.JsonNode> all(String token) throws Exception {
    var byId = new HashMap<String, tools.jackson.databind.JsonNode>();
    for (int page = 0; ; page++) {
      var items =
          ok(send("GET", "/api/v1/me/active-sessions?size=100&page=" + page, token, null), 200)
              .get("items");
      items.forEach(i -> byId.put(i.get("sesion_id").asText(), i));
      if (items.size() < 100) return byId;
    }
  }
}
