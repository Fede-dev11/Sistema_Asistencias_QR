package ec.edu.unl.sso.usuarios.presentacion;

import ec.edu.unl.sso.servicios.persistencia.ServiceRegistry;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/v1/users/status: estado de una cuenta, con la clave (X-Service-Key) de un servicio. */
@RestController
class StatusController {
  private final ServiceRegistry services;
  private final UserRepository users;

  StatusController(ServiceRegistry services, UserRepository users) {
    this.services = services;
    this.users = users;
  }

  @GetMapping("/api/v1/users/status")
  ResponseEntity<Map<String, Boolean>> status(
      @RequestParam String correo,
      @RequestHeader(name = "X-Service-Key", required = false) String key) {
    if (key == null || key.isBlank() || services.idPorClave(key).isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return users
        .porCorreo(correo)
        .map(u -> ResponseEntity.ok(Map.of("activo", u.activo())))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
