package ec.edu.unl.geoasist.identidades.presentacion;

import ec.edu.unl.geoasist.identidades.aplicacion.*;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthenticationService auth;
  private final RequestBudget budget;

  public AuthController(AuthenticationService auth, RequestBudget budget) {
    this.auth = auth;
    this.budget = budget;
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
