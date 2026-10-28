package ec.edu.unl.geoasist.identidades.presentacion;

import ec.edu.unl.geoasist.identidades.aplicacion.AccountSelfService;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class AccountController {
  private final AccountSelfService service;

  public AccountController(AccountSelfService service) {
    this.service = service;
  }

  @GetMapping
  public Map<String, Object> me(@AuthenticationPrincipal UserSession user) {
    return service.profile(user);
  }
}
