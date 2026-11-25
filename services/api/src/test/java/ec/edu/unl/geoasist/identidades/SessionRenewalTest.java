package ec.edu.unl.geoasist.identidades;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sun.net.httpserver.HttpServer;
import ec.edu.unl.geoasist.soporte.PostgresFixture;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.test.context.*;

class SessionRenewalTest extends PostgresFixture {
  private static final AtomicReference<String> STATUS = new AtomicReference<>();
  private static final HttpServer SSO;

  static {
    try {
      SSO = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
      SSO.createContext(
          "/api/v1/users/status",
          x -> {
            String body = STATUS.get();
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
  }

  @AfterAll
  static void stopSso() {
    SSO.stop(0);
  }

  @AfterEach
  void restoreProvider() {
    db.update(
        "UPDATE cuentas SET proveedor='DEV',password_hash=(SELECT password_hash FROM cuentas WHERE identificador='docente@example.test') WHERE identificador='estudiante@example.test'");
  }

  @Test
  void casAccountRenewalChecksSsoAndFailsClosed() throws Exception {
    String first = token("estudiante@example.test");
    String second = token("estudiante@example.test");
    db.update(
        "UPDATE cuentas SET proveedor='CAS',password_hash=NULL WHERE identificador='estudiante@example.test'");
    STATUS.set("{\"activo\":true}");
    String t = renew(first, 200);
    me(t, 200);
    STATUS.set("{\"activo\":false}");
    api.perform(post("/api/v1/auth/renew").header("Authorization", "Bearer " + t))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("code").value("CUENTA_INACTIVA"));
    me(t, 401);
    STATUS.set(null);
    api.perform(post("/api/v1/auth/renew").header("Authorization", "Bearer " + second))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("code").value("SSO_NO_DISPONIBLE"));
  }

  String renew(String bearer, int status) throws Exception {
    var r = api.perform(post("/api/v1/auth/renew").header("Authorization", "Bearer " + bearer));
    r.andExpect(status().is(status));
    return status == 200
        ? json.readTree(r.andReturn().getResponse().getContentAsString())
            .get("access_token")
            .asText()
        : null;
  }

  void me(String bearer, int status) throws Exception {
    api.perform(get("/api/v1/me").header("Authorization", "Bearer " + bearer))
        .andExpect(status().is(status));
  }

  @Test
  void renewalRotatesSecretAndOldOneStopsWorking() throws Exception {
    String first = token("estudiante@example.test");
    String second = renew(first, 200);
    me(first, 401);
    me(second, 200);
    renew(first, 401);
  }

  @Test
  void revokedOrExpiredSessionCannotRenew() throws Exception {
    String t = token("estudiante@example.test");
    api.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + t));
    renew(t, 401);
    String u = token("estudiante@example.test");
    clock.set(clock.instant().plus(Duration.ofHours(9)));
    renew(u, 401);
  }

  @Test
  void renewsUpToThirtyDaysFromOriginalLogin() throws Exception {
    var start = clock.instant();
    String t = token("estudiante@example.test");
    for (int k = 1; k <= 4 * 29 + 3; k++) {
      clock.set(start.plus(Duration.ofHours(6L * k)));
      t = renew(t, 200);
    }
    clock.set(start.plus(Duration.ofDays(30)).plus(Duration.ofMinutes(1)));
    api.perform(post("/api/v1/auth/renew").header("Authorization", "Bearer " + t))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("code").value("RENOVACION_VENCIDA"));
  }
}
