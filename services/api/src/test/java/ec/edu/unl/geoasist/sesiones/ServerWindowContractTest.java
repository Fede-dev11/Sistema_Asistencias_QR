package ec.edu.unl.geoasist.sesiones;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ServerWindowContractTest extends PostgresFixture {
  @Test
  void guestFormDoesNotExtendTheSessionWindow() throws Exception {
    String cedula = fictionalId();
    var event = guests(120, cedula);
    Instant end = clock.instant().plusSeconds(5);
    db.update("UPDATE sesiones SET ventana_fin=? WHERE id=?", Timestamp.from(end), event.session());
    String secret = open(event).get("registro_invitado_token").asText();
    clock.set(end);
    guestSubmit(secret, cedula, UUID.randomUUID())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("code").value("FUERA_DE_HORARIO"))
        .andExpect(jsonPath("horario_fin").value(end.toString()))
        .andExpect(jsonPath("accion_recomendada").isNotEmpty());
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE sesion_id=?", Integer.class, event.session()));
  }

  @Test
  void startUsesServerTimeAndAllowsOnlyTheHalfOpenWindow() throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    Instant begin = clock.instant().plusSeconds(10), end = begin.plusSeconds(60);
    db.update(
        "UPDATE sesiones SET ventana_inicio=?,ventana_fin=? WHERE id=?",
        Timestamp.from(begin),
        Timestamp.from(end),
        ids[1]);

    for (Instant rejected : List.of(begin.minusNanos(1000), end, end.plusSeconds(1))) {
      clock.set(rejected);
      api.perform(
              post("/api/v1/sessions/" + ids[1] + "/start")
                  .header("Authorization", "Bearer " + owner)
                  .header("Idempotency-Key", UUID.randomUUID().toString())
                  .header("Date", begin.toString())
                  .contentType("application/json")
                  .content("{\"hora_cliente\":\"" + begin + "\"}"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("code").value("FUERA_DE_HORARIO"))
          .andExpect(jsonPath("horario_inicio").value(begin.toString()))
          .andExpect(jsonPath("horario_fin").value(end.toString()))
          .andExpect(jsonPath("hora_servidor").value(rejected.toString()))
          .andExpect(jsonPath("accion_recomendada").isNotEmpty());
      assertEquals(
          "PROGRAMADA",
          db.queryForObject("SELECT estado FROM sesiones WHERE id=?", String.class, ids[1]));
    }
    clock.set(begin);
    start(ids[1], owner);
  }

  @Test
  void qrAndAttendanceRejectAtTheEndEvenWhenTheSchedulerHasNotClosedTheSession() throws Exception {
    var ids = scenario("CLASE", 200);
    db.update(
        "INSERT INTO estudiantes_autorizados VALUES (?,?,true)", ids[0], ScenarioFixtures.id(102));
    String owner = token(ScenarioFixtures.TEACHER), student = token(ScenarioFixtures.STUDENT);
    Instant end = clock.instant().plusSeconds(10);
    db.update("UPDATE sesiones SET ventana_fin=? WHERE id=?", Timestamp.from(end), ids[1]);
    start(ids[1], owner);
    var qr =
        json.readTree(
            api.perform(
                    get("/api/v1/sessions/" + ids[1] + "/qr")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("hora_servidor").value(clock.instant().toString()))
                .andExpect(jsonPath("vence_en").value(end.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString());

    clock.set(end);
    api.perform(
            get("/api/v1/sessions/" + ids[1] + "/qr").header("Authorization", "Bearer " + owner))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("code").value("FUERA_DE_HORARIO"));
    api.perform(
            post("/api/v1/attendance/attempts")
                .header("Authorization", "Bearer " + student)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "qr_token",
                            qr.get("qr_token").asText(),
                            "dispositivo_id",
                            UUID.randomUUID()))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("code").value("FUERA_DE_HORARIO"))
        .andExpect(jsonPath("horario_fin").value(end.toString()))
        .andExpect(jsonPath("accion_recomendada").isNotEmpty());
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE sesion_id=?", Integer.class, ids[1]));
  }
}
