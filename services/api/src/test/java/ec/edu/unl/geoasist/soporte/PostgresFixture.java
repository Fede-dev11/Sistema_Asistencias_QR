package ec.edu.unl.geoasist.soporte;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "geoasist.hmac-key=fixture-key-exclusivamente-para-pruebas-con-datos-ficticios",
      "geoasist.dev-seed-password=fixture-password",
      "spring.profiles.active=dev",
      "geoasist.rate-limit-multiplier=1000",
      "geoasist.scheduling-enabled=false"
    })
@AutoConfigureMockMvc(print = org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
@org.springframework.context.annotation.Import(PostgresFixture.TimeConfig.class)
public abstract class PostgresFixture {
  @org.springframework.boot.test.context.TestConfiguration
  public static class TimeConfig {
    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    public MutableClock testClock() {
      return new MutableClock();
    }
  }

  @org.springframework.beans.factory.annotation.Autowired protected MutableClock clock;

  @org.junit.jupiter.api.BeforeEach
  void resetClock() {
    clock.set(java.time.Instant.parse("2026-10-20T01:00:00Z"));
  }

  protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.7");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @org.springframework.beans.factory.annotation.Autowired
  protected org.springframework.jdbc.core.JdbcTemplate db;

  @org.springframework.beans.factory.annotation.Autowired
  protected org.springframework.test.web.servlet.MockMvc api;

  @org.springframework.beans.factory.annotation.Autowired
  protected tools.jackson.databind.ObjectMapper json;

  protected String token(String email) throws Exception {
    var result =
        api.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/v1/auth/login")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            java.util.Map.of(
                                "correo", email, "password", ScenarioFixtures.PASSWORD))))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
  }

  protected java.util.UUID[] scenario(String mode, int owner) {
    var activity = java.util.UUID.randomUUID();
    var session = java.util.UUID.randomUUID();
    db.update(
        "INSERT INTO actividades(id,nombre,modalidad,responsable_id) VALUES (?,?,?,?)",
        activity,
        "Escenario ficticio",
        mode,
        ScenarioFixtures.id(owner));
    db.update("INSERT INTO actividad_carreras VALUES (?,?)", activity, ScenarioFixtures.id(1));
    db.update(
        "INSERT INTO sesiones(id,actividad_id,ventana_inicio,ventana_fin) VALUES (?,?,?,?)",
        session,
        activity,
        java.sql.Timestamp.from(clock.instant().minusSeconds(3600)),
        java.sql.Timestamp.from(clock.instant().plusSeconds(86400)));
    return new java.util.UUID[] {activity, session};
  }

  protected void start(java.util.UUID session, String bearer) throws Exception {
    api.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/v1/sessions/" + session + "/start")
                .header("Authorization", "Bearer " + bearer)
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString()))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
  }

  protected String qr(java.util.UUID session, String owner) throws Exception {
    return json.readTree(
            api.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                            "/api/v1/sessions/" + session + "/qr")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(
                    org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .get("qr_token")
        .asText();
  }

  protected record GuestScenario(
      java.util.UUID activity, java.util.UUID session, String owner, String qr) {}

  protected GuestScenario guests(int duration, String... cedulas) throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    api.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/activities/" + ids[0] + "/guest-settings")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        java.util.Map.of(
                            "habilitado", true, "tolerancia_invitado_segundos", duration))))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    var entries =
        java.util.Arrays.stream(cedulas)
            .map(c -> java.util.Map.of("cedula", c, "nombres", "Nombre del listado ficticio"))
            .toList();
    api.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/activities/" + ids[0] + "/guest-list")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("invitados", entries))))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    start(ids[1], owner);
    return new GuestScenario(ids[0], ids[1], owner, qr(ids[1], owner));
  }

  protected tools.jackson.databind.JsonNode open(GuestScenario event) throws Exception {
    return json.readTree(
        api.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/v1/guest-registrations")
                    .contentType("application/json")
                    .content(json.writeValueAsString(java.util.Map.of("qr_token", event.qr()))))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                    .isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  protected org.springframework.test.web.servlet.ResultActions guestSubmit(
      String secret, String cedula, java.util.UUID key) throws Exception {
    // Dispositivo estable por clave: un reintento idempotente conserva el mismo cuerpo canonico.
    return guestSubmit(
        secret, cedula, key, java.util.UUID.nameUUIDFromBytes(key.toString().getBytes()));
  }

  protected org.springframework.test.web.servlet.ResultActions guestSubmit(
      String secret, String cedula, java.util.UUID key, java.util.UUID deviceId) throws Exception {
    return api.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/v1/guest-registrations/submit")
            .header("Idempotency-Key", key.toString())
            .contentType("application/json")
            .content(
                json.writeValueAsString(
                    java.util.Map.of(
                        "registro_invitado_token", secret,
                        "cedula", cedula,
                        "dispositivo_id", deviceId))));
  }

  private static final java.util.concurrent.atomic.AtomicLong FICTIONAL_IDS =
      new java.util.concurrent.atomic.AtomicLong(500000);

  protected String fictionalId() {
    String base = "010%06d".formatted(FICTIONAL_IDS.incrementAndGet());
    int sum = 0;
    for (int i = 0; i < 9; i++) {
      int d = (base.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
      sum += d > 9 ? d - 9 : d;
    }
    return base + (10 - sum % 10) % 10;
  }
}
