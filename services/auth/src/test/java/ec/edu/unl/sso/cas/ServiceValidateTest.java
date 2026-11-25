package ec.edu.unl.sso.cas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.sso.soporte.AuthFixture;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class ServiceValidateTest extends AuthFixture {
  private static final String VALIDATE = "/cas/p3/serviceValidate";

  private String fresh(String email, String service) throws Exception {
    return ticket(tgc(email), service);
  }

  @Test
  void jsonSuccessCarriesAttributes() throws Exception {
    String t = fresh("director-carrera@example.test", WEB);
    mvc.perform(get(VALIDATE).param("service", WEB).param("ticket", t).param("format", "JSON"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.serviceResponse.authenticationSuccess.user")
                .value("director-carrera@example.test"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationSuccess.attributes.correo[0]")
                .value("director-carrera@example.test"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationSuccess.attributes.cedula[0]")
                .value("0100000058"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationSuccess.attributes.nombres[0]")
                .value("Persona ficticia 5"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationSuccess.attributes.roles[0]")
                .value("DIRECTOR_CARRERA"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationSuccess.attributes.carreras[0]")
                .value("CARRERA-1"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationSuccess.attributes.activo[0]").value("true"));
  }

  @Test
  void xmlIsTheDefaultFormat() throws Exception {
    String t = fresh("docente@example.test", WEB);
    String xml =
        mvc.perform(get(VALIDATE).param("service", WEB).param("ticket", t))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/xml"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(xml.contains("<cas:authenticationSuccess>"));
    assertTrue(xml.contains("<cas:user>docente@example.test</cas:user>"));
    assertTrue(xml.contains("<cas:cedula>0100000017</cas:cedula>"));
    assertTrue(xml.contains("<cas:roles>DOCENTE</cas:roles>"));
    assertTrue(xml.contains("<cas:activo>true</cas:activo>"));
  }

  @Test
  void ticketIsSingleUse() throws Exception {
    String t = fresh("docente@example.test", WEB);
    mvc.perform(get(VALIDATE).param("service", WEB).param("ticket", t).param("format", "JSON"))
        .andExpect(jsonPath("$.serviceResponse.authenticationSuccess").exists());
    mvc.perform(get(VALIDATE).param("service", WEB).param("ticket", t).param("format", "JSON"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationFailure.code").value("INVALID_TICKET"));
  }

  @Test
  void expiredTicketIsInvalid() throws Exception {
    String t = fresh("docente@example.test", WEB);
    clock.set(MUTABLE_START_PLUS(11));
    mvc.perform(get(VALIDATE).param("service", WEB).param("ticket", t).param("format", "JSON"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationFailure.code").value("INVALID_TICKET"));
  }

  @Test
  void ticketStillValidAtNineSeconds() throws Exception {
    String t = fresh("docente@example.test", WEB);
    clock.set(MUTABLE_START_PLUS(9));
    mvc.perform(get(VALIDATE).param("service", WEB).param("ticket", t).param("format", "JSON"))
        .andExpect(jsonPath("$.serviceResponse.authenticationSuccess").exists());
  }

  @Test
  void otherServiceIsInvalidServiceAndBurnsTheTicket() throws Exception {
    String t = fresh("docente@example.test", WEB);
    mvc.perform(get(VALIDATE).param("service", EXAMPLE).param("ticket", t).param("format", "JSON"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationFailure.code").value("INVALID_SERVICE"));
    mvc.perform(get(VALIDATE).param("service", WEB).param("ticket", t).param("format", "JSON"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationFailure.code").value("INVALID_TICKET"));
  }

  @Test
  void missingParametersAreInvalidRequestInBothFormats() throws Exception {
    mvc.perform(get(VALIDATE).param("format", "JSON"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationFailure.code").value("INVALID_REQUEST"));
    mvc.perform(get(VALIDATE).param("service", WEB))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("code=\"INVALID_REQUEST\"")));
    mvc.perform(
            get(VALIDATE)
                .param("service", WEB)
                .param("ticket", "ST-inexistente")
                .param("format", "JSON"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationFailure.code").value("INVALID_TICKET"));
    mvc.perform(get(VALIDATE).param("service", WEB).param("ticket", "basura"))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("code=\"INVALID_TICKET\"")));
  }

  @Test
  void ticketOfClosedSessionOrInactiveUserIsInvalid() throws Exception {
    String tgc = tgc("docente@example.test");
    String t = ticket(tgc, WEB);
    db.update("UPDATE sesiones_sso SET cerrada_en = ?", java.sql.Timestamp.from(Instant.now()));
    mvc.perform(get(VALIDATE).param("service", WEB).param("ticket", t).param("format", "JSON"))
        .andExpect(
            jsonPath("$.serviceResponse.authenticationFailure.code").value("INVALID_TICKET"));
  }

  @Test
  void twentyConcurrentValidationsYieldExactlyOneSuccess() throws Exception {
    String t = fresh("docente@example.test", WEB);
    ExecutorService pool = Executors.newFixedThreadPool(20);
    try {
      List<Callable<Boolean>> jobs = new ArrayList<>();
      for (int i = 0; i < 20; i++) {
        jobs.add(
            () ->
                mvc.perform(
                        get(VALIDATE)
                            .param("service", WEB)
                            .param("ticket", t)
                            .param("format", "JSON"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString()
                    .contains("authenticationSuccess"));
      }
      int ok = 0;
      for (Future<Boolean> f : pool.invokeAll(jobs)) if (f.get()) ok++;
      assertEquals(1, ok);
    } finally {
      pool.shutdownNow();
    }
  }

  private static Instant MUTABLE_START_PLUS(int seconds) {
    return ec.edu.unl.sso.soporte.MutableClock.START.plusSeconds(seconds);
  }
}
