package ec.edu.unl.sso.usuarios.presentacion;

import ec.edu.unl.sso.cas.aplicacion.SsoSessions;
import ec.edu.unl.sso.cas.persistencia.LoginLog;
import ec.edu.unl.sso.cas.persistencia.LoginLog.Resultado;
import ec.edu.unl.sso.compartido.RateLimiter;
import ec.edu.unl.sso.servicios.aplicacion.ManageServices;
import ec.edu.unl.sso.servicios.persistencia.ServiceRegistry;
import ec.edu.unl.sso.usuarios.aplicacion.CsvUserImport;
import ec.edu.unl.sso.usuarios.aplicacion.ManageUsers;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Administración HTML (/admin/**). Exige una sesión SSO (TGT) de un usuario ADMINISTRADOR. Como
 * CASTGC tiene Path=/cas y no llega aquí, el ingreso en /admin/login crea una sesión SSO normal
 * cuyo secreto viaja en la cookie ADMINTGC (Path=/admin).
 */
@Controller
@RequestMapping("/admin")
class AdminController {
  static final String COOKIE = "ADMINTGC";
  private static final int PAGE_SIZE = 20;

  private static final class SinSesion extends RuntimeException {}

  private static final class Prohibido extends RuntimeException {}

  private final SsoSessions sessions;
  private final UserRepository users;
  private final ManageUsers manageUsers;
  private final CsvUserImport csv;
  private final ManageServices manageServices;
  private final ServiceRegistry registry;
  private final PasswordEncoder encoder;
  private final RateLimiter limiter;
  private final LoginLog log;
  private final boolean secure;
  private final Duration cookieMaxAge;
  private final int limitEmail;
  private final int limitIp;
  private final String dummyHash;

  AdminController(
      SsoSessions sessions,
      UserRepository users,
      ManageUsers manageUsers,
      CsvUserImport csv,
      ManageServices manageServices,
      ServiceRegistry registry,
      PasswordEncoder encoder,
      RateLimiter limiter,
      LoginLog log,
      @Value("${sso.cookie-secure}") boolean secure,
      @Value("${sso.session-max}") Duration cookieMaxAge,
      @Value("${sso.login-limit-email}") int limitEmail,
      @Value("${sso.login-limit-ip}") int limitIp) {
    this.sessions = sessions;
    this.users = users;
    this.manageUsers = manageUsers;
    this.csv = csv;
    this.manageServices = manageServices;
    this.registry = registry;
    this.encoder = encoder;
    this.limiter = limiter;
    this.log = log;
    this.secure = secure;
    this.cookieMaxAge = cookieMaxAge;
    this.limitEmail = limitEmail;
    this.limitIp = limitIp;
    this.dummyHash = encoder.encode("sin-usuario-" + Math.random());
  }

  // --- acceso -----------------------------------------------------------------------------

  /** Sesión vigente de un administrador; si no, SinSesion (→ login) o Prohibido (→ 403). */
  private Usuario admin(String cookie) {
    var s = sessions.vigente(cookie).orElseThrow(SinSesion::new);
    Usuario u = users.porId(s.usuarioId()).orElseThrow(SinSesion::new);
    if (!u.roles().contains("ADMINISTRADOR")) throw new Prohibido();
    return u;
  }

  @ExceptionHandler(SinSesion.class)
  String sinSesion() {
    return "redirect:/admin/login";
  }

  @ExceptionHandler(Prohibido.class)
  String prohibido(Model model, HttpServletResponse resp) {
    resp.setStatus(HttpStatus.FORBIDDEN.value());
    model.addAttribute("titulo", "Acceso denegado");
    model.addAttribute("mensaje", "Esta sección es solo para administradores del servicio.");
    return "error";
  }

  @GetMapping({"", "/"})
  String inicio() {
    return "redirect:/admin/usuarios";
  }

  @GetMapping("/login")
  String loginPage(@CookieValue(name = COOKIE, required = false) String tgc, Model model) {
    if (sessions.vigente(tgc).isPresent()) return "redirect:/admin/usuarios";
    model.addAttribute("error", null);
    return "admin/login";
  }

  @PostMapping("/login")
  String login(
      @RequestParam(required = false) String correo,
      @RequestParam(required = false) String password,
      Model model,
      HttpServletRequest req,
      HttpServletResponse resp) {
    String email = correo == null ? "" : correo.strip().toLowerCase();
    String origin = req.getRemoteAddr();
    model.addAttribute("error", null);
    if (!limiter.allow("login-correo", email, limitEmail)
        || !limiter.allow("login-ip", origin, limitIp)) {
      log.registrar(email, Resultado.BLOQUEADO, origin, null);
      resp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      resp.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(limiter.retryAfterSeconds()));
      model.addAttribute("error", "Demasiados intentos. Vuelve a intentarlo en un minuto.");
      return "admin/login";
    }
    Optional<Usuario> user = users.porCorreo(email);
    String hash = user.map(Usuario::passwordHash).orElse(dummyHash);
    boolean valid = password != null && password.length() <= 128 && encoder.matches(password, hash);
    if (!valid || user.isEmpty()) {
      log.registrar(email, Resultado.CREDENCIALES, origin, null);
      model.addAttribute("error", "Correo o contraseña incorrectos");
      return "admin/login";
    }
    Usuario u = user.get();
    if (!u.activo()) {
      log.registrar(email, Resultado.INACTIVO, origin, null);
      model.addAttribute("error", "Tu cuenta está desactivada. Consulta con el administrador.");
      return "admin/login";
    }
    if (!u.roles().contains("ADMINISTRADOR")) {
      log.registrar(email, Resultado.CREDENCIALES, origin, null);
      return prohibido(model, resp);
    }
    if (u.debeCambiarPassword()) {
      model.addAttribute(
          "error", "Debes cambiar tu contraseña: ingresa primero en /cas/login y cámbiala.");
      return "admin/login";
    }
    log.registrar(email, Resultado.OK, origin, null);
    resp.addHeader(HttpHeaders.SET_COOKIE, cookie(sessions.crear(u.id()), cookieMaxAge).toString());
    return "redirect:/admin/usuarios";
  }

  @PostMapping("/logout")
  String logout(
      @CookieValue(name = COOKIE, required = false) String tgc, HttpServletResponse resp) {
    sessions.cerrar(tgc);
    resp.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    return "redirect:/admin/login";
  }

  private ResponseCookie cookie(String value, Duration maxAge) {
    return ResponseCookie.from(COOKIE, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/admin")
        .maxAge(maxAge)
        .build();
  }

  // --- usuarios ---------------------------------------------------------------------------

  @GetMapping("/usuarios")
  String lista(
      @RequestParam(required = false, defaultValue = "") String q,
      @RequestParam(required = false, defaultValue = "0") int pagina,
      @RequestParam(required = false) String aviso,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model) {
    Usuario actor = admin(tgc);
    int total = users.contar(q);
    int paginas = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    int p = Math.min(Math.max(pagina, 0), paginas - 1);
    model.addAttribute("actor", actor);
    model.addAttribute("usuarios", users.buscar(q, PAGE_SIZE, p * PAGE_SIZE));
    model.addAttribute("q", q);
    model.addAttribute("pagina", p);
    model.addAttribute("paginas", paginas);
    model.addAttribute("total", total);
    model.addAttribute("aviso", aviso);
    return "admin/usuarios";
  }

  @GetMapping("/usuarios/nuevo")
  String nuevo(@CookieValue(name = COOKIE, required = false) String tgc, Model model) {
    admin(tgc);
    return formulario(model, null, null);
  }

  @GetMapping("/usuarios/{id}")
  String editar(
      @PathVariable UUID id,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model) {
    admin(tgc);
    Optional<Usuario> u = users.porId(id);
    if (u.isEmpty()) return "redirect:/admin/usuarios?aviso=El+usuario+no+existe.";
    return formulario(model, u.get(), null);
  }

  @PostMapping("/usuarios")
  String crear(
      @RequestParam(required = false) String correo,
      @RequestParam(required = false) String cedula,
      @RequestParam(required = false) String nombres,
      @RequestParam(required = false) List<String> roles,
      @RequestParam(required = false) String carreras,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp) {
    Usuario actor = admin(tgc);
    try {
      var creado =
          manageUsers.crear(
              actor.id(), correo, cedula, nombres, roles, ManageUsers.lista(carreras));
      model.addAttribute("temporal", creado.temporal());
      return formulario(model, creado.usuario(), null);
    } catch (IllegalArgumentException e) {
      resp.setStatus(HttpStatus.BAD_REQUEST.value());
      return formularioError(model, null, correo, cedula, nombres, roles, carreras, e.getMessage());
    }
  }

  @PostMapping("/usuarios/{id}")
  String guardar(
      @PathVariable UUID id,
      @RequestParam(required = false) String correo,
      @RequestParam(required = false) String cedula,
      @RequestParam(required = false) String nombres,
      @RequestParam(required = false) List<String> roles,
      @RequestParam(required = false) String carreras,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp) {
    Usuario actor = admin(tgc);
    try {
      Usuario u =
          manageUsers.editar(
              actor.id(), id, correo, cedula, nombres, roles, ManageUsers.lista(carreras));
      model.addAttribute("aviso", "Cambios guardados.");
      return formulario(model, u, null);
    } catch (IllegalArgumentException e) {
      resp.setStatus(HttpStatus.BAD_REQUEST.value());
      return formularioError(
          model,
          users.porId(id).orElse(null),
          correo,
          cedula,
          nombres,
          roles,
          carreras,
          e.getMessage());
    }
  }

  @PostMapping("/usuarios/{id}/estado")
  String estado(
      @PathVariable UUID id,
      @RequestParam boolean activo,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp) {
    Usuario actor = admin(tgc);
    try {
      manageUsers.fijarActivo(actor.id(), id, activo);
      model.addAttribute("aviso", activo ? "Usuario activado." : "Usuario desactivado.");
      return formulario(model, users.porId(id).orElseThrow(), null);
    } catch (IllegalArgumentException e) {
      resp.setStatus(HttpStatus.BAD_REQUEST.value());
      return formulario(model, users.porId(id).orElse(null), e.getMessage());
    }
  }

  @PostMapping("/usuarios/{id}/restablecer")
  String restablecer(
      @PathVariable UUID id,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp) {
    Usuario actor = admin(tgc);
    try {
      model.addAttribute("temporal", manageUsers.restablecer(actor.id(), id));
      return formulario(model, users.porId(id).orElseThrow(), null);
    } catch (IllegalArgumentException e) {
      resp.setStatus(HttpStatus.BAD_REQUEST.value());
      return formulario(model, users.porId(id).orElse(null), e.getMessage());
    }
  }

  private String formulario(Model model, Usuario u, String error) {
    model.addAttribute("u", u);
    model.addAttribute("roles", ManageUsers.ROLES);
    model.addAttribute("rolesElegidos", u == null ? List.of() : u.roles());
    model.addAttribute("carreras", u == null ? "" : String.join(", ", u.carreras()));
    model.addAttribute("error", error);
    return "admin/usuario";
  }

  /** Devuelve lo que el administrador escribió para que no lo pierda. */
  private String formularioError(
      Model model,
      Usuario u,
      String correo,
      String cedula,
      String nombres,
      List<String> roles,
      String carreras,
      String error) {
    formulario(model, u, error);
    model.addAttribute("correoValor", correo);
    model.addAttribute("cedulaValor", cedula);
    model.addAttribute("nombresValor", nombres);
    model.addAttribute("rolesElegidos", roles == null ? List.of() : roles);
    model.addAttribute("carreras", carreras == null ? "" : carreras);
    return "admin/usuario";
  }

  // --- importación CSV --------------------------------------------------------------------

  @GetMapping("/importar")
  String importarPage(@CookieValue(name = COOKIE, required = false) String tgc) {
    admin(tgc);
    return "admin/importar";
  }

  @PostMapping("/importar")
  String importar(
      @RequestParam(required = false) MultipartFile archivo,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp)
      throws IOException {
    Usuario actor = admin(tgc);
    if (archivo == null || archivo.isEmpty()) {
      resp.setStatus(HttpStatus.BAD_REQUEST.value());
      model.addAttribute("error", "Selecciona un archivo CSV.");
      return "admin/importar";
    }
    try {
      model.addAttribute("informe", csv.importar(actor.id(), archivo.getBytes()));
    } catch (CsvUserImport.ArchivoInvalido e) {
      resp.setStatus(HttpStatus.BAD_REQUEST.value());
      model.addAttribute("error", e.getMessage());
    }
    return "admin/importar";
  }

  // --- servicios registrados --------------------------------------------------------------

  @GetMapping("/servicios")
  String servicios(@CookieValue(name = COOKIE, required = false) String tgc, Model model) {
    admin(tgc);
    return serviciosView(model, null, null, null);
  }

  @PostMapping("/servicios")
  String crearServicio(
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) String retornos,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp) {
    Usuario actor = admin(tgc);
    try {
      String key = manageServices.crear(actor.id(), nombre, retornos);
      return serviciosView(model, null, nombre, key);
    } catch (IllegalArgumentException e) {
      resp.setStatus(HttpStatus.BAD_REQUEST.value());
      return serviciosView(model, e.getMessage(), null, null);
    }
  }

  @PostMapping("/servicios/{id}")
  String editarServicio(
      @PathVariable UUID id,
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) String retornos,
      @RequestParam(required = false, defaultValue = "false") boolean activo,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp) {
    Usuario actor = admin(tgc);
    try {
      manageServices.editar(actor.id(), id, nombre, retornos, activo);
      model.addAttribute("aviso", "Servicio guardado.");
      return serviciosView(model, null, null, null);
    } catch (IllegalArgumentException e) {
      resp.setStatus(HttpStatus.BAD_REQUEST.value());
      return serviciosView(model, e.getMessage(), null, null);
    }
  }

  @PostMapping("/servicios/{id}/clave")
  String regenerarClave(
      @PathVariable UUID id,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp) {
    Usuario actor = admin(tgc);
    try {
      String key = manageServices.regenerarClave(actor.id(), id);
      String nombre =
          registry.todos().stream()
              .filter(s -> s.id().equals(id))
              .map(ServiceRegistry.Servicio::nombre)
              .findFirst()
              .orElse("");
      return serviciosView(model, null, nombre, key);
    } catch (IllegalArgumentException e) {
      resp.setStatus(HttpStatus.BAD_REQUEST.value());
      return serviciosView(model, e.getMessage(), null, null);
    }
  }

  private String serviciosView(Model model, String error, String nombreClave, String clave) {
    model.addAttribute("servicios", registry.todos());
    model.addAttribute("error", error);
    model.addAttribute("nombreClave", nombreClave);
    model.addAttribute("clave", clave);
    return "admin/servicios";
  }
}
