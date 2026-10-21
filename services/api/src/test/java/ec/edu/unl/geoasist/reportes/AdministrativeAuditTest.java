package ec.edu.unl.geoasist.reportes;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdministrativeAuditTest extends PostgresFixture {
  @Test
  void allAdministrativeCommandsExposeOnlyMinimumAuditValues() throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    api.perform(
            put("/api/v1/sessions/" + ids[1] + "/geolocation")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content("{\"habilitada\":false}"))
        .andExpect(r -> assertEquals(200, r.getResponse().getStatus()));
    api.perform(
            put("/api/v1/activities/" + ids[0] + "/guest-settings")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content("{\"habilitado\":true,\"tolerancia_invitado_segundos\":120}"))
        .andExpect(r -> assertEquals(200, r.getResponse().getStatus()));
    api.perform(
            put("/api/v1/activities/" + ids[0] + "/guest-list")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        java.util.Map.of(
                            "invitados",
                            java.util.List.of(
                                java.util.Map.of(
                                    "cedula",
                                    fictionalId(),
                                    "nombres",
                                    "MARCADOR_LISTADO_FICTICIO"))))))
        .andExpect(r -> assertEquals(200, r.getResponse().getStatus()));
    start(ids[1], owner);
    UUID attempt = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,'Pendiente ficticio','PENDIENTE_REVISION','UBICACION_INCIERTA','Pendiente','CREDENCIALES',?)",
        attempt,
        ids[1],
        ec.edu.unl.geoasist.soporte.ScenarioFixtures.id(102),
        java.sql.Timestamp.from(clock.instant()));
    api.perform(
            post("/api/v1/attempts/" + attempt + "/review")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content("{\"decision\":\"RECHAZAR\",\"razon\":\"Razón ficticia auditada\"}"))
        .andExpect(r -> assertEquals(200, r.getResponse().getStatus()));
    api.perform(
            get("/api/v1/sessions/" + ids[1] + "/attendances/export")
                .header("Authorization", "Bearer " + owner))
        .andExpect(r -> assertEquals(200, r.getResponse().getStatus()));
    api.perform(
            post("/api/v1/sessions/" + ids[1] + "/close")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(r -> assertEquals(200, r.getResponse().getStatus()));
    api.perform(
            post("/api/v1/activities/" + ids[0] + "/close")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(r -> assertEquals(200, r.getResponse().getStatus()));
    var result =
        api.perform(
                get("/api/v1/activities/" + ids[0] + "/audit?size=100")
                    .header("Authorization", "Bearer " + owner))
            .andReturn();
    assertEquals(200, result.getResponse().getStatus());
    var rows = json.readTree(result.getResponse().getContentAsString()).get("items");
    Set<String> actions = new java.util.HashSet<>();
    for (var row : rows) {
      actions.add(row.get("accion").asText());
      assertEquals(
          ec.edu.unl.geoasist.soporte.ScenarioFixtures.id(201).toString(),
          row.get("responsable_id").asText());
      assertNotNull(row.get("ocurrida_en"));
      assertNotNull(row.get("anterior"));
      assertNotNull(row.get("nuevo"));
      assertFalse(row.toString().contains("MARCADOR_LISTADO_FICTICIO"));
    }
    assertEquals(
        Set.of(
            "CONFIGURAR_GEOLOCALIZACION",
            "CONFIGURAR_INVITADOS",
            "REEMPLAZAR_INVITADOS",
            "INICIAR_SESION",
            "REVISAR_INTENTO",
            "EXPORTAR_ASISTENCIAS",
            "CERRAR_SESION",
            "CERRAR_ACTIVIDAD"),
        actions);
  }

  @Test
  void exportIsAuditedWithoutCsvSecretsCoordinatesOrRawIp() throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    api.perform(
            get("/api/v1/sessions/" + ids[1] + "/attendances/export")
                .header("Authorization", "Bearer " + owner))
        .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));
    String audit =
        api.perform(
                get("/api/v1/activities/" + ids[0] + "/audit")
                    .header("Authorization", "Bearer " + owner))
            .andExpect(result -> assertEquals(200, result.getResponse().getStatus()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(audit.contains("EXPORTAR_ASISTENCIAS"));
    assertFalse(
        audit.matches("(?s).*qr_token|registro_invitado_token|latitud|longitud|127\\.0\\.0\\.1.*"));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=? AND accion='EXPORTAR_ASISTENCIAS'",
            Integer.class,
            ids[0]));
  }
}
