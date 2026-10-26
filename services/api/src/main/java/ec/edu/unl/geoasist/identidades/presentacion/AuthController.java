package ec.edu.unl.geoasist.identidades.presentacion;

import ec.edu.unl.geoasist.identidades.aplicacion.*;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthenticationService auth;
  private final RequestBudget budget;
  private final String publicUrl;
  private final String webService;
  private final String mobileServicePrefix;

  public AuthController(
      AuthenticationService auth,
      RequestBudget budget,
      @Value("${geoasist.sso.public-url}") String publicUrl,
      @Value("${geoasist.sso.web-service}") String webService,
      @Value("${geoasist.sso.mobile-service-prefix}") String mobileServicePrefix) {
    this.auth = auth;
    this.budget = budget;
    this.publicUrl = publicUrl.replaceAll("/+$", "");
    this.webService = webService;
    this.mobileServicePrefix = mobileServicePrefix;
  }

  public record CasLogin(
      @NotBlank @Size(max = 256) String ticket, @NotBlank @Size(max = 2048) String service) {
    public String toString() {
      return "CasLogin[ticket omitido, service=" + service + "]";
    }
  }

  @PostMapping("/cas")
  public Map<String, Object> cas(@Valid @RequestBody CasLogin body) {
    return auth.cas(body.ticket(), body.service());
  }

  @GetMapping("/sso-config")
  public Map<String, String> ssoConfig() {
    return Map.of(
        "login_url",
        publicUrl + "/cas/login",
        "logout_url",
        publicUrl + "/cas/logout",
        "web_service",
        webService,
        "mobile_service_prefix",
        mobileServicePrefix);
  }

  public record Login(@NotBlank @Email String correo, @NotBlank String password) {
    @Override
    public String toString() {
      return "Login[credenciales omitidas]";
    }
  }

  @PostMapping("/login")
  public Map<String, Object> login(@Valid @RequestBody Login body, HttpServletRequest req) {
    budget.check("login-ip:" + req.getRemoteAddr(), 300);
    budget.check("login-account:" + body.correo().toLowerCase(java.util.Locale.ROOT), 5);
    return auth.login(body.correo(), body.password());
  }

  @PostMapping("/renew")
  public Map<String, Object> renew(@AuthenticationPrincipal UserSession user) {
    return auth.renew(user);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@AuthenticationPrincipal UserSession user) {
    auth.logout(user);
    return ResponseEntity.noContent().build();
  }
}
