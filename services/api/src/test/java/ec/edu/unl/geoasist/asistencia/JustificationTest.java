package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.CorrectionSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JustificationTest extends CorrectionSupport {
  private static final String SECRET_MOTIVE = "Certificado medico ficticio 12345";

  private UUID closedClass(String owner) throws Exception {
    var ids = scenario("CLASE", 200);
    authorize(ids[0], true, S1, S2, S3);
    start(ids[1], owner);
    attend(ids[1], S3);
    close(ids[1], owner);
    activity = ids[0];
    return ids[1];
  }

  private UUID activity;

  private String state(UUID session, UUID person) {
    return db.queryForObject(
        "SELECT estado FROM ausencias WHERE sesion_id=? AND persona_id=?",
        String.class,
        session,
        person);
  }

  @Test
  void responsibleJustifiesAnAbsenceWithAuditedReason() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    UUID session = closedClass(owner);
    String path = "/sessions/" + session + "/absences/" + S1 + "/justify";

    assertEquals(400, status(send(path, owner, Map.of("razon", " "))));
    assertEquals(403, status(send(path, token("otro-docente@example.test"), Map.of("razon", "x"))));
    assertEquals(403, status(send(path, token(ScenarioFixtures.STUDENT), Map.of("razon", "x"))));
    var ok = send(path, owner, Map.of("razon", "Razón ficticia del docente"));
    assertEquals(201, status(ok));
    assertEquals("JUSTIFICADA", state(session, S1));
    assertEquals(
        1,
        count(
            "SELECT count(*) FROM justificaciones WHERE sesion_id=? AND persona_id=? AND origen='RESPONSABLE' AND estado='APROBADA'",
            session,
            S1));
    assertEquals(
        1,
        count(
            "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=? AND accion='JUSTIFICAR_AUSENCIA' AND razon='Razón ficticia del docente'",
            activity));
    // Ya justificada, o sin ausencia (S3 asistió): se rechaza.
    assertEquals(409, status(send(path, owner, Map.of("razon", "otra"))));
    var none =
        send("/sessions/" + session + "/absences/" + S3 + "/justify", owner, Map.of("razon", "x"));
    assertEquals("SIN_AUSENCIA", code(none));
  }

  @Test
  void studentRequestsAndResponsibleDecidesImmutably() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    UUID session = closedClass(owner);
    String student = token(ScenarioFixtures.STUDENT);
    String path = "/me/absences/" + session + "/justification-requests";

    assertEquals(400, status(send(path, student, Map.of("motivo", ""))));
    var created = send(path, student, Map.of("motivo", SECRET_MOTIVE));
    assertEquals(201, status(created));
    UUID request = UUID.fromString(json.readTree(text(created)).get("id").asText());
    // Una pendiente por ausencia; sin ausencia (S3 asistió) no hay solicitud.
    assertEquals(
        "JUSTIFICACION_PENDIENTE_EXISTE", code(send(path, student, Map.of("motivo", "otra"))));
    assertEquals(
        "JUSTIFICACION_PENDIENTE_EXISTE",
        code(
            send(
                "/sessions/" + session + "/absences/" + S1 + "/justify",
                owner,
                Map.of("razon", "x"))));
    assertEquals(
        "SIN_AUSENCIA",
        code(send(path, token("otro-estudiante@example.test"), Map.of("motivo", "x"))));

    // Bandeja visible al responsable; el estudiante no decide ni ve la bandeja.
    var inbox =
        json.readTree(
            fetch("/activities/" + activity + "/justification-requests?estado=PENDIENTE", owner));
    assertEquals(1, inbox.get("total").asInt());
    assertEquals(SECRET_MOTIVE, inbox.get("items").get(0).get("motivo").asText());
    String decide = "/justification-requests/" + request + "/decision";
    assertEquals(403, status(send(decide, student, Map.of("decision", "APROBAR", "razon", "x"))));
    assertEquals(400, status(send(decide, owner, Map.of("decision", "TAL", "razon", "x"))));

    assertEquals(
        200, status(send(decide, owner, Map.of("decision", "RECHAZAR", "razon", "Sin respaldo"))));
    assertEquals("AUSENTE", state(session, S1), "rechazar no justifica");
    // US2-6: inmutable.
    assertEquals(
        409, status(send(decide, owner, Map.of("decision", "APROBAR", "razon", "Cambio"))));
    assertEquals(
        "RECHAZADA",
        db.queryForObject("SELECT estado FROM justificaciones WHERE id=?", String.class, request));
    // Tras decidir se puede pedir otra y aprobarla.
    var again = send(path, student, Map.of("motivo", "Con documento"));
    assertEquals(201, status(again));
    UUID second = UUID.fromString(json.readTree(text(again)).get("id").asText());
    assertEquals(
        200,
        status(
            send(
                "/justification-requests/" + second + "/decision",
                owner,
                Map.of("decision", "APROBAR", "razon", "Documento válido"))));
    assertEquals("JUSTIFICADA", state(session, S1));
    // SC-004: auditoría con razón y sin el motivo; CSV sin el motivo.
    assertEquals(
        2,
        count(
            "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=? AND accion='DECIDIR_JUSTIFICACION' AND razon IS NOT NULL",
            activity));
    assertEquals(
        0,
        count(
            "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=? AND (nuevo::text LIKE ? OR anterior::text LIKE ? OR razon LIKE ?)",
            activity,
            "%Certificado%",
            "%Certificado%",
            "%Certificado%"));
    assertFalse(
        fetch("/sessions/" + session + "/attendances/export", owner).contains("Certificado"));
  }

  @Test
  void requestsAndJustificationsExpireWithTheCorrectionWindow() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    UUID session = closedClass(owner);
    clock.set(clock.instant().plus(Duration.ofDays(8)));
    var late =
        send(
            "/me/absences/" + session + "/justification-requests",
            token(ScenarioFixtures.STUDENT),
            Map.of("motivo", "tarde"));
    assertEquals(409, status(late));
    assertEquals("PLAZO_CORRECCION_VENCIDO", code(late));
    var lateOwner =
        send(
            "/sessions/" + session + "/absences/" + S1 + "/justify",
            token(ScenarioFixtures.TEACHER),
            Map.of("razon", "tarde"));
    assertEquals("PLAZO_CORRECCION_VENCIDO", code(lateOwner));
    assertEquals("AUSENTE", state(session, S1));
  }
}
