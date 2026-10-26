package ec.edu.unl.geoasist.academico.presentacion;

import ec.edu.unl.geoasist.academico.aplicacion.ManageCareersAndParallels;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class TeacherCatalogController {
  private final ManageCareersAndParallels service;

  public TeacherCatalogController(ManageCareersAndParallels service) {
    this.service = service;
  }

  @GetMapping("/parallels")
  public Map<String, Object> parallels(@AuthenticationPrincipal UserSession user) {
    return service.myParallels(user);
  }
}
