package ec.edu.unl.geoasist.espacios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class GeoEvidenceTest extends PostgresFixture {
  private static final String SPACE = ScenarioFixtures.id(501).toString();

  private org.springframework.test.web.servlet.ResultActions postEvidence(
      String bearer, String space, String body) throws Exception {
    return api.perform(
        post("/api/v1/spaces/" + space + "/geo-evidence")
            .header("Authorization", "Bearer " + bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void createsNextImmutableVersionAuditsAndListsNewestFirst() throws Exception {
    String admin = token("admin@dev.geoasist.local");
    String body =
        "{\"referencia\":\"  Informe GPS  \" ,\"realizada_en\":\"2026-10-19T12:00:00Z\",\"falsos_positivos\":0.010000,\"falsos_negativos\":0.020000}";
    postEvidence(admin, SPACE, body)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("version").value(1))
        .andExpect(jsonPath("referencia").value("Informe GPS"));
    postEvidence(admin, SPACE, body)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("version").value(2));
    api.perform(
            get("/api/v1/spaces/" + SPACE + "/geo-evidence")
                .header("Authorization", "Bearer " + token(ScenarioFixtures.TEACHER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].version").value(2))
        .andExpect(jsonPath("$[1].version").value(1));
    org.junit.jupiter.api.Assertions.assertEquals(
        2,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_espacios WHERE espacio_id=?::uuid AND accion='REGISTRAR_EVIDENCIA_GEOGRAFICA'",
            Integer.class,
            SPACE));
    api.perform(
            put("/api/v1/spaces/" + SPACE + "/geo-evidence/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isNotFound());
    api.perform(
            delete("/api/v1/spaces/" + SPACE + "/geo-evidence/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsInvalidInputUnauthorizedActorAndSpaceWithoutLocation() throws Exception {
    String admin = token("admin@dev.geoasist.local");
    for (String body :
        new String[] {
          "{\"referencia\":\"   \",\"realizada_en\":\"2026-10-19T12:00:00Z\",\"falsos_positivos\":0,\"falsos_negativos\":0}",
          "{\"referencia\":\"R\",\"realizada_en\":\"2026-10-21T12:00:00Z\",\"falsos_positivos\":0,\"falsos_negativos\":0}",
          "{\"referencia\":\"R\",\"realizada_en\":\"2026-10-19T12:00:00Z\",\"falsos_positivos\":1.1,\"falsos_negativos\":0}"
        })
      postEvidence(admin, SPACE, body)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("mensaje").isNotEmpty());
    String valid =
        "{\"referencia\":\"R\",\"realizada_en\":\"2026-10-19T12:00:00Z\",\"falsos_positivos\":0,\"falsos_negativos\":0}";
    postEvidence(token(ScenarioFixtures.TEACHER), SPACE, valid).andExpect(status().isForbidden());
    UUID bare = UUID.randomUUID();
    db.update("INSERT INTO espacios(id,nombre,tipo) VALUES (?,'Sin coordenadas','AULA')", bare);
    postEvidence(admin, bare.toString(), valid)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("code").value("ESPACIO_SIN_UBICACION"));
    postEvidence(admin, UUID.randomUUID().toString(), valid).andExpect(status().isNotFound());
  }

  @Test
  void concurrentPostsGetDistinctVersions() throws Exception {
    String admin = token("admin@dev.geoasist.local");
    String body =
        "{\"referencia\":\"Concurrente\",\"realizada_en\":\"2026-10-19T12:00:00Z\",\"falsos_positivos\":0,\"falsos_negativos\":0}";
    var pool = java.util.concurrent.Executors.newFixedThreadPool(4);
    try {
      var futures = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
      for (int i = 0; i < 4; i++)
        futures.add(
            pool.submit(
                () -> postEvidence(admin, SPACE, body).andReturn().getResponse().getStatus()));
      for (var f : futures) org.junit.jupiter.api.Assertions.assertEquals(201, f.get());
    } finally {
      pool.shutdown();
    }
    // other tests share the space: versions must be unique (no UNIQUE violation) and gap-free
    org.junit.jupiter.api.Assertions.assertEquals(
        db.queryForObject(
            "SELECT max(version) FROM evidencias_geograficas WHERE espacio_id=?::uuid",
            Integer.class,
            SPACE),
        db.queryForObject(
            "SELECT count(DISTINCT version) FROM evidencias_geograficas WHERE espacio_id=?::uuid",
            Integer.class,
            SPACE));
  }
}
