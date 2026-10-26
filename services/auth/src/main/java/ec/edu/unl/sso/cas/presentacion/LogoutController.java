package ec.edu.unl.sso.cas.presentacion;

import ec.edu.unl.sso.cas.aplicacion.SsoSessions;
import ec.edu.unl.sso.servicios.aplicacion.ServiceMatcher;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** GET /cas/logout: cierra el TGT, borra la cookie y solo redirige a servicios registrados. */
@Controller
class LogoutController {
  private final SsoSessions sessions;
  private final ServiceMatcher matcher;
  private final boolean secure;

  LogoutController(
      SsoSessions sessions, ServiceMatcher matcher, @Value("${sso.cookie-secure}") boolean secure) {
    this.sessions = sessions;
    this.matcher = matcher;
    this.secure = secure;
  }

  @GetMapping("/cas/logout")
  Object logout(
      @RequestParam(required = false) String service,
      @CookieValue(name = LoginController.COOKIE, required = false) String tgc,
      Model model,
      jakarta.servlet.http.HttpServletResponse resp) {
    sessions.cerrar(tgc);
    String clear =
        ResponseCookie.from(LoginController.COOKIE, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/cas")
            .maxAge(Duration.ZERO)
            .build()
            .toString();
    if (service != null && matcher.match(service).isPresent()) {
      return ResponseEntity.status(HttpStatus.FOUND)
          .header(HttpHeaders.SET_COOKIE, clear)
          .header(HttpHeaders.LOCATION, service)
          .build();
    }
    model.addAttribute("titulo", "Sesión cerrada");
    model.addAttribute("mensaje", "Cerraste tu sesión universitaria.");
    resp.addHeader(HttpHeaders.SET_COOKIE, clear);
    return "error";
  }
}
