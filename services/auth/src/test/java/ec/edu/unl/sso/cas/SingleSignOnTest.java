package ec.edu.unl.sso.cas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.sso.soporte.AuthFixture;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** US2: una sola sesión universitaria sirve a varios servicios, con vencimientos y cierre. */
class SingleSignOnTest extends AuthFixture {
  private static final String VALIDATE = "/cas/p3/serviceValidate";

  private int loginStatus(String tgc, String service) throws Exception {
    return mvc.perform(
            get("/cas/login").param("service", service).cookie(new Cookie("CASTGC", tgc)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  @Test
  void validSessionRedirectsImmediatelyWithNewTicket() throws Exception {
    String tgc = tgc("docente@example.test");
    var res =
        mvc.perform(get("/cas/login").param("service", EXAMPLE).cookie(new Cookie("CASTGC", tgc)))
            .andReturn()
            .getResponse();
    assertEquals(302, res.getStatus());
    assertTrue(res.getHeader("Location").startsWith(EXAMPLE + "?ticket=ST-"));
  }

  @Test
  void ticketOfOneServiceIsInvalidForAnother() throws Exception {
    String t = ticket(tgc("docente@example.test"), WEB);
    mvc.perform(get(VALIDATE).param("service", EXAMPLE).param("ticket", t).param("format", "JSON"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationFailure.code").value("INVALID_SERVICE"));
  }

  @Test
  void unregisteredServiceIsBadRequestWithoutLocation() throws Exception {
    String tgc = tgc("docente@example.test");
    var res =
        mvc.perform(
                get("/cas/login")
                    .param("service", "https://evil.example/cb")
                    .cookie(new Cookie("CASTGC", tgc)))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse();
    assertNull(res.getHeader("Location"));
  }

  @Test
  void logoutClosesSessionAndNextLoginAsksForCredentials() throws Exception {
    String tgc = tgc("docente@example.test");
    assertEquals(302, loginStatus(tgc, EXAMPLE));
    mvc.perform(get("/cas/logout").cookie(new Cookie("CASTGC", tgc))).andExpect(status().isOk());
    assertEquals(200, loginStatus(tgc, EXAMPLE));
  }

  @Test
  void idleSessionExpiresAfterEightHours() throws Exception {
    String tgc = tgc("docente@example.test");
    clock.set(clock.instant().plus(Duration.ofHours(7)));
    assertEquals(302, loginStatus(tgc, EXAMPLE));
    clock.set(clock.instant().plus(Duration.ofHours(8)).plusSeconds(1));
    assertEquals(200, loginStatus(tgc, EXAMPLE));
  }

  @Test
  void activeSessionStillEndsAfterTwentyFourHours() throws Exception {
    String tgc = tgc("docente@example.test");
    for (int i = 0; i < 3; i++) {
      clock.set(clock.instant().plus(Duration.ofHours(7)));
      assertEquals(302, loginStatus(tgc, EXAMPLE));
    }
    clock.set(clock.instant().plus(Duration.ofHours(3)));
    assertEquals(200, loginStatus(tgc, EXAMPLE));
  }
}
