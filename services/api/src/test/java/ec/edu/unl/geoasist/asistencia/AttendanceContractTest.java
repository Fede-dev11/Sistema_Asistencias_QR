package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AttendanceContractTest extends PostgresFixture {
  @Test
  void authenticatedAttendanceAndIdempotency() throws Exception {
    var ids = scenario("CLASE", 200);
    db.update(
        "INSERT INTO estudiantes_autorizados VALUES (?,?,true)", ids[0], ScenarioFixtures.id(102));
    String owner = token(ScenarioFixtures.TEACHER), student = token(ScenarioFixtures.STUDENT);
    start(ids[1], owner);
    String qr = qr(ids[1], owner), key = UUID.randomUUID().toString();
    UUID deviceId = UUID.randomUUID();
    String body = json.writeValueAsString(Map.of("qr_token", qr, "dispositivo_id", deviceId));
    var request =
        post("/api/v1/attendance/attempts")
            .header("Authorization", "Bearer " + student)
            .header("Idempotency-Key", key)
            .contentType("application/json")
            .content(body);
    var first =
        api.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("resultado").value("CONFIRMADA"))
            .andExpect(jsonPath("mensaje").isString())
            .andExpect(jsonPath("cedula").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(
        json.readTree(first),
        json.readTree(
            api.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()));
    api.perform(
            post("/api/v1/attendance/attempts")
                .header("Authorization", "Bearer " + student)
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "qr_token",
                            qr,
                            "dispositivo_id",
                            deviceId,
                            "ubicacion",
                            Map.of(
                                "latitud",
                                0,
                                "longitud",
                                0,
                                "precision_m",
                                1,
                                "medido_en",
                                clock.instant().toString())))))
        .andExpect(status().isConflict());
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE sesion_id=?", Integer.class, ids[1]));
  }

  @Test
  void guestUsesOnlyPreloadedIdentityAndConsumesOneForm() throws Exception {
    String cedula = fictionalId();
    var event = guests(120, cedula);
    var opened = open(event);
    String secret = opened.get("registro_invitado_token").asText();
    UUID key = UUID.randomUUID();
    var receipt =
        guestSubmit(secret, cedula, key)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("resultado").value("CONFIRMADA"))
            .andExpect(jsonPath("nombres").doesNotExist())
            .andExpect(jsonPath("cedula").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(
        json.readTree(receipt),
        json.readTree(
            guestSubmit(secret, cedula, key)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()));
    assertEquals(
        "Nombre del listado ficticio",
        db.queryForObject(
            "SELECT nombres_registro FROM asistencias WHERE sesion_id=?",
            String.class,
            event.session()));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM cuentas c JOIN personas p ON p.id=c.persona_id WHERE p.cedula_normalizada=?",
            Integer.class,
            cedula));
    api.perform(
            post("/api/v1/guest-registrations/submit")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "registro_invitado_token",
                            secret,
                            "cedula",
                            cedula,
                            "dispositivo_id",
                            UUID.randomUUID(),
                            "nombres",
                            "Nombre del solicitante"))))
        .andExpect(status().isBadRequest());
  }
}
