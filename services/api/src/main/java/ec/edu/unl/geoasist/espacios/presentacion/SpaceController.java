package ec.edu.unl.geoasist.espacios.presentacion;

import ec.edu.unl.geoasist.espacios.aplicacion.GeoEvidence;
import ec.edu.unl.geoasist.espacios.aplicacion.ManageSpaces;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

  public record GeoEvidenceInput(
      String referencia,
      Instant realizada_en,
      BigDecimal falsos_positivos,
      BigDecimal falsos_negativos) {}

  private final ManageSpaces service;
  private final GeoEvidence evidence;

  public SpaceController(ManageSpaces service, GeoEvidence evidence) {
    this.service = service;
    this.evidence = evidence;
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

  @PostMapping("/{spaceId}/geo-evidence")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createEvidence(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID spaceId,
      @RequestBody GeoEvidenceInput input) {
    return evidence.create(
        user,
        spaceId,
        new GeoEvidence.Command(
            input.referencia(),
            input.realizada_en(),
            input.falsos_positivos(),
            input.falsos_negativos()));
  }

  @GetMapping("/{spaceId}/geo-evidence")
  public List<Map<String, Object>> listEvidence(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID spaceId) {
    return evidence.list(user, spaceId);
  }
}
