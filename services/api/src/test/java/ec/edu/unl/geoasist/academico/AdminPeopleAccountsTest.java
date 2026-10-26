package ec.edu.unl.geoasist.academico;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class AdminPeopleAccountsTest extends AdminSupport {
  @Test
  void createsPeopleRejectsDuplicatesAndInvalidIds() throws Exception {
    String a = admin();
    String cedula = fictionalId();
    var p = createPerson(a, cedula, "  Ana Ficticia ");
    assertEquals("Ana Ficticia", p.get("nombres").asText());
    send("POST", "/api/v1/admin/people", a, Map.of("cedula", cedula, "nombres", "Otra"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("code").value("PERSONA_EXISTE"));
    send("POST", "/api/v1/admin/people", a, Map.of("cedula", "0100000018", "nombres", "X"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("code").value("CEDULA_INVALIDA"));
    var renamed =
        ok(
            send(
                "PATCH",
                "/api/v1/admin/people/" + p.get("id").asText(),
                a,
                Map.of("nombres", "Ana B")),
            200);
    assertEquals("Ana B", renamed.get("nombres").asText());
    var found = ok(send("GET", "/api/v1/admin/people?q=" + cedula, a, null), 200);
    assertEquals(1, found.get("total").asInt());
    assertEquals(
        2L,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_academica WHERE accion IN ('CREAR_PERSONA','EDITAR_PERSONA') AND entidad_id=?",
            Long.class,
            UUID.fromString(p.get("id").asText())));
  }

  @Test
  void idempotentCreateReplaysAndDifferentPayloadConflicts() throws Exception {
    String a = admin();
    UUID key = UUID.randomUUID();
    var body = json.writeValueAsString(Map.of("cedula", fictionalId(), "nombres", "Idem"));
    var first =
        api.perform(
                post("/api/v1/admin/people")
                    .header("Authorization", "Bearer " + a)
                    .header("Idempotency-Key", key.toString())
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var again =
        api.perform(
                post("/api/v1/admin/people")
                    .header("Authorization", "Bearer " + a)
                    .header("Idempotency-Key", key.toString())
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(json.readTree(first).get("id"), json.readTree(again).get("id"));
    api.perform(
            post("/api/v1/admin/people")
                .header("Authorization", "Bearer " + a)
                .header("Idempotency-Key", key.toString())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(Map.of("cedula", fictionalId(), "nombres", "Idem"))))
        .andExpect(status().isConflict());
  }

  @Test
  void deactivatedAccountCannotLoginButKeepsData() throws Exception {
    String a = admin();
    var p = createPerson(a, fictionalId(), "Cuenta Ficticia");
    String mail = "cuenta-" + UUID.randomUUID() + "@example.test";
    var withAccount = createAccount(a, p.get("id").asText(), mail, List.of("ESTUDIANTE"));
    UUID account = UUID.fromString(withAccount.get("cuenta").get("id").asText());
    send(
            "POST",
            "/api/v1/admin/accounts",
            a,
            Map.of(
                "persona_id",
                p.get("id").asText(),
                "correo",
                "otro-" + mail,
                "password",
                "fixture-password",
                "roles",
                List.of("ESTUDIANTE")))
        .andExpect(status().isConflict());
    String session = token(mail);
    long attendances = db.queryForObject("SELECT count(*) FROM asistencias", Long.class);
    ok(send("PATCH", "/api/v1/admin/accounts/" + account, a, Map.of("activa", false)), 200);
    api.perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("correo", mail, "password", "fixture-password"))))
        .andExpect(status().isUnauthorized());
    api.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + session))
        .andExpect(status().isUnauthorized());
    assertEquals(attendances, db.queryForObject("SELECT count(*) FROM asistencias", Long.class));
    assertEquals(
        1L, db.queryForObject("SELECT count(*) FROM cuentas WHERE id=?", Long.class, account));
    ok(
        send(
            "PATCH",
            "/api/v1/admin/accounts/" + account,
            a,
            Map.of("roles", List.of("DOCENTE"), "activa", true)),
        200);
    assertEquals(
        List.of("DOCENTE"),
        db.queryForList("SELECT rol FROM roles_cuenta WHERE cuenta_id=?", String.class, account));
    send("PATCH", "/api/v1/admin/accounts/" + account, a, Map.of("roles", List.of("INVENTADO")))
        .andExpect(status().isBadRequest());
    assertEquals(
        3L,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_academica WHERE entidad_id=?", Long.class, account));
  }

  @Test
  void adminCannotLockThemselvesOut() throws Exception {
    send(
            "PATCH",
            "/api/v1/admin/accounts/00000000-0000-0000-0000-000000000208",
            admin(),
            Map.of("activa", false))
        .andExpect(status().isConflict());
  }
}
