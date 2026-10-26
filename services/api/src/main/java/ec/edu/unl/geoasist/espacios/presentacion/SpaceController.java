package ec.edu.unl.geoasist.espacios.presentacion;

import ec.edu.unl.geoasist.espacios.aplicacion.ManageSpaces;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/spaces")
public class SpaceController {
  public record CreateInput(
      @NotBlank @Size(max = 40) String codigo,
      @NotBlank @Size(max = 200) String nombre,
      String tipo,
      Double latitud,
      Double longitud,
      Double radio_defecto_m,
      Double precision_defecto_m) {}

  public record PatchInput(
      @Size(max = 200) String nombre,
      String tipo,
      Double latitud,
      Double longitud,
      Double radio_defecto_m,
      Double precision_defecto_m) {}

  private final ManageSpaces service;

  public SpaceController(ManageSpaces service) {
    this.service = service;
  }

  @GetMapping
  public Map<String, Object> search(
      @AuthenticationPrincipal UserSession user,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.search(user, q, page, size);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> create(
      @AuthenticationPrincipal UserSession user, @Valid @RequestBody CreateInput i) {
    return service.create(
        user,
        new ManageSpaces.Command(
            i.codigo(),
            i.nombre(),
            i.tipo(),
            i.latitud(),
            i.longitud(),
            i.radio_defecto_m(),
            i.precision_defecto_m()));
  }

  @PatchMapping("/{spaceId}")
  public Map<String, Object> update(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID spaceId,
      @Valid @RequestBody PatchInput i) {
    return service.update(
        user,
        spaceId,
        new ManageSpaces.Command(
            null,
            i.nombre(),
            i.tipo(),
            i.latitud(),
            i.longitud(),
            i.radio_defecto_m(),
            i.precision_defecto_m()));
  }
}
