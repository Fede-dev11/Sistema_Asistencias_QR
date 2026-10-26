package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.espacios.persistencia.SpaceRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Valida espacio existente y que un método con GPS tenga un espacio con ubicación. */
@Component
public class SpaceMethodRules {
  public static final Set<String> METHODS = Set.of("QR_Y_GPS", "SOLO_QR", "SOLO_GPS", "NINGUNO");
  private final SpaceRepository spaces;

  public SpaceMethodRules(SpaceRepository spaces) {
    this.spaces = spaces;
  }

  public void check(UUID space, String method) {
    if (method != null && !METHODS.contains(method)) throw ApiFailure.invalid();
    if (space == null) return;
    var row = spaces.find(space).orElseThrow(ApiFailure::invalid);
    if (method != null && method.contains("GPS") && row.latitude() == null)
      throw new ApiFailure(
          409, "ESPACIO_SIN_UBICACION", "El espacio no tiene ubicación; elige un método sin GPS.");
  }
}
