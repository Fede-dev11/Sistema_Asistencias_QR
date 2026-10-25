package ec.edu.unl.geoasist.identidades.presentacion;

import ec.edu.unl.geoasist.identidades.aplicacion.AccountSelfService;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class AccountController {
  private final AccountSelfService service;

  public AccountController(AccountSelfService service) {
    this.service = service;
  }

  public record PasswordChange(String actual, String nueva) {
    @Override
    public String toString() {
      return "PasswordChange[credenciales omitidas]";
    }
  }

  @GetMapping
  public Map<String, Object> me(@AuthenticationPrincipal UserSession user) {
    return service.profile(user);
  }

  @PutMapping("/password")
  public ResponseEntity<Void> password(
      @AuthenticationPrincipal UserSession user, @RequestBody PasswordChange body) {
    service.changePassword(user, body.actual(), body.nueva());
    return ResponseEntity.noContent().build();
  }
}
