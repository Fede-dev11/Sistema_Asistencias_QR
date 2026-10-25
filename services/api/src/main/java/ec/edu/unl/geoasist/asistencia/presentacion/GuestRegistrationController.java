package ec.edu.unl.geoasist.asistencia.presentacion;

import ec.edu.unl.geoasist.asistencia.aplicacion.*;
import ec.edu.unl.geoasist.compartido.aplicacion.*;
import ec.edu.unl.geoasist.qr.aplicacion.IssueSessionQr;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/guest-registrations")
public class GuestRegistrationController {
  private final StartGuestRegistration opening;
  private final SubmitGuestAttendance submit;
  private final AttendanceRequestLimits limits;
  private final IssueSessionQr qr;

  public GuestRegistrationController(
      StartGuestRegistration opening,
      SubmitGuestAttendance submit,
      AttendanceRequestLimits limits,
      IssueSessionQr qr) {
    this.opening = opening;
    this.submit = submit;
    this.limits = limits;
    this.qr = qr;
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> open(
      @Valid @RequestBody AttendanceInputs.GuestStart body, HttpServletRequest request) {
    UUID session = null;
    try {
      session = qr.lookup(body.qr_token()).session();
    } catch (ApiFailure e) {
      limits.opening(request.getRemoteAddr(), null);
      throw e;
    }
    limits.opening(request.getRemoteAddr(), session);
    return ResponseEntity.status(201)
        .body(opening.open(body.qr_token(), ServerTime.received(request)));
  }

  @PostMapping("/submit")
  public ResponseEntity<Map<String, Object>> submit(
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody AttendanceInputs.GuestSubmit body,
      HttpServletRequest request) {
    limits.submission(request.getRemoteAddr(), body.registro_invitado_token());
    return AttendanceController.response(
        submit.submit(
            body.registro_invitado_token(),
            body.cedula(),
            body.dispositivo_id(),
            body.ubicacion() == null ? null : body.ubicacion().measurement(),
            key,
            body,
            ServerTime.received(request)));
  }
}
