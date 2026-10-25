package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.CorrectionSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManualAttendanceTest extends CorrectionSupport {
  private String path(UUID session) {
    return "/sessions/" + session + "/manual-attendances";
  }

  @Test
  void manualAttendanceRemovesTheAbsenceIsAuditedAndIsIdempotent() throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    authorize(ids[0], true, S1, S2, S3);
    start(ids[1], owner);
    close(ids[1], owner);
    assertEquals(3, absences(ids[1]));

    UUID key = UUID.randomUUID();
    var body = Map.of("persona_id", S1.toString(), "razon", "Sin teléfono ficticio");
    var first = send(path(ids[1]), owner, key, body);
    assertEquals(201, status(first));
    assertEquals("MANUAL", json.readTree(text(first)).get("forma").asText());
    assertEquals(2, absences(ids[1]), "US1-1: la ausencia se elimina");
    assertEquals(
        "MANUAL",
        db.queryForObject(
            "SELECT forma FROM asistencias WHERE sesion_id=? AND persona_id=?",
            String.class, ids[1], S1));
    assertEquals(
        "MANUAL",
        db.queryForObject(
            "SELECT canal FROM intentos_asistencia WHERE sesion_id=? AND persona_id=?",
            String.class, ids[1], S1));
    assertEquals(
        1,
        count(
            "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=? AND accion='ASISTENCIA_MANUAL' AND razon='Sin teléfono ficticio'",
            ids[0]));

    // Idempotencia: misma clave y carga devuelve lo guardado; otra clave es duplicado (US2-2).
    var replay = send(path(ids[1]), owner, key, body);
    assertEquals(200, status(replay));
    assertEquals(json.readTree(text(first)), json.readTree(text(replay)));
    var duplicate = send(path(ids[1]), owner, body);
    assertEquals(409, status(duplicate));
    assertEquals("ASISTENCIA_DUPLICADA", code(duplicate));
    assertEquals(1, count("SELECT count(*) FROM asistencias WHERE sesion_id=?", ids[1]));

    // US2-8: listado y CSV muestran forma y razón.
    var page = json.readTree(fetch("/sessions/" + ids[1] + "/attendances", owner));
    assertEquals("MANUAL", page.get("items").get(0).get("forma").asText());
    assertEquals("Sin teléfono ficticio", page.get("items").get(0).get("razon_manual").asText());
    String csv = fetch("/sessions/" + ids[1] + "/attendances/export", owner);
    assertTrue(csv.contains(",MANUAL,Sin teléfono ficticio"), csv);
  }

  @Test
  void validatesActorAuthorizationAndInput() throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    authorize(ids[0], true, S1);
    authorize(ids[0], false, S2);
    start(ids[1], owner);
    // Sesión ACTIVA: permitido. US2-7: otros actores rechazados.
    assertEquals(403, status(send(path(ids[1]), token("otro-docente@example.test"),
        Map.of("persona_id", S1.toString(), "razon", "x"))));
    assertEquals(403, status(send(path(ids[1]), token(ScenarioFixtures.STUDENT),
        Map.of("persona_id", S1.toString(), "razon", "x"))));
    assertEquals(403, status(send(path(ids[1]), token(ScenarioFixtures.DIRECTOR),
        Map.of("persona_id", S1.toString(), "razon", "x"))));
    assertEquals(400, status(send(path(ids[1]), owner, Map.of("persona_id", S1.toString(), "razon", "  "))));
    var notListed = send(path(ids[1]), owner, Map.of("persona_id", S2.toString(), "razon", "x"));
    assertEquals(422, status(notListed));
    assertEquals("NO_AUTORIZADO_EN_SESION", code(notListed));
    assertEquals(0, count("SELECT count(*) FROM asistencias WHERE sesion_id=?", ids[1]));
    assertEquals(201, status(send(path(ids[1]), owner, Map.of("persona_id", S1.toString(), "razon", "Activa"))));
  }

  @Test
  void correctionWindowIsSevenDaysAfterClose() throws Exception {
    var ids = scenario("CLASE", 200);
    authorize(ids[0], true, S1, S2);
    String owner = token(ScenarioFixtures.TEACHER);
    start(ids[1], owner);
    close(ids[1], owner);
    var closedAt = clock.instant();

    clock.set(closedAt.plus(Duration.ofDays(6)));
    assertEquals(201, status(send(path(ids[1]), token(ScenarioFixtures.TEACHER),
        Map.of("persona_id", S1.toString(), "razon", "Dentro del plazo"))));

    clock.set(closedAt.plus(Duration.ofDays(8)));
    var late = send(path(ids[1]), token(ScenarioFixtures.TEACHER),
        Map.of("persona_id", S2.toString(), "razon", "Fuera del plazo"));
    assertEquals(409, status(late));
    assertEquals("PLAZO_CORRECCION_VENCIDO", code(late));
    assertEquals(1, absences(ids[1]), "S2 sigue ausente");
  }

  @Test
  void pendingReviewIsResolvedAsRejectedByManualRegistration() throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    authorize(ids[0], true, S2);
    start(ids[1], owner);
    UUID attempt = pendingAttempt(ids[1], S2);
    assertEquals(201, status(send(path(ids[1]), owner, Map.of("persona_id", S2.toString(), "razon", "Manual"))));
    assertEquals(
        "RECHAZAR",
        db.queryForObject("SELECT decision FROM revisiones_asistencia WHERE intento_id=?", String.class, attempt));
    assertEquals(
        "Registro manual",
        db.queryForObject("SELECT razon FROM revisiones_asistencia WHERE intento_id=?", String.class, attempt));
    // Un pendiente ya resuelto no puede aprobarse después.
    assertEquals(
        409,
        status(send("/attempts/" + attempt + "/review", owner,
            Map.of("decision", "APROBAR", "razon", "Tarde"))));
  }

  @Test
  void approvingAReviewAfterCloseRemovesTheAbsenceOnlyInsideTheWindow() throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    authorize(ids[0], true, S1, S3);
    start(ids[1], owner);
    UUID inside = pendingAttempt(ids[1], S1);
    UUID outside = pendingAttempt(ids[1], S3);
    close(ids[1], owner);
    assertEquals(2, absences(ids[1]), "US1-2: el pendiente figura ausente");
    var closedAt = clock.instant();

    clock.set(closedAt.plus(Duration.ofDays(2)));
    String later = token(ScenarioFixtures.TEACHER);
    var approved = send("/attempts/" + inside + "/review", later, Map.of("decision", "APROBAR", "razon", "Verificado"));
    assertEquals(200, status(approved));
    assertEquals(1, absences(ids[1]), "la ausencia de S1 se elimina");
    assertEquals(
        "REVISION",
        db.queryForObject("SELECT forma FROM asistencias WHERE sesion_id=? AND persona_id=?", String.class, ids[1], S1));

    clock.set(closedAt.plus(Duration.ofDays(8)));
    var late = send("/attempts/" + outside + "/review", token(ScenarioFixtures.TEACHER),
        Map.of("decision", "APROBAR", "razon", "Muy tarde"));
    assertEquals(409, status(late));
    assertEquals("PLAZO_CORRECCION_VENCIDO", code(late));
    assertEquals(1, absences(ids[1]));
  }
}
