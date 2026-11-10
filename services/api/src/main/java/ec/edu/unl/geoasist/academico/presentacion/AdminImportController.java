package ec.edu.unl.geoasist.academico.presentacion;

import ec.edu.unl.geoasist.academico.aplicacion.CsvImport;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/imports")
public class AdminImportController {
  private final CsvImport csv;

  public AdminImportController(CsvImport csv) {
    this.csv = csv;
  }

  @PostMapping(value = "/{kind}", consumes = "multipart/form-data")
  public CsvImport.ImportReport run(
      @AuthenticationPrincipal UserSession user,
      @PathVariable String kind,
      @RequestParam("file") MultipartFile file)
      throws java.io.IOException {
    if (file.isEmpty()) throw ApiFailure.invalid();
    return csv.run(user, kind, file.getBytes());
  }
}
