package ec.edu.unl.sso.cas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.sso.soporte.AuthFixture;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class LoginTest extends AuthFixture {
  @Test
  void correctLoginSetsCookieAndRedirectsWithTicket() throws Exception {
    MvcResult r = login("docente@example.test", PASSWORD, WEB);
    var res = r.getResponse();
    assertEquals(302, res.getStatus());
    assertTrue(res.getHeader("Location").startsWith(WEB + "?ticket=ST-"));
    String cookie = res.getHeader("Set-Cookie");
    assertTrue(cookie.startsWith("CASTGC="));
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("SameSite=Lax"));
    assertTrue(cookie.contains("Path=/cas"));
    assertFalse(cookie.contains("Secure"));
    assertTrue(res.getHeader("Cache-Control").contains("no-store"));
    assertEquals("DENY", res.getHeader("X-Frame-Options"));
  }

  @Test
  void onlyHashesAreStored() throws Exception {
    MvcResult r = login("docente@example.test", PASSWORD, WEB);
    String tgc = r.getResponse().getCookie("CASTGC").getValue();
    String ticket = r.getResponse().getHeader("Location").split("ticket=")[1];
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM sesiones_sso WHERE secreto_hash = ?", Integer.class, tgc));
    assertEquals(
        0, db.queryForObject("SELECT count(*) FROM tickets WHERE hash = ?", Integer.class, ticket));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM tickets WHERE hash ~ '^[0-9a-f]{64}$'", Integer.class));
  }

  @Test
  void wrongCredentialsGiveSameGenericMessageWithoutRedirect() throws Exception {
    var wrongPassword = login("docente@example.test", "mala-contraseña-123", WEB).getResponse();
    var unknownUser = login("nadie@example.test", "mala-contraseña-123", WEB).getResponse();
    for (var res : List.of(wrongPassword, unknownUser)) {
      assertEquals(200, res.getStatus());
      assertNull(res.getHeader("Location"));
      assertNull(res.getCookie("CASTGC"));
      assertTrue(res.getContentAsString().contains("Correo o contraseña incorrectos"));
    }
  }

  @Test
  void inactiveUserIsRejectedWithMessage() throws Exception {
    var res = login("inactiva@example.test", PASSWORD, WEB).getResponse();
    assertEquals(200, res.getStatus());
    assertNull(res.getHeader("Location"));
    assertNull(res.getCookie("CASTGC"));
    assertTrue(res.getContentAsString().contains("desactivada"));
  }

  @Test
  void sixthFailedAttemptInOneMinuteIsBlocked() throws Exception {
    for (int i = 0; i < 5; i++) {
      assertEquals(
          200, login("docente@example.test", "mala-contraseña-123", WEB).getResponse().getStatus());
    }
    var blocked = login("docente@example.test", PASSWORD, WEB).getResponse();
    assertEquals(429, blocked.getStatus());
    assertEquals("60", blocked.getHeader("Retry-After"));
    assertNull(blocked.getCookie("CASTGC"));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM registro_ingresos WHERE resultado = 'BLOQUEADO'", Integer.class));
  }

  @Test
  void perIpLimitBlocksManyDifferentEmails() throws Exception {
    for (int i = 0; i < 50; i++) {
      assertEquals(
          200,
          login("x" + i + "@example.test", "mala-contraseña-123", WEB).getResponse().getStatus());
    }
    assertEquals(
        429, login("y@example.test", "mala-contraseña-123", WEB).getResponse().getStatus());
  }

  @Test
  void loginAttemptsAreRecordedWithoutPasswords() throws Exception {
    login("docente@example.test", "mala-contraseña-123", WEB);
    login("docente@example.test", PASSWORD, WEB);
    List<Map<String, Object>> rows =
        db.queryForList("SELECT * FROM registro_ingresos ORDER BY en, resultado");
    assertEquals(2, rows.size());
    assertEquals(
        List.of("CREDENCIALES", "OK"),
        rows.stream().map(m -> (String) m.get("resultado")).sorted().toList());
    assertFalse(rows.toString().contains("mala-contraseña"));
    assertFalse(rows.toString().contains(PASSWORD));
  }

  @Test
  void mustChangePasswordGoesToPasswordPageThenToService() throws Exception {
    db.update(
        "UPDATE usuarios SET debe_cambiar_password = true WHERE correo = 'docente@example.test'");
    var res = login("docente@example.test", PASSWORD, WEB).getResponse();
    assertEquals(302, res.getStatus());
    assertTrue(res.getHeader("Location").startsWith("/cas/password?service="));
    Cookie tgc = res.getCookie("CASTGC");

    mvc.perform(get("/cas/password").param("service", WEB).cookie(tgc)).andExpect(status().isOk());
    var bad =
        mvc.perform(
                post("/cas/password")
                    .with(csrf())
                    .cookie(tgc)
                    .param("service", WEB)
                    .param("actual", PASSWORD)
                    .param("nueva", "corta")
                    .param("confirmar", "corta"))
            .andReturn()
            .getResponse();
    assertEquals(200, bad.getStatus());
    assertTrue(bad.getContentAsString().contains("entre 12 y 128"));

    var ok =
        mvc.perform(
                post("/cas/password")
                    .with(csrf())
                    .cookie(tgc)
                    .param("service", WEB)
                    .param("actual", PASSWORD)
                    .param("nueva", "nueva-contraseña-segura")
                    .param("confirmar", "nueva-contraseña-segura"))
            .andReturn()
            .getResponse();
    assertEquals(302, ok.getStatus());
    assertTrue(ok.getHeader("Location").startsWith(WEB + "?ticket=ST-"));
    assertFalse(
        db.queryForObject(
            "SELECT debe_cambiar_password FROM usuarios WHERE correo = 'docente@example.test'",
            Boolean.class));
    assertEquals(
        302,
        login("docente@example.test", "nueva-contraseña-segura", WEB).getResponse().getStatus());
  }

  @Test
  void passwordPageWithoutSessionRedirectsToLogin() throws Exception {
    var res = mvc.perform(get("/cas/password").param("service", WEB)).andReturn().getResponse();
    assertEquals(302, res.getStatus());
    assertTrue(res.getHeader("Location").startsWith("/cas/login?service="));
  }

  @Test
  void unregisteredServiceGivesErrorPageWithoutRedirect() throws Exception {
    for (String service : List.of("https://evil.example/", "//evil", "http://localhost@evil")) {
      var res = mvc.perform(get("/cas/login").param("service", service)).andReturn().getResponse();
      assertEquals(400, res.getStatus());
      assertNull(res.getHeader("Location"));
      var post = login("docente@example.test", PASSWORD, service).getResponse();
      assertEquals(400, post.getStatus());
      assertNull(post.getHeader("Location"));
      assertNull(post.getCookie("CASTGC"));
    }
  }

  @Test
  void loginPageRendersFormAndCsrfIsRequired() throws Exception {
    var page = mvc.perform(get("/cas/login").param("service", WEB)).andReturn().getResponse();
    assertEquals(200, page.getStatus());
    String html = page.getContentAsString();
    assertTrue(html.contains("name=\"_csrf\""));
    assertTrue(html.contains("<label for=\"correo\">"));
    mvc.perform(
            post("/cas/login")
                .param("correo", "docente@example.test")
                .param("password", PASSWORD)
                .param("service", WEB))
        .andExpect(status().isForbidden());
  }

  @Test
  void existingSessionIssuesTicketWithoutPasswordAndLogoutClosesIt() throws Exception {
    String tgc = tgc("docente@example.test");
    var again =
        mvc.perform(get("/cas/login").param("service", EXAMPLE).cookie(new Cookie("CASTGC", tgc)))
            .andReturn()
            .getResponse();
    assertEquals(302, again.getStatus());
    assertTrue(again.getHeader("Location").startsWith(EXAMPLE + "?ticket=ST-"));
    var out =
        mvc.perform(get("/cas/logout").param("service", WEB).cookie(new Cookie("CASTGC", tgc)))
            .andReturn()
            .getResponse();
    assertEquals(302, out.getStatus());
    assertEquals(WEB, out.getHeader("Location"));
    assertTrue(out.getHeader("Set-Cookie").contains("Max-Age=0"));
    var after =
        mvc.perform(get("/cas/login").param("service", EXAMPLE).cookie(new Cookie("CASTGC", tgc)))
            .andReturn()
            .getResponse();
    assertEquals(200, after.getStatus());
    var evil =
        mvc.perform(get("/cas/logout").param("service", "https://evil.example/"))
            .andReturn()
            .getResponse();
    assertEquals(200, evil.getStatus());
    assertNull(evil.getHeader("Location"));
  }
}
