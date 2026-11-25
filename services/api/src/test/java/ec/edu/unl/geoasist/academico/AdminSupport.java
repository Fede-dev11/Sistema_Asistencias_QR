package ec.edu.unl.geoasist.academico;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import java.util.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

abstract class AdminSupport extends PostgresFixture {
  static final String ADMIN = "admin@dev.geoasist.local";

  String admin() throws Exception {
    return token(ADMIN);
  }

  ResultActions send(String method, String path, String bearer, Object body) throws Exception {
    var b =
        switch (method) {
          case "POST" -> post(path);
          case "PUT" -> put(path);
          case "PATCH" -> patch(path);
          default -> get(path);
        };
    if (bearer != null) b.header("Authorization", "Bearer " + bearer);
    if (method.equals("POST")) b.header("Idempotency-Key", UUID.randomUUID().toString());
    if (body != null) b.contentType("application/json").content(json.writeValueAsString(body));
    return api.perform(b);
  }

  JsonNode ok(ResultActions r, int status) throws Exception {
    r.andExpect(status().is(status));
    return json.readTree(r.andReturn().getResponse().getContentAsString());
  }

  JsonNode createPerson(String bearer, String cedula, String nombres) throws Exception {
    return ok(
        send("POST", "/api/v1/admin/people", bearer, Map.of("cedula", cedula, "nombres", nombres)),
        201);
  }

  JsonNode createAccount(String bearer, String person, String mail, List<String> roles)
      throws Exception {
    var created =
        ok(
            send(
                "POST",
                "/api/v1/admin/accounts",
                bearer,
                Map.of(
                    "persona_id", person,
                    "correo", mail,
                    "roles", roles)),
            201);
    // Las cuentas creadas por la API son CAS sin contraseña; las pruebas necesitan iniciar sesión
    // local.
    db.update(
        "UPDATE cuentas SET proveedor='DEV',password_hash=(SELECT password_hash FROM cuentas WHERE identificador='docente@example.test') WHERE identificador=?",
        mail);
    return created;
  }

  JsonNode importCsv(String bearer, String kind, String csv, int status) throws Exception {
    return ok(
        api.perform(
            multipart("/api/v1/admin/imports/" + kind)
                .file(
                    new MockMultipartFile(
                        "file",
                        "x.csv",
                        "text/csv",
                        csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + bearer)),
        status);
  }
}
