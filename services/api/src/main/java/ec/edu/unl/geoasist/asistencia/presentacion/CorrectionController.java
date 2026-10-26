package ec.edu.unl.geoasist.asistencia.presentacion;

import ec.edu.unl.geoasist.asistencia.aplicacion.Justifications;
import ec.edu.unl.geoasist.asistencia.aplicacion.ManualAttendance;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Asistencia manual, justificaciones y su bandeja (003 US2). */
@RestController
@RequestMapping("/api/v1")
public class CorrectionController {
  public record ManualInput(@NotNull UUID persona_id, @NotBlank @Size(max = 2000) String razon) {}

  public record ReasonInput(@NotBlank @Size(max = 2000) String razon) {}

  public record MotiveInput(@NotBlank @Size(max = 2000) String motivo) {}

  public record DecisionInput(@NotNull String decision, @NotBlank @Size(max = 2000) String razon) {}

  private final ManualAttendance manual;
  private final Justifications justifications;

  public CorrectionController(ManualAttendance manual, Justifications justifications) {
    this.manual = manual;
    this.justifications = justifications;
  }

  @PostMapping("/sessions/{sessionId}/manual-attendances")
  public ResponseEntity<Map<String, Object>> manual(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID sessionId,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody ManualInput input) {
    return AttendanceController.response(
        manual.register(
            user,
            sessionId,
            new ManualAttendance.Command(input.persona_id(), input.razon()),
            key,
            input));
  }

  @PostMapping("/sessions/{sessionId}/absences/{personId}/justify")
  public ResponseEntity<Map<String, Object>> justify(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID sessionId,
      @PathVariable UUID personId,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody ReasonInput input) {
    return AttendanceController.response(
        justifications.justify(user, sessionId, personId, input.razon(), key, input));
  }

  @PostMapping("/me/absences/{sessionId}/justification-requests")
  public ResponseEntity<Map<String, Object>> request(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID sessionId,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody MotiveInput input) {
    user.requireAnyRole("ESTUDIANTE");
    return AttendanceController.response(
        justifications.request(user, sessionId, input.motivo(), key, input));
  }

  @GetMapping("/activities/{activityId}/justification-requests")
  public Map<String, Object> list(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @RequestParam(required = false) String estado,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return justifications.list(user, activityId, estado, page, size);
  }

  @PostMapping("/justification-requests/{id}/decision")
  public ResponseEntity<Map<String, Object>> decide(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID id,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody DecisionInput input) {
    if (!Set.of("APROBAR", "RECHAZAR").contains(input.decision())) throw ApiFailure.invalid();
    return AttendanceController.response(
        justifications.decide(user, id, input.decision(), input.razon(), key, input));
  }
}
