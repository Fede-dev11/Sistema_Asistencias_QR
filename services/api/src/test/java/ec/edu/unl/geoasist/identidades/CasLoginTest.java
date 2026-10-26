package ec.edu.unl.geoasist.identidades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sun.net.httpserver.HttpServer;
import ec.edu.unl.geoasist.soporte.PostgresFixture;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.test.context.*;

class CasLoginTest extends PostgresFixture {
  private static final AtomicReference<String> RESPONSE = new AtomicReference<>(),
      QUERY = new AtomicReference<>();
  private static final HttpServer SSO;

  static {
    try {
      SSO = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
      SSO.createContext(
          "/cas/p3/serviceValidate",
          x -> {
            QUERY.set(x.getRequestURI().getRawQuery());
            String body = RESPONSE.get();
            if (body == null) {
              x.close();
              return;
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            x.getResponseHeaders().set("Content-Type", "application/json");
            x.sendResponseHeaders(200, bytes.length);
            x.getResponseBody().write(bytes);
            x.close();
          });
      SSO.start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void sso(DynamicPropertyRegistry r) {
    r.add("geoasist.sso.internal-url", () -> "http://127.0.0.1:" + SSO.getAddress().getPort());
    r.add("geoasist.sso.public-url", () -> "https://sso.example.test");
    r.add("geoasist.sso.web-service", () -> "https://geoasist.example.test/auth/callback");
  }

  @BeforeEach
  void reset() {
    RESPONSE.set(
        success("docente.sso@example.test", fictionalId(), List.of("DOCENTE"), List.of(), true));
  }

  @AfterAll
  static void stopSso() {
    SSO.stop(0);
  }

  @Test
  void validExchangeCreatesPasswordlessCasAccount() throws Exception {
    exchange("ST-valid")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").isString())
        .andExpect(jsonPath("$.roles[0]").value("DOCENTE"));
    assertThat(QUERY.get()).contains("format=JSON", "ticket=ST-valid");
    var row =
        db.queryForMap(
            "SELECT proveedor,password_hash FROM cuentas WHERE identificador='docente.sso@example.test'");
    assertThat(row.get("proveedor")).isEqualTo("CAS");
    assertThat(row.get("password_hash")).isNull();
  }

  @Test
  void linksExternalPersonAndKeepsEnrollment() throws Exception {
    UUID person = UUID.randomUUID();
    String cedula = fictionalId();
    db.update("INSERT INTO personas VALUES (?,?,?,'EXTERNA')", person, cedula, "Anterior");
    db.update(
        "INSERT INTO matriculas VALUES (?,?,true)",
        person,
        UUID.fromString("00000000-0000-0000-0000-000000000011"));
    RESPONSE.set(success("link@example.test", cedula, List.of("ESTUDIANTE"), List.of(), true));
    exchange("ST-link").andExpect(status().isOk());
    assertThat(db.queryForObject("SELECT tipo FROM personas WHERE id=?", String.class, person))
        .isEqualTo("INSTITUCIONAL");
    assertThat(
            db.queryForObject(
                "SELECT count(*) FROM matriculas WHERE persona_id=?", Integer.class, person))
        .isOne();
  }

  @Test
  void replacesRolesAndIgnoresUnknownOnSecondLogin() throws Exception {
    String cedula = fictionalId();
    RESPONSE.set(
        success("roles@example.test", cedula, List.of("DOCENTE", "AJENO"), List.of(), true));
    exchange("ST-1");
    RESPONSE.set(
        success("roles@example.test", cedula, List.of("ESTUDIANTE", "OTRO"), List.of(), true));
    exchange("ST-2").andExpect(status().isOk());
    assertThat(
            db.queryForList(
                "SELECT rol FROM roles_cuenta r JOIN cuentas c ON c.id=r.cuenta_id WHERE c.identificador='roles@example.test'",
                String.class))
        .containsExactly("ESTUDIANTE");
  }

  @Test
  void mapsInvalidIdentityCasFailureAndOutage() throws Exception {
    RESPONSE.set(success("bad@example.test", "123", List.of("DOCENTE"), List.of(), true));
    exchange("ST-bad-id")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("IDENTIDAD_INVALIDA"));
    RESPONSE.set(
        "{\"serviceResponse\":{\"authenticationFailure\":{\"code\":\"INVALID_TICKET\",\"description\":\"used\"}}}");
    exchange("ST-used")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TICKET_INVALIDO"));
    RESPONSE.set(null);
    exchange("ST-down")
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("SSO_NO_DISPONIBLE"));
  }

  @Test
  void syncsDirectorCareerAndUpdatesChangedEmailForCedula() throws Exception {
    String cedula = fictionalId();
    RESPONSE.set(
        success(
            "before@example.test",
            cedula,
            List.of("DIRECTOR_CARRERA", "UNKNOWN"),
            List.of("CARRERA-2", "UNKNOWN"),
            true));
    exchange("ST-dir");
    assertThat(
            db.queryForList(
                "SELECT ca.codigo FROM direcciones_carrera d JOIN cuentas c ON c.id=d.cuenta_id JOIN carreras ca ON ca.id=d.carrera_id WHERE c.identificador='before@example.test'",
                String.class))
        .containsExactly("CARRERA-2");
    RESPONSE.set(success("after@example.test", cedula, List.of("DOCENTE"), List.of(), true));
    exchange("ST-mail").andExpect(status().isOk());
    assertThat(
            db.queryForObject(
                "SELECT count(*) FROM cuentas WHERE identificador='after@example.test'",
                Integer.class))
        .isOne();
  }

  @Test
  void rejectsInactiveAndKeepsAccountWithNoKnownRole() throws Exception {
    RESPONSE.set(
        success("inactive@example.test", fictionalId(), List.of("DOCENTE"), List.of(), false));
    exchange("ST-inactive")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CUENTA_INACTIVA"));
    RESPONSE.set(
        success("norole@example.test", fictionalId(), List.of("UNKNOWN"), List.of(), true));
    exchange("ST-norole").andExpect(status().isOk()).andExpect(jsonPath("$.roles").isEmpty());
  }

  @Test
  void exposesSsoConfigurationAndLocalLoginRemainsEnabledInFixture() throws Exception {
    api.perform(get("/api/v1/auth/sso-config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.login_url").value("https://sso.example.test/cas/login"))
        .andExpect(jsonPath("$.logout_url").value("https://sso.example.test/cas/logout"))
        .andExpect(jsonPath("$.web_service").value("https://geoasist.example.test/auth/callback"))
        .andExpect(jsonPath("$.mobile_service_prefix").value("geoasist://auth/callback"));
    token("docente@example.test");
  }

  private org.springframework.test.web.servlet.ResultActions exchange(String ticket)
      throws Exception {
    return api.perform(
        post("/api/v1/auth/cas")
            .contentType("application/json")
            .content(
                json.writeValueAsString(
                    Map.of(
                        "ticket",
                        ticket,
                        "service",
                        "https://geoasist.example.test/auth/callback"))));
  }

  private static String success(
      String email, String cedula, List<String> roles, List<String> careers, boolean active) {
    return "{\"serviceResponse\":{\"authenticationSuccess\":{\"user\":\"%s\",\"attributes\":{\"correo\":[\"%s\"],\"cedula\":[\"%s\"],\"nombres\":[\"Persona SSO\"],\"roles\":%s,\"carreras\":%s,\"activo\":[\"%s\"]}}}}"
        .formatted(email, email, cedula, array(roles), array(careers), active);
  }

  private static String array(List<String> v) {
    return v.stream()
        .map(x -> "\"" + x + "\"")
        .collect(java.util.stream.Collectors.joining(",", "[", "]"));
  }
}
