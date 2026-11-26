package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ClassSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

class RequiredLocationContractTest extends ClassSupport {
  @Test
  void missingInvalidAndExpiredLocationsNeverConfirmAttendance() throws Exception {
    clock.set(IN_CLASS);
    String teacher = token(ScenarioFixtures.TEACHER), student = token(ScenarioFixtures.STUDENT);
    var activity =
        createClass(teacher, List.of(P11), schedule(List.of(2), "2026-10-20", "2026-10-20"), 201);
    String session = activity.get("sesiones").get(0).get("id").asText();
    db.update(
        "UPDATE sesiones SET metodo_verificacion='SOLO_GPS' WHERE id=?", UUID.fromString(session));
    ec.edu.unl.geoasist.sesiones.VerificationMethodTest.enableGps(db, session);
    ok(send("POST", "/api/v1/sessions/" + session + "/start", teacher, null), 200);
    var payload = new LinkedHashMap<String, Object>();
    payload.put("sesion_id", session);
    payload.put("dispositivo_id", UUID.randomUUID());
    for (int retry = 0; retry < 3; retry++) {
      var missing = ok(send("POST", "/api/v1/attendance/attempts", student, payload), 202);
      assertEquals("REPETIR", missing.get("resultado").asText());
      assertEquals("UBICACION_REQUERIDA", missing.get("code").asText());
      assertFalse(missing.get("accion_recomendada").asText().isBlank());
    }
    payload.put(
        "ubicacion",
        Map.of("latitud", 91, "longitud", -79, "precision_m", 5, "medido_en", clock.instant()));
    assertEquals(
        "UBICACION_INVALIDA",
        ok(send("POST", "/api/v1/attendance/attempts", student, payload), 400)
            .get("code")
            .asText());
    for (var measured : List.of(clock.instant().minusSeconds(61), clock.instant().plusSeconds(6))) {
      payload.put(
          "ubicacion",
          Map.of("latitud", -4, "longitud", -79, "precision_m", 5, "medido_en", measured));
      var stale = ok(send("POST", "/api/v1/attendance/attempts", student, payload), 202);
      assertEquals("UBICACION_ANTIGUA", stale.get("code").asText());
      assertEquals("REPETIR", stale.get("resultado").asText());
    }
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE sesion_id=?",
            Integer.class,
            UUID.fromString(session)));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM intentos_asistencia WHERE sesion_id=? AND resultado='PENDIENTE_REVISION'",
            Integer.class,
            UUID.fromString(session)));
  }
}
