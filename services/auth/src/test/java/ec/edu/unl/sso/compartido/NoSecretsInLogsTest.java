package ec.edu.unl.sso.compartido;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.edu.unl.sso.soporte.AuthFixture;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** SC-004 (servicio): ningún ticket, contraseña ni CASTGC en los registros. */
@ExtendWith(OutputCaptureExtension.class)
class NoSecretsInLogsTest extends AuthFixture {
  @Test
  void redactionPatternWorksAtRuntime(CapturedOutput out) {
    LoggerFactory.getLogger("ec.edu.unl.sso.prueba")
        .info("GET /cas/p3/serviceValidate?service=x&ticket=ST-secreto&format=JSON");
    LoggerFactory.getLogger("ec.edu.unl.sso.prueba")
        .info("password=hunter2hunter2 CASTGC=tgc-secreto; Path=/cas");
    String all = out.getAll();
    assertTrue(all.contains("ticket=***&format=JSON"), all);
    assertTrue(all.contains("password=*** CASTGC=***;"), all);
    assertFalse(all.contains("ST-secreto"));
    assertFalse(all.contains("hunter2"));
    assertFalse(all.contains("tgc-secreto"));
  }

  @Test
  void fullFlowLeavesNoSecretsInOutput(CapturedOutput out) throws Exception {
    String tgc = tgc("docente@example.test");
    String ticket = ticket(tgc, EXAMPLE);
    mvc.perform(get("/cas/p3/serviceValidate").param("service", EXAMPLE).param("ticket", ticket));
    login("docente@example.test", "contraseña-equivocada-1", WEB);
    mvc.perform(
        post("/cas/password")
            .with(csrf())
            .cookie(new Cookie("CASTGC", tgc))
            .param("actual", "actual-secreta-xyz")
            .param("nueva", "nueva-secreta-xyz-1")
            .param("confirmar", "nueva-secreta-xyz-1"));
    String all = out.getAll();
    for (String secret :
        new String[] {
          tgc,
          ticket,
          PASSWORD,
          "contraseña-equivocada-1",
          "actual-secreta-xyz",
          "nueva-secreta-xyz-1"
        }) {
      assertFalse(all.contains(secret), "aparece un secreto en los registros");
    }
  }
}
