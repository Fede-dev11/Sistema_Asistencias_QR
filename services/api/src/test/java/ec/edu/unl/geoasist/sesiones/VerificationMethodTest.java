package ec.edu.unl.geoasist.sesiones;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ClassSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

public class VerificationMethodTest extends ClassSupport {
  private tools.jackson.databind.JsonNode create(String teacher, String method) throws Exception {
    var body = new LinkedHashMap<String, Object>();
    body.put("modalidad", "CLASE");
    body.put("nombre", "Clase con método");
    body.put("espacio_id", SPACE);
    body.put("paralelo_ids", List.of(P11));
    body.put("horario", schedule(List.of(1, 2, 3, 4, 5, 6, 7), "2026-10-20", "2026-10-21"));
    if (method != null) body.put("metodo_verificacion", method);
    return ok(send("POST", "/api/v1/activities", teacher, body), 201);
  }

  private String method(String session) {
    return db.queryForObject(
        "SELECT metodo_verificacion FROM sesiones WHERE id=?::uuid", String.class, session);
  }

  /** Sin PostGIS en este fixture, la configuración se siembra directamente. */
  public static void enableGps(org.springframework.jdbc.core.JdbcTemplate db, String session) {
    db.update(
        "INSERT INTO configuracion_geografica(sesion_id,habilitada,radio_m,precision_max_m,antiguedad_max_s,modo,configurada_por,configurada_en) VALUES (?::uuid,true,20,30,60,'REVIEW_ONLY',(SELECT id FROM cuentas LIMIT 1),now()) ON CONFLICT(sesion_id) DO UPDATE SET habilitada=true,radio_m=20,precision_max_m=30,antiguedad_max_s=60,modo='REVIEW_ONLY'",
        session);
  }

  @Test
  void sessionsInheritActivityMethodAndAreEditableWhileScheduled() throws Exception {
    clock.set(IN_CLASS);
    String teacher = token(ScenarioFixtures.TEACHER);
    String plain = create(teacher, null).get("sesiones").get(0).get("id").asText();
    assertEquals("SOLO_QR", method(plain));
    String session = create(teacher, "SOLO_GPS").get("sesiones").get(0).get("id").asText();
    assertEquals("SOLO_GPS", method(session));

    var edited =
        ok(
            send(
                "PATCH",
                "/api/v1/sessions/" + session,
                teacher,
                Map.of("metodo_verificacion", "NINGUNO")),
            200);
    assertEquals("NINGUNO", edited.get("metodo_verificacion").asText());
    ok(
        send("PATCH", "/api/v1/sessions/" + session, teacher, Map.of("metodo_verificacion", "X")),
        400);

    String bare = UUID.randomUUID().toString();
    db.update("INSERT INTO espacios(id,nombre,tipo) VALUES (?::uuid,'Sin ubicacion','AULA')", bare);
    assertEquals(
        "ESPACIO_SIN_UBICACION",
        ok(
                send(
                    "PATCH",
                    "/api/v1/sessions/" + session,
                    teacher,
                    Map.of("metodo_verificacion", "QR_Y_GPS", "espacio_id", bare)),
                409)
            .get("code")
            .asText());
  }

  @Test
  void leavingGpsDisablesConfigurationAndMethodFreezesOnStart() throws Exception {
    clock.set(IN_CLASS);
    String teacher = token(ScenarioFixtures.TEACHER);
    String session = create(teacher, "SOLO_GPS").get("sesiones").get(0).get("id").asText();

    assertEquals(
        "CONFIGURACION_GPS_REQUERIDA",
        ok(send("POST", "/api/v1/sessions/" + session + "/start", teacher, null), 409)
            .get("code")
            .asText());
    enableGps(db, session);
    ok(
        send(
            "PATCH",
            "/api/v1/sessions/" + session,
            teacher,
            Map.of("metodo_verificacion", "SOLO_QR")),
        200);
    assertFalse(
        db.queryForObject(
            "SELECT habilitada FROM configuracion_geografica WHERE sesion_id=?::uuid",
            Boolean.class,
            session));
    ok(
        send(
            "PATCH",
            "/api/v1/sessions/" + session,
            teacher,
            Map.of("metodo_verificacion", "SOLO_GPS")),
        200);
    enableGps(db, session);
    ok(send("POST", "/api/v1/sessions/" + session + "/start", teacher, null), 200);
    assertEquals(
        "SESION_NO_EDITABLE",
        ok(
                send(
                    "PATCH",
                    "/api/v1/sessions/" + session,
                    teacher,
                    Map.of("metodo_verificacion", "NINGUNO")),
                409)
            .get("code")
            .asText());
    ok(
        send(
            "PUT",
            "/api/v1/sessions/" + session + "/geolocation",
            teacher,
            Map.of("habilitada", false)),
        409);
    ok(send("GET", "/api/v1/sessions/" + session + "/qr", teacher, null), 409);
  }
}
