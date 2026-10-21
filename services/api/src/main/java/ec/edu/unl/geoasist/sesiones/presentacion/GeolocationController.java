package ec.edu.unl.geoasist.sesiones.presentacion;

import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.sesiones.aplicacion.ConfigureSessionGeolocation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
public class GeolocationController {
  public record Input(
      @NotNull Boolean habilitada,
      Double radio_m,
      Double precision_max_m,
      Integer antiguedad_max_s) {}

  private final ConfigureSessionGeolocation service;

  public GeolocationController(ConfigureSessionGeolocation service) {
    this.service = service;
  }

  @PutMapping("/{sessionId}/geolocation")
  public Map<String, Object> configure(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID sessionId,
      @Valid @RequestBody Input input) {
    return service.configure(
        user,
        sessionId,
        new ConfigureSessionGeolocation.Command(
            input.habilitada(),
            input.radio_m(),
            input.precision_max_m(),
            input.antiguedad_max_s()));
  }
}
