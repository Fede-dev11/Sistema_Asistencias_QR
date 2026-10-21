package ec.edu.unl.geoasist.reportes.presentacion;

import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.reportes.aplicacion.ExportAttendancesCsv;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
public class AttendanceExportController {
  private final ExportAttendancesCsv service;

  public AttendanceExportController(ExportAttendancesCsv service) {
    this.service = service;
  }

  @GetMapping(value = "/{sessionId}/attendances/export", produces = "text/csv")
  public ResponseEntity<byte[]> export(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID sessionId,
      @RequestParam(required = false) String estado) {
    byte[] body = service.export(user, sessionId, estado).getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"asistencias.csv\"")
        .body(body);
  }
}
