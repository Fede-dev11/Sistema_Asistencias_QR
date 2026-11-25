package ec.edu.unl.geoasist.asistencia.presentacion;

import ec.edu.unl.geoasist.asistencia.aplicacion.RecordAuthenticatedAttendance;
import ec.edu.unl.geoasist.compartido.aplicacion.*;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.qr.aplicacion.IssueSessionQr;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {
  private final RecordAuthenticatedAttendance service;
  private final AttendanceRequestLimits limits;
  private final IssueSessionQr qr;

  public AttendanceController(
      RecordAuthenticatedAttendance service, AttendanceRequestLimits limits, IssueSessionQr qr) {
    this.service = service;
    this.limits = limits;
    this.qr = qr;
  }

  @PostMapping("/attempts")
  public ResponseEntity<Map<String, Object>> record(
      @AuthenticationPrincipal UserSession user,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody AttendanceInputs.AccountAttempt body,
      HttpServletRequest request) {
    UUID session = null;
    try {
      if (body.sesion_id() == null && body.qr_token() == null) throw ApiFailure.invalid();
      session = body.sesion_id() != null ? body.sesion_id() : qr.lookup(body.qr_token()).session();
    } catch (ApiFailure e) {
      limits.account(user.personId(), null);
      throw e;
    }
    limits.account(user.personId(), session);
    var receipt =
        service.record(
            user,
            body.qr_token(),
            body.sesion_id(),
            body.dispositivo_id(),
            body.ubicacion() == null ? null : body.ubicacion().measurement(),
            key,
            body,
            ServerTime.received(request));
    return response(receipt);
  }

  static ResponseEntity<Map<String, Object>> response(IdempotencyService.Receipt receipt) {
    var response = ResponseEntity.status(receipt.status());
    if (receipt.status() >= 400) response.contentType(MediaType.APPLICATION_PROBLEM_JSON);
    return response.body(receipt.body());
  }
}
