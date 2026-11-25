package ec.edu.unl.sso.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.sso.cas.aplicacion.SsoSessions;
import ec.edu.unl.sso.soporte.AuthFixture;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminUsersTest extends AuthFixture {
  private static final Pattern SECRET = Pattern.compile("class=\"secreto\">([^<]+)<");

  @Autowired SsoSessions sessions;

  private MockHttpServletResponse createUser(Cookie admin, String correo, String cedula, String rol)
      throws Exception {
    return mvc.perform(
            post("/admin/usuarios")
                .with(csrf())
                .cookie(admin)
                .param("correo", correo)
                .param("cedula", cedula)
                .param("nombres", "Persona de prueba")
                .param("roles", rol)
                .param("carreras", "CARRERA-1, CARRERA-2"))
        .andReturn()
        .getResponse();
  }

  private static String secret(MockHttpServletResponse res) throws Exception {
    Matcher m = SECRET.matcher(res.getContentAsString());
    assertTrue(m.find(), "se esperaba un secreto en la página");
    return m.group(1);
  }

  @Test
  void adminAreaRequiresAdministratorSession() throws Exception {
    mvc.perform(get("/admin/usuarios"))
        .andExpect(status().isFound())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                "/admin/login"));
    // credenciales válidas de un docente: 403 y ninguna cookie de administración
    var res =
        mvc.perform(
                post("/admin/login")
                    .with(csrf())
                    .param("correo", "docente@example.test")
                    .param("password", PASSWORD))
            .andExpect(status().isForbidden())
            .andReturn()
            .getResponse();
    assertEquals(null, res.getCookie("ADMINTGC"));
    // sesión SSO válida pero de un docente
    UUID docente =
        db.queryForObject(
            "SELECT id FROM usuarios WHERE correo = 'docente@example.test'", UUID.class);
    Cookie cookie = new Cookie("ADMINTGC", sessions.crear(docente));
    mvc.perform(get("/admin/usuarios").cookie(cookie)).andExpect(status().isForbidden());
    mvc.perform(post("/admin/usuarios/" + docente + "/restablecer").with(csrf()).cookie(cookie))
        .andExpect(status().isForbidden());
    mvc.perform(get("/admin/usuarios").cookie(adminCookie())).andExpect(status().isOk());
  }

  @Test
  void adminPostsRequireCsrf() throws Exception {
    mvc.perform(post("/admin/usuarios").cookie(adminCookie()).param("correo", "x@nuevo.test"))
        .andExpect(status().isForbidden());
  }

  @Test
  void createsUserWithLowercaseEmailAndTemporaryPassword() throws Exception {
    Cookie admin = adminCookie();
    var res = createUser(admin, "Alta.Uno@NUEVO.test", cedula(1), "DOCENTE");
    assertEquals(200, res.getStatus());
    String temporal = secret(res);
    assertEquals(16, temporal.length());
    assertTrue(
        db.queryForObject(
            "SELECT debe_cambiar_password FROM usuarios WHERE correo = 'alta.uno@nuevo.test'",
            Boolean.class));
    assertEquals(
        2,
        db.queryForObject(
            "SELECT count(*) FROM usuario_carreras c JOIN usuarios u ON u.id = c.usuario_id WHERE u.correo = 'alta.uno@nuevo.test'",
            Integer.class));
    // ingresa con la temporal y queda obligado a cambiarla
    var login = login("alta.uno@nuevo.test", temporal, WEB).getResponse();
    assertTrue(login.getHeader("Location").startsWith("/cas/password"));
  }

  @Test
  void rejectsDuplicatesAndInvalidData() throws Exception {
    Cookie admin = adminCookie();
    assertEquals(200, createUser(admin, "dup@nuevo.test", cedula(2), "DOCENTE").getStatus());
    assertEquals(400, createUser(admin, "DUP@nuevo.test", cedula(3), "DOCENTE").getStatus());
    assertEquals(400, createUser(admin, "otro@nuevo.test", cedula(2), "DOCENTE").getStatus());
    assertEquals(400, createUser(admin, "mala@nuevo.test", "0100000018", "DOCENTE").getStatus());
    assertEquals(400, createUser(admin, "rol@nuevo.test", cedula(4), "SUPERUSUARIO").getStatus());
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM usuarios WHERE correo LIKE '%@nuevo.test'", Integer.class));
  }

  @Test
  void deactivatingClosesSsoSessions() throws Exception {
    Cookie admin = adminCookie();
    String temporal = secret(createUser(admin, "baja@nuevo.test", cedula(5), "ESTUDIANTE"));
    String tgc =
        login("baja@nuevo.test", temporal, WEB).getResponse().getCookie("CASTGC").getValue();
    UUID id =
        db.queryForObject("SELECT id FROM usuarios WHERE correo = 'baja@nuevo.test'", UUID.class);
    mvc.perform(
            post("/admin/usuarios/" + id + "/estado")
                .with(csrf())
                .cookie(admin)
                .param("activo", "false"))
        .andExpect(status().isOk());
    assertFalse(db.queryForObject("SELECT activo FROM usuarios WHERE id = ?", Boolean.class, id));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM sesiones_sso WHERE usuario_id = ? AND cerrada_en IS NULL",
            Integer.class,
            id));
    mvc.perform(get("/cas/login").param("service", WEB).cookie(new Cookie("CASTGC", tgc)))
        .andExpect(status().isOk());
  }

  @Test
  void adminCannotDeactivateSelf() throws Exception {
    UUID admin =
        db.queryForObject(
            "SELECT id FROM usuarios WHERE correo = 'admin@dev.geoasist.local'", UUID.class);
    mvc.perform(
            post("/admin/usuarios/" + admin + "/estado")
                .with(csrf())
                .cookie(adminCookie())
                .param("activo", "false"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resetGeneratesShownOnceTemporaryAndClosesSessions() throws Exception {
    Cookie admin = adminCookie();
    String inicial = secret(createUser(admin, "reset@nuevo.test", cedula(6), "DOCENTE"));
    login("reset@nuevo.test", inicial, WEB);
    UUID id =
        db.queryForObject("SELECT id FROM usuarios WHERE correo = 'reset@nuevo.test'", UUID.class);
    var res =
        mvc.perform(post("/admin/usuarios/" + id + "/restablecer").with(csrf()).cookie(admin))
            .andReturn()
            .getResponse();
    String temporal = secret(res);
    assertEquals(16, temporal.length());
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM sesiones_sso WHERE usuario_id = ? AND cerrada_en IS NULL",
            Integer.class,
            id));
    assertTrue(
        db.queryForObject(
            "SELECT debe_cambiar_password FROM usuarios WHERE id = ?", Boolean.class, id));
    assertEquals(200, login("reset@nuevo.test", inicial, WEB).getResponse().getStatus());
    assertEquals(302, login("reset@nuevo.test", temporal, WEB).getResponse().getStatus());
    // la página del usuario no vuelve a mostrar el secreto
    assertFalse(
        mvc.perform(get("/admin/usuarios/" + id).cookie(admin))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .contains(temporal));
  }

  @Test
  void actionsAreAuditedWithoutPasswords() throws Exception {
    Cookie admin = adminCookie();
    String temporal = secret(createUser(admin, "audit@nuevo.test", cedula(7), "DOCENTE"));
    UUID id =
        db.queryForObject("SELECT id FROM usuarios WHERE correo = 'audit@nuevo.test'", UUID.class);
    mvc.perform(post("/admin/usuarios/" + id + "/restablecer").with(csrf()).cookie(admin));
    mvc.perform(
        post("/admin/usuarios/" + id + "/estado")
            .with(csrf())
            .cookie(admin)
            .param("activo", "false"));
    assertEquals(
        3,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_admin WHERE objetivo_id = ?", Integer.class, id));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_admin WHERE coalesce(anterior::text,'') || coalesce(nuevo::text,'') ~* ('password_hash|\\$2a\\$|' || ?)",
            Integer.class,
            temporal));
  }

  @Test
  void searchesByEmailOrCedulaAndPaginates() throws Exception {
    Cookie admin = adminCookie();
    String body =
        mvc.perform(get("/admin/usuarios").cookie(admin).param("q", "0100000017"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(body.contains("docente@example.test"));
    assertFalse(body.contains("encargado@example.test"));
    body =
        mvc.perform(get("/admin/usuarios").cookie(admin).param("q", "ENCARGADO@"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(body.contains("encargado@example.test"));
    mvc.perform(get("/admin/usuarios").cookie(admin).param("pagina", "99"))
        .andExpect(status().isOk());
  }

  @Test
  void registeredServicesValidateReturnPrefixesAndRotateKeys() throws Exception {
    Cookie admin = adminCookie();
    for (String bad :
        new String[] {
          "http://evil.example/cb",
          "javascript:alert(1)",
          "https://u:p@host.test/",
          "https://host.test/a/../b"
        }) {
      mvc.perform(
              post("/admin/servicios")
                  .with(csrf())
                  .cookie(admin)
                  .param("nombre", "test-malo")
                  .param("retornos", bad))
          .andExpect(status().isBadRequest());
    }
    var res =
        mvc.perform(
                post("/admin/servicios")
                    .with(csrf())
                    .cookie(admin)
                    .param("nombre", "test-ok")
                    .param("retornos", "https://app.test/auth/callback"))
            .andReturn()
            .getResponse();
    assertEquals(200, res.getStatus());
    String key = secret(res);
    mvc.perform(
            post("/admin/servicios")
                .with(csrf())
                .cookie(admin)
                .param("nombre", "test-ok")
                .param("retornos", "https://otra.test/"))
        .andExpect(status().isBadRequest());
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM servicios WHERE nombre = 'test-ok' AND clave_hash = encode(sha256(convert_to(?, 'UTF8')), 'hex')",
            Integer.class,
            key));
    // regenerar: la clave anterior deja de valer
    UUID id = db.queryForObject("SELECT id FROM servicios WHERE nombre = 'test-ok'", UUID.class);
    String nueva =
        secret(
            mvc.perform(post("/admin/servicios/" + id + "/clave").with(csrf()).cookie(admin))
                .andReturn()
                .getResponse());
    mvc.perform(
            get("/api/v1/users/status")
                .param("correo", "docente@example.test")
                .header("X-Service-Key", key))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/v1/users/status")
                .param("correo", "docente@example.test")
                .header("X-Service-Key", nueva))
        .andExpect(status().isOk());
    // editar, desactivar y que deje de aceptar retornos
    mvc.perform(
            post("/admin/servicios/" + id)
                .with(csrf())
                .cookie(admin)
                .param("nombre", "test-ok")
                .param("retornos", "https://app.test/auth/callback"))
        .andExpect(status().isOk());
    mvc.perform(get("/cas/login").param("service", "https://app.test/auth/callback"))
        .andExpect(status().isBadRequest());
    assertEquals(
        3,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_admin WHERE objetivo_id = ?", Integer.class, id));
  }
}
