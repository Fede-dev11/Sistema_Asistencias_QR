package ec.edu.unl.geoasist.asistencia.presentacion;

import ec.edu.unl.geoasist.asistencia.aplicacion.ResolveAttendanceReview;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attempts")
public class ReviewController {
  public record Input(@NotNull String decision, @NotBlank @Size(max = 2000) String razon) {}

  private final ResolveAttendanceReview service;

  public ReviewController(ResolveAttendanceReview service) {
    this.service = service;
  }

  @PostMapping("/{attemptId}/review")
  public ResponseEntity<Map<String, Object>> review(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID attemptId,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody Input input) {
    if (!Set.of("APROBAR", "RECHAZAR").contains(input.decision()))
      throw ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure.invalid();
    var receipt =
        service.resolve(
            user,
            attemptId,
            new ResolveAttendanceReview.Command(input.decision(), input.razon()),
            key,
            input);
    return AttendanceController.response(receipt);
  }
}
