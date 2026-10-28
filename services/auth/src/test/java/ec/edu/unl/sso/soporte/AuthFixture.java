package ec.edu.unl.sso.soporte;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL real (Testcontainers), MockMvc, reloj ajustable y semilla dev. */
@SpringBootTest(
    properties = {
      "spring.profiles.active=dev",
      "sso.initial-password=fixture-password-123",
      "sso.service-key=fixture-service-key",
      "sso.retention-cron=-"
    })
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import(AuthFixture.TimeConfig.class)
public abstract class AuthFixture {
  public static final String PASSWORD = "fixture-password-123";
  public static final String WEB = "http://localhost:3000/auth/callback";
  public static final String EXAMPLE = "http://localhost:8082/";

  @TestConfiguration
  public static class TimeConfig {
    @Bean
    @Primary
    public MutableClock testClock() {
      return new MutableClock();
    }
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

  @Autowired protected MutableClock clock;
  @Autowired protected JdbcTemplate db;
  @Autowired protected MockMvc mvc;
  @Autowired protected PasswordEncoder encoder;

  @BeforeEach
  void resetState() {
    clock.set(MutableClock.START);
    db.update(
        "TRUNCATE sesiones_sso, contadores_limite, registro_ingresos, auditoria_admin CASCADE");
    db.update("DELETE FROM usuarios WHERE correo LIKE '%@nuevo.test'");
    db.update("DELETE FROM servicios WHERE nombre LIKE 'test-%'");
    db.update(
        "UPDATE usuarios SET password_hash = ?, debe_cambiar_password = false, activo = (correo <> 'inactiva@example.test')",
        encoder.encode(PASSWORD));
  }

  protected MvcResult login(String correo, String password, String service) throws Exception {
    return mvc.perform(
            post("/cas/login")
                .with(csrf())
                .param("correo", correo)
                .param("password", password)
                .param("service", service))
        .andReturn();
  }

  /** Inicia sesión y devuelve el valor de la cookie CASTGC. */
  protected String tgc(String correo) throws Exception {
    return login(correo, PASSWORD, WEB).getResponse().getCookie("CASTGC").getValue();
  }

  /** Pide un ticket para {@code service} con una sesión SSO existente. */
  protected String ticket(String tgc, String service) throws Exception {
    String location =
        mvc.perform(get("/cas/login").param("service", service).cookie(new Cookie("CASTGC", tgc)))
            .andReturn()
            .getResponse()
            .getHeader("Location");
    return location.substring(location.indexOf("ticket=") + 7);
  }

  /** Cédula válida (módulo 10) de provincia 01 a partir de un número de 0 a 9 999 999. */
  protected static String cedula(int n) {
    String base = String.format("01%07d", 5_000_000 + n);
    int sum = 0;
    for (int i = 0; i < 9; i++) {
      int d = (base.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
      sum += d > 9 ? d - 9 : d;
    }
    return base + (10 - sum % 10) % 10;
  }

  /** Ingresa en /admin/login como administrador y devuelve la cookie ADMINTGC. */
  protected jakarta.servlet.http.Cookie adminCookie() throws Exception {
    var res =
        mvc.perform(
                post("/admin/login")
                    .with(csrf())
                    .param("correo", "admin@dev.geoasist.local")
                    .param("password", PASSWORD))
            .andReturn()
            .getResponse();
    return new jakarta.servlet.http.Cookie("ADMINTGC", res.getCookie("ADMINTGC").getValue());
  }
}
