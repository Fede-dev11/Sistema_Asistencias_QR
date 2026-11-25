package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ClassSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

class MethodAttendanceTest extends ClassSupport {
  /** Crea una clase con el método dado, inicia su primera sesión y devuelve su id. */
  private String started(String teacher, String method) throws Exception {
    clock.set(IN_CLASS);
    var body = new LinkedHashMap<String, Object>();
    body.put("modalidad", "CLASE");
    body.put("nombre", "Clase " + method);
    body.put("espacio_id", SPACE);
    body.put("paralelo_ids", List.of(P11));
    body.put("metodo_verificacion", method);
    body.put("horario", schedule(List.of(1, 2, 3, 4, 5, 6, 7), "2026-10-20", "2026-10-21"));
    String session =
        ok(send("POST", "/api/v1/activities", teacher, body), 201)
            .get("sesiones")
            .get(0)
            .get("id")
            .asText();
    if (method.contains("GPS"))
      ec.edu.unl.geoasist.sesiones.VerificationMethodTest.enableGps(db, session);
    ok(send("POST", "/api/v1/sessions/" + session + "/start", teacher, null), 200);
    return session;
  }

  private Map<String, Object> attempt(String key, String value) {
    var body = new LinkedHashMap<String, Object>();
    body.put(key, value);
    body.put("dispositivo_id", UUID.randomUUID().toString());
    return body;
  }

  @Test
  void noneAcceptsOnlyWhileActiveAndWithoutQr() throws Exception {
    clock.set(IN_CLASS);
    String teacher = token(ScenarioFixtures.TEACHER);
    String student = token(ScenarioFixtures.STUDENT);
    String session = started(teacher, "NINGUNO");
    ok(send("POST", "/api/v1/attendance/attempts", student, attempt("sesion_id", session)), 201);

    var withToken = attempt("qr_token", "x".repeat(40));
    withToken.put("sesion_id", session);
    assertEquals(
        "METODO_NO_ADMITE_QR",
        ok(send("POST", "/api/v1/attendance/attempts", student, withToken), 400)
            .get("code")
            .asText());
    ok(
        send(
            "POST",
            "/api/v1/attendance/attempts",
            student,
            Map.of("dispositivo_id", UUID.randomUUID().toString())),
        400);

    ok(send("POST", "/api/v1/sessions/" + session + "/close", teacher, null), 200);
    assertEquals(
        "SESION_INACTIVA",
        ok(send("POST", "/api/v1/attendance/attempts", student, attempt("sesion_id", session)), 409)
            .get("code")
            .asText());
  }

  @Test
  void qrMethodsRequireTheToken() throws Exception {
    clock.set(IN_CLASS);
    String teacher = token(ScenarioFixtures.TEACHER);
    String student = token(ScenarioFixtures.STUDENT);
    String session = started(teacher, "SOLO_QR");
    assertEquals(
        "QR_REQUERIDO",
        ok(send("POST", "/api/v1/attendance/attempts", student, attempt("sesion_id", session)), 400)
            .get("code")
            .asText());
    String qr =
        ok(send("GET", "/api/v1/sessions/" + session + "/qr", teacher, null), 200)
            .get("qr_token")
            .asText();
    ok(send("POST", "/api/v1/attendance/attempts", student, attempt("qr_token", qr)), 201);
  }

  @Test
  void gpsOnlyNeedsNoQrAndGoesThroughGeographicReview() throws Exception {
    clock.set(IN_CLASS);
    String teacher = token(ScenarioFixtures.TEACHER);
    String student = token(ScenarioFixtures.STUDENT);
    String session = started(teacher, "SOLO_GPS");
    var body = attempt("sesion_id", session);
    body.put(
        "ubicacion",
        Map.of(
            "latitud",
            -4.0328,
            "longitud",
            -79.2026,
            "precision_m",
            5,
            "medido_en",
            clock.instant().toString()));
    var result = ok(send("POST", "/api/v1/attendance/attempts", student, body), 202);
    assertEquals(
        "REPETIR",
        result.get("resultado").asText()); // sin PostGIS no hay distancia: 001 pide repetir
  }

  @Test
  void guestStartBySessionIdOnlyForMethodsWithoutQr() throws Exception {
    clock.set(IN_CLASS);
    String teacher = token(ScenarioFixtures.TEACHER);
    String session = started(teacher, "NINGUNO");
    // Una clase no admite invitados: la sesión se resuelve (sin QR) y se rechaza por modalidad.
    ok(
        api.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/v1/guest-registrations")
                .contentType("application/json")
                .content("{\"sesion_id\":\"" + session + "\"}")),
        403);
    String qrSession = started(teacher, "SOLO_QR");
    assertEquals(
        "QR_REQUERIDO",
        ok(
                api.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                            "/api/v1/guest-registrations")
                        .contentType("application/json")
                        .content("{\"sesion_id\":\"" + qrSession + "\"}")),
                400)
            .get("code")
            .asText());
  }
}
