package ec.edu.unl.geoasist.academico;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AdminContractTest extends AdminSupport {
  static final String ZERO = "00000000-0000-0000-0000-";
  static final List<String[]> ROUTES =
      List.of(
          new String[] {"GET", "/api/v1/admin/people"},
          new String[] {"POST", "/api/v1/admin/people"},
          new String[] {"GET", "/api/v1/admin/people/" + ZERO + "000000000100"},
          new String[] {"PATCH", "/api/v1/admin/people/" + ZERO + "000000000100"},
          new String[] {"POST", "/api/v1/admin/accounts"},
          new String[] {"PATCH", "/api/v1/admin/accounts/" + ZERO + "000000000200"},
          new String[] {"GET", "/api/v1/admin/careers"},
          new String[] {"POST", "/api/v1/admin/careers"},
          new String[] {"PATCH", "/api/v1/admin/careers/" + ZERO + "000000000001"},
          new String[] {"GET", "/api/v1/admin/careers/" + ZERO + "000000000001/parallels"},
          new String[] {"POST", "/api/v1/admin/careers/" + ZERO + "000000000001/parallels"},
          new String[] {"PUT", "/api/v1/admin/parallels/" + ZERO + "000000000011/enrollments"},
          new String[] {"PUT", "/api/v1/admin/parallels/" + ZERO + "000000000011/teachers"});

  @Test
  void withoutSessionIs401AndNonAdminRolesAre403() throws Exception {
    for (String[] r : ROUTES) send(r[0], r[1], null, Map.of()).andExpect(status().isUnauthorized());
    for (String who : List.of("docente", "estudiante", "director", "director-carrera", "encargado")) {
      String t = token(who + "@example.test");
      for (String[] r : ROUTES) send(r[0], r[1], t, Map.of()).andExpect(status().isForbidden());
      api.perform(
              multipart("/api/v1/admin/imports/students")
                  .file(new MockMultipartFile("file", "a.csv", "text/csv", "x".getBytes()))
                  .header("Authorization", "Bearer " + t))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void adminResponsesFollowTheContractShape() throws Exception {
    String a = admin();
    var page = ok(send("GET", "/api/v1/admin/people?size=5", a, null), 200);
    assertTrue(
        page.has("items") && page.has("page") && page.has("size") && page.has("total"));
    assertTrue(page.get("items").get(0).has("cedula"));
    send("GET", "/api/v1/admin/people?size=101", a, null).andExpect(status().isBadRequest());
    var careers = ok(send("GET", "/api/v1/admin/careers", a, null), 200);
    assertTrue(careers.get("items").get(0).has("codigo"));
    var report = importCsv(a, "students", "cedula,nombres,correo\n", 200);
    assertTrue(
        report.has("procesadas")
            && report.has("creadas")
            && report.has("actualizadas")
            && report.has("sin_cambios")
            && report.get("rechazadas").isArray());
  }
}
