package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.compartido.aplicacion.OpaqueSecrets;
import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

class DeviceUniquenessTest extends PostgresFixture {
  @org.springframework.beans.factory.annotation.Autowired OpaqueSecrets secrets;

  @Test
  void authenticatedDeviceIsUniquePerSessionAndNeverStoredInClear() throws Exception {
    var first = scenario("EVENTO", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    start(first[1], owner);
    UUID device = UUID.randomUUID();
    account(first[1], token(ScenarioFixtures.STUDENT), qr(first[1], owner), device)
        .andExpect(status().isCreated());
    account(first[1], token(ScenarioFixtures.DIRECTOR), qr(first[1], owner), device)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("code").value("DISPOSITIVO_YA_REGISTRADO"));

    var second = scenario("EVENTO", 200);
    start(second[1], owner);
    account(second[1], token(ScenarioFixtures.DIRECTOR), qr(second[1], owner), device)
        .andExpect(status().isCreated());

    String clear = device.toString();
    String protectedValue = secrets.hmac("device:" + clear);
    assertNotEquals(clear, protectedValue);
    assertEquals(0, db.queryForObject(
        "SELECT count(*) FROM intentos_asistencia WHERE dispositivo_hash=?", Integer.class, clear));
    assertTrue(db.queryForObject(
        "SELECT count(*) FROM intentos_asistencia WHERE dispositivo_hash=?", Integer.class,
        protectedValue) >= 2);
  }

  @Test
  void closedGuestUsesSameRuleAndSamePersonKeepsDuplicateBehavior() throws Exception {
    String a = fictionalId(), b = fictionalId();
    var event = guests(120, a, b);
    UUID device = UUID.randomUUID();
    String first = open(event).get("registro_invitado_token").asText();
    UUID firstKey = UUID.randomUUID();
    guestSubmit(first, a, firstKey, device).andExpect(status().isCreated());
    guestSubmit(first, a, firstKey, device).andExpect(status().isOk());
    String second = open(event).get("registro_invitado_token").asText();
    guestSubmit(second, b, UUID.randomUUID(), device)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("code").value("DISPOSITIVO_YA_REGISTRADO"));
  }

  @Test
  void missingOrInvalidDeviceHasDedicatedProblem() throws Exception {
    var ids = scenario("EVENTO", 200);
    String owner = token(ScenarioFixtures.TEACHER), student = token(ScenarioFixtures.STUDENT);
    start(ids[1], owner);
    String qr = qr(ids[1], owner);
    for (String body : List.of(
        json.writeValueAsString(Map.of("qr_token", qr)),
        json.writeValueAsString(Map.of("qr_token", qr, "dispositivo_id", "no-es-uuid")))) {
      api.perform(
              post("/api/v1/attendance/attempts")
                  .header("Authorization", "Bearer " + student)
                  .header("Idempotency-Key", UUID.randomUUID())
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("code").value("DISPOSITIVO_REQUERIDO"));
    }
  }

  @Test
  void selfRegistrationUsesTheSameDeviceUniquenessRule() throws Exception {
    var ids=scenario("EVENTO",201);
    db.update("UPDATE actividades SET acceso='ABIERTO' WHERE id=?",ids[0]);
    db.update("INSERT INTO configuracion_invitados(actividad_id,habilitado,tolerancia_invitado_segundos) VALUES (?,true,120)",ids[0]);
    String owner=token("encargado@example.test"); start(ids[1],owner);
    GuestScenario event=new GuestScenario(ids[0],ids[1],owner,qr(ids[1],owner));
    UUID device=UUID.randomUUID();
    guestSubmit(open(event).get("registro_invitado_token").asText(),fictionalId(),"Primera",UUID.randomUUID(),device)
        .andExpect(status().isCreated());
    guestSubmit(open(event).get("registro_invitado_token").asText(),fictionalId(),"Segunda",UUID.randomUUID(),device)
        .andExpect(status().isConflict()).andExpect(jsonPath("code").value("DISPOSITIVO_YA_REGISTRADO"));
  }

  private org.springframework.test.web.servlet.ResultActions account(
      UUID session, String bearer, String qr, UUID device) throws Exception {
    return api.perform(
        post("/api/v1/attendance/attempts")
            .header("Authorization", "Bearer " + bearer)
            .header("Idempotency-Key", UUID.randomUUID())
            .contentType("application/json")
            .content(
                json.writeValueAsString(
                    Map.of("qr_token", qr, "dispositivo_id", device))));
  }
}
