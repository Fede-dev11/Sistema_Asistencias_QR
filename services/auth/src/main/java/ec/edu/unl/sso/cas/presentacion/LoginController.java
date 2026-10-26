package ec.edu.unl.sso.cas.presentacion;

import ec.edu.unl.sso.cas.aplicacion.SsoSessions;
import ec.edu.unl.sso.cas.aplicacion.SsoSessions.Sesion;
import ec.edu.unl.sso.cas.aplicacion.Tickets;
import ec.edu.unl.sso.cas.persistencia.LoginLog;
import ec.edu.unl.sso.cas.persistencia.LoginLog.Resultado;
import ec.edu.unl.sso.compartido.RateLimiter;
import ec.edu.unl.sso.servicios.aplicacion.ServiceMatcher;
import ec.edu.unl.sso.servicios.persistencia.ServiceRegistry.Servicio;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

/** Inicio de sesión (GET/POST /cas/login) y cambio de contraseña propio (/cas/password). */
@Controller
@RequestMapping("/cas")
class LoginController {
  static final String COOKIE = "CASTGC";
  static final String GENERIC_ERROR = "Correo o contraseña incorrectos";

  private final UserRepository users;
  private final SsoSessions sessions;
  private final Tickets tickets;
  private final ServiceMatcher matcher;
  private final PasswordEncoder encoder;
  private final RateLimiter limiter;
  private final LoginLog log;
  private final boolean secure;
  private final Duration cookieMaxAge;
  private final int limitEmail;
  private final int limitIp;

  /** Contra enumeración por tiempo: se compara contra un hash aunque el correo no exista. */
  private final String dummyHash;

  LoginController(
      UserRepository users,
      SsoSessions sessions,
      Tickets tickets,
      ServiceMatcher matcher,
      PasswordEncoder encoder,
      RateLimiter limiter,
      LoginLog log,
      @Value("${sso.cookie-secure}") boolean secure,
      @Value("${sso.session-max}") Duration cookieMaxAge,
      @Value("${sso.login-limit-email}") int limitEmail,
      @Value("${sso.login-limit-ip}") int limitIp) {
    this.users = users;
    this.sessions = sessions;
    this.tickets = tickets;
    this.matcher = matcher;
    this.encoder = encoder;
    this.limiter = limiter;
    this.log = log;
    this.secure = secure;
    this.cookieMaxAge = cookieMaxAge;
    this.limitEmail = limitEmail;
    this.limitIp = limitIp;
    this.dummyHash = encoder.encode("sin-usuario-" + Math.random());
  }

  @GetMapping("/login")
  Object loginPage(
      @RequestParam(required = false) String service,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp) {
    Optional<Servicio> svc = resolve(service);
    if (svc == null) return unregistered(model, resp);
    Optional<Sesion> session = sessions.vigente(tgc);
    if (session.isPresent()) {
      Usuario user = users.porId(session.get().usuarioId()).orElseThrow();
      if (user.debeCambiarPassword()) return redirect(passwordUrl(service));
      if (svc.isPresent()) return ticketRedirect(session.get(), svc.get(), service);
      return info(model, "Sesión iniciada", "Ya tienes una sesión universitaria activa.");
    }
    return loginView(model, service, null);
  }

  @PostMapping("/login")
  Object login(
      @RequestParam(required = false) String correo,
      @RequestParam(required = false) String password,
      @RequestParam(required = false) String service,
      Model model,
      HttpServletRequest req,
      HttpServletResponse resp) {
    Optional<Servicio> svc = resolve(service);
    if (svc == null) return unregistered(model, resp);
    String email = correo == null ? "" : correo.strip().toLowerCase();
    String origin = req.getRemoteAddr();
    java.util.UUID serviceId = svc.map(Servicio::id).orElse(null);
    if (!limiter.allow("login-correo", email, limitEmail)
        || !limiter.allow("login-ip", origin, limitIp)) {
      log.registrar(email, Resultado.BLOQUEADO, origin, serviceId);
      resp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      resp.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(limiter.retryAfterSeconds()));
      return loginView(model, service, "Demasiados intentos. Vuelve a intentarlo en un minuto.");
    }
    Optional<Usuario> user = users.porCorreo(email);
    boolean valid =
        matches(password, user.map(Usuario::passwordHash).orElse(dummyHash)) && user.isPresent();
    if (!valid) {
      log.registrar(email, Resultado.CREDENCIALES, origin, serviceId);
      return loginView(model, service, GENERIC_ERROR);
    }
    if (!user.get().activo()) {
      log.registrar(email, Resultado.INACTIVO, origin, serviceId);
      return loginView(
          model, service, "Tu cuenta está desactivada. Consulta con el administrador.");
    }
    log.registrar(email, Resultado.OK, origin, serviceId);
    String secret = sessions.crear(user.get().id());
    Sesion session = sessions.vigente(secret).orElseThrow();
    resp.addHeader(HttpHeaders.SET_COOKIE, cookie(secret, cookieMaxAge).toString());
    if (user.get().debeCambiarPassword()) return redirect(passwordUrl(service));
    if (svc.isEmpty()) return info(model, "Sesión iniciada", "Ya puedes cerrar esta ventana.");
    return ticketRedirect(session, svc.get(), service);
  }

  @GetMapping("/password")
  Object passwordPage(
      @RequestParam(required = false) String service,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp) {
    Optional<Servicio> svc = resolve(service);
    if (svc == null) return unregistered(model, resp);
    Optional<Sesion> session = sessions.vigente(tgc);
    if (session.isEmpty()) return redirect(loginUrl(service));
    return passwordView(model, service, null, users.porId(session.get().usuarioId()).orElseThrow());
  }

  @PostMapping("/password")
  Object changePassword(
      @RequestParam(required = false) String actual,
      @RequestParam(required = false) String nueva,
      @RequestParam(required = false) String confirmar,
      @RequestParam(required = false) String service,
      @CookieValue(name = COOKIE, required = false) String tgc,
      Model model,
      HttpServletResponse resp) {
    Optional<Servicio> svc = resolve(service);
    if (svc == null) return unregistered(model, resp);
    Optional<Sesion> session = sessions.vigente(tgc);
    if (session.isEmpty()) return redirect(loginUrl(service));
    Usuario user = users.porId(session.get().usuarioId()).orElseThrow();
    String error = null;
    if (!limiter.allow("password-correo", user.correo(), limitEmail)) {
      resp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      resp.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(limiter.retryAfterSeconds()));
      error = "Demasiados intentos. Vuelve a intentarlo en un minuto.";
    } else if (!matches(actual, user.passwordHash())) {
      error = "La contraseña actual no es correcta.";
    } else if (nueva == null || nueva.length() < 12 || nueva.length() > 128) {
      error = "La nueva contraseña debe tener entre 12 y 128 caracteres.";
    } else if (nueva.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
      error = "La nueva contraseña no puede superar 72 bytes.";
    } else if (nueva.equals(actual)) {
      error = "La nueva contraseña debe ser distinta de la actual.";
    } else if (!nueva.equals(confirmar)) {
      error = "La confirmación no coincide.";
    }
    if (error != null) return passwordView(model, service, error, user);
    users.cambiarPassword(user.id(), encoder.encode(nueva));
    sessions.cerrarOtras(user.id(), session.get().id());
    if (svc.isPresent()) return ticketRedirect(session.get(), svc.get(), service);
    return info(model, "Contraseña actualizada", "Tu contraseña se cambió correctamente.");
  }

  // --- utilidades -------------------------------------------------------------------------

  /** null = service presente pero no registrado; vacío = sin service. */
  private Optional<Servicio> resolve(String service) {
    if (service == null || service.isBlank()) return Optional.empty();
    Optional<Servicio> svc = matcher.match(service);
    return svc.isPresent() ? svc : null;
  }

  private boolean matches(String raw, String hash) {
    if (raw == null || raw.length() > 128) {
      encoder.matches("x", hash);
      return false;
    }
    try {
      return encoder.matches(raw, hash);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private String unregistered(Model model, HttpServletResponse resp) {
    resp.setStatus(HttpStatus.BAD_REQUEST.value());
    model.addAttribute("titulo", "Servicio no registrado");
    model.addAttribute(
        "mensaje", "La dirección de retorno solicitada no corresponde a un servicio registrado.");
    return "error";
  }

  private String info(Model model, String titulo, String mensaje) {
    model.addAttribute("titulo", titulo);
    model.addAttribute("mensaje", mensaje);
    return "error";
  }

  private String loginView(Model model, String service, String error) {
    model.addAttribute("service", service);
    model.addAttribute("error", error);
    return "login";
  }

  private String passwordView(Model model, String service, String error, Usuario user) {
    model.addAttribute("service", service);
    model.addAttribute("error", error);
    model.addAttribute("obligatorio", user.debeCambiarPassword());
    return "password";
  }

  private ResponseEntity<Void> ticketRedirect(Sesion session, Servicio svc, String service) {
    return redirect(location(session, svc, service));
  }

  private String location(Sesion session, Servicio svc, String service) {
    String ticket = tickets.emitir(session.id(), svc.id(), service);
    return service + (service.contains("?") ? "&" : "?") + "ticket=" + ticket;
  }

  private static ResponseEntity<Void> redirect(String url) {
    return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, url).build();
  }

  private static String passwordUrl(String service) {
    return "/cas/password" + serviceQuery(service);
  }

  private static String loginUrl(String service) {
    return "/cas/login" + serviceQuery(service);
  }

  private static String serviceQuery(String service) {
    return service == null || service.isBlank()
        ? ""
        : "?service=" + UriUtils.encodeQueryParam(service, java.nio.charset.StandardCharsets.UTF_8);
  }

  /** CASTGC: HttpOnly, SameSite=Lax, Path=/cas, Secure según sso.cookie-secure. */
  ResponseCookie cookie(String value, Duration maxAge) {
    return ResponseCookie.from(COOKIE, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/cas")
        .maxAge(maxAge)
        .build();
  }
}
