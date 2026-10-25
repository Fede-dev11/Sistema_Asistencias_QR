package ec.edu.unl.geoasist.actividades;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GuestConfigurationContractTest extends PostgresFixture {
  @Test
  void guestDurationIsRequiredAndExclusiveToEvent() throws Exception {
    UUID[] ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    for (String value :
        List.of(
            "{}",
            "{\"habilitado\":true}",
            "{\"habilitado\":true,\"tolerancia_invitado_segundos\":40}",
            "{\"habilitado\":true,\"tolerancia_invitado_segundos\":41.5}"))
      api.perform(
              put("/api/v1/activities/" + ids[0] + "/guest-settings")
                  .header("Authorization", "Bearer " + owner)
                  .contentType("application/json")
                  .content(value))
          .andExpect(status().isBadRequest());
    api.perform(
            put("/api/v1/activities/" + ids[0] + "/guest-settings")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content("{\"habilitado\":true,\"tolerancia_invitado_segundos\":41}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("tolerancia_invitado_segundos").value(41));
    UUID[] clazz = scenario("CLASE", 201);
    api.perform(
            put("/api/v1/activities/" + clazz[0] + "/guest-settings")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content("{\"habilitado\":true,\"tolerancia_invitado_segundos\":120}"))
        .andExpect(status().isConflict());
  }

  @Test
  void listIsAtomicDeduplicatedAndUsesTextIds() throws Exception {
    UUID[] ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    String path = "/api/v1/activities/" + ids[0] + "/guest-list";
    api.perform(
            put(path)
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content(
                    "{\"invitados\":[{\"cedula\":\" 0123456782 \",\"nombres\":\"Invitado ficticio\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("total").value(1));
    api.perform(
            put(path)
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content(
                    "{\"invitados\":[{\"cedula\":\"0123456782\",\"nombres\":\"Primero\"},{\"cedula\":\"0123456782\",\"nombres\":\"Duplicado\"}]}"))
        .andExpect(status().isBadRequest());
    api.perform(
            put(path)
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content("{\"invitados\":[{\"cedula\":\"inválida\",\"nombres\":\"Invitado\"}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("code").value("CEDULA_INVALIDA"));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM invitados_evento WHERE actividad_id=?", Integer.class, ids[0]));
    assertEquals(
        "0123456782",
        db.queryForObject(
            "SELECT p.cedula_normalizada FROM personas p JOIN invitados_evento i ON i.persona_id=p.id WHERE i.actividad_id=?",
            String.class,
            ids[0]));
  }

  @Test
  void identifiersAndDurationsRespectJsonTypes() throws Exception {
    UUID[] ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    api.perform(
            put("/api/v1/activities/" + ids[0] + "/guest-list")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content(
                    "{\"invitados\":[{\"cedula\":1000000000,\"nombres\":\"Persona ficticia\"}]}"))
        .andExpect(status().isBadRequest());
    api.perform(
            put("/api/v1/activities/" + ids[0] + "/guest-settings")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content("{\"habilitado\":true,\"tolerancia_invitado_segundos\":\"120\"}"))
        .andExpect(status().isBadRequest());
  }
}
