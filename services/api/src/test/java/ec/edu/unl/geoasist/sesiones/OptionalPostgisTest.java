package ec.edu.unl.geoasist.sesiones;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    properties = {
      "geoasist.hmac-key=fixture-key-exclusivamente-para-pruebas-con-datos-ficticios",
      "geoasist.dev-seed-password=fixture-password",
      "spring.profiles.active=dev,geo",
      "geoasist.scheduling-enabled=false"
    })
@AutoConfigureMockMvc(print = org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
class OptionalPostgisTest {
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"));

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired JdbcTemplate db;
  @Autowired MockMvc api;
  @Autowired ObjectMapper json;

  @Test
  void geoProfileMigratesPostgisAndKeepsCoordinatesTransient() throws Exception {
    assertTrue(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname='postgis')", Boolean.class));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM flyway_geo_schema_history WHERE success AND version='002'",
            Integer.class));

    UUID session = ScenarioFixtures.id(402);
    UUID space = UUID.randomUUID();
    db.update(
        "INSERT INTO espacios(id,nombre,tipo,latitud,longitud) VALUES (?,?,'AULA',-4.0328,-79.2026)",
        space,
        "Espacio ficticio PostGIS");
    db.update("UPDATE sesiones SET espacio_id=? WHERE id=?", space, session);
    db.update(
        "UPDATE actividades SET acceso='ABIERTO' WHERE id=(SELECT actividad_id FROM sesiones WHERE id=?)",
        session);

    String owner = token("encargado@example.test");
    String student = token("estudiante@example.test");
    var inherited =
        json.readTree(
            api.perform(
                    put("/api/v1/sessions/" + session + "/geolocation")
                        .header("Authorization", "Bearer " + owner)
                        .contentType("application/json")
                        .content("{\"habilitada\":true}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertEquals(20.0, inherited.get("configuracion").get("radio_m").asDouble());
    assertEquals(30.0, inherited.get("configuracion").get("precision_max_m").asDouble());
    assertEquals(60, inherited.get("configuracion").get("antiguedad_max_s").asInt());
    String settings =
        api.perform(
                put("/api/v1/sessions/" + session + "/geolocation")
                    .header("Authorization", "Bearer " + owner)
                    .contentType("application/json")
                    .content(
                        "{\"habilitada\":true,\"radio_m\":30,\"precision_max_m\":20,\"antiguedad_max_s\":60}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals("PILOTO_REVISION", json.readTree(settings).get("modo_efectivo").asText());

    api.perform(
            post("/api/v1/sessions/" + session + "/start")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(status().isOk());
    String qr =
        json.readTree(
                api.perform(
                        get("/api/v1/sessions/" + session + "/qr")
                            .header("Authorization", "Bearer " + owner))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("qr_token")
            .asText();

    String response =
        api.perform(
                post("/api/v1/attendance/attempts")
                    .header("Authorization", "Bearer " + student)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "qr_token",
                                qr,
                                "dispositivo_id",
                                UUID.randomUUID(),
                                "ubicacion",
                                Map.of(
                                    "latitud",
                                    -4.0328,
                                    "longitud",
                                    -79.2026,
                                    "precision_m",
                                    5,
                                    "medido_en",
                                    Instant.now())))))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals("PENDIENTE_REVISION", json.readTree(response).get("resultado").asText());
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM information_schema.columns WHERE table_name='intentos_asistencia' AND column_name IN ('latitud','longitud')",
            Integer.class));
    assertNotNull(
        db.queryForObject(
            "SELECT distancia_m FROM intentos_asistencia WHERE id=?",
            Double.class,
            UUID.fromString(json.readTree(response).get("attempt_id").asText())));
  }

  private String token(String email) throws Exception {
    String response =
        api.perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("correo", email, "password", ScenarioFixtures.PASSWORD))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(response).get("access_token").asText();
  }
}
