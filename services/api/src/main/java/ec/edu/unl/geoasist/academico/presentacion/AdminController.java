package ec.edu.unl.geoasist.academico.presentacion;

import ec.edu.unl.geoasist.academico.aplicacion.*;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
  private final ManagePeopleAndAccounts people;
  private final ManageCareersAndParallels academic;

  public AdminController(ManagePeopleAndAccounts people, ManageCareersAndParallels academic) {
    this.people = people;
    this.academic = academic;
  }

  public record PersonBody(@NotBlank String cedula, @NotBlank String nombres) {
    @Override
    public String toString() {
      return "PersonBody[datos omitidos]";
    }
  }

  public record NameBody(@NotBlank String nombres) {}

  public record AccountBody(
      @NotNull UUID persona_id,
      @NotBlank String correo,
      @NotBlank String password,
      @NotNull List<String> roles) {
    @Override
    public String toString() {
      return "AccountBody[datos omitidos]";
    }
  }

  public record AccountPatch(List<String> roles, Boolean activa, String password) {
    @Override
    public String toString() {
      return "AccountPatch[datos omitidos]";
    }
  }

  public record CareerBody(@NotBlank String codigo, @NotBlank String nombre) {}

  public record CareerPatch(@NotBlank String nombre) {}

  public record ParallelBody(@NotBlank String codigo) {}

  public record Cedulas(@NotNull List<String> cedulas) {}

  public record Correos(@NotNull List<String> correos) {}

  private static ResponseEntity<Map<String, Object>> reply(Receipt r) {
    return ResponseEntity.status(r.status()).body(r.body());
  }

  @GetMapping("/people")
  public Map<String, Object> people(
      @AuthenticationPrincipal UserSession user,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return people.list(user, q, page, size);
  }

  @PostMapping("/people")
  public ResponseEntity<Map<String, Object>> createPerson(
      @AuthenticationPrincipal UserSession user,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody PersonBody body) {
    return reply(people.createPerson(user, key, body.cedula(), body.nombres()));
  }

  @GetMapping("/people/{personId}")
  public Map<String, Object> person(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID personId) {
    return people.person(user, personId);
  }

  @PatchMapping("/people/{personId}")
  public Map<String, Object> rename(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID personId,
      @Valid @RequestBody NameBody body) {
    return people.rename(user, personId, body.nombres());
  }

  @PostMapping("/accounts")
  public ResponseEntity<Map<String, Object>> createAccount(
      @AuthenticationPrincipal UserSession user,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody AccountBody body) {
    return reply(
        people.createAccount(
            user, key, body.persona_id(), body.correo(), body.password(), body.roles()));
  }

  @PatchMapping("/accounts/{accountId}")
  public Map<String, Object> updateAccount(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID accountId,
      @RequestBody AccountPatch body) {
    return people.updateAccount(user, accountId, body.roles(), body.activa(), body.password());
  }

  @PostMapping("/accounts/{accountId}/password-reset")
  public Map<String, Object> resetPassword(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID accountId) {
    return people.resetPassword(user, accountId);
  }

  @GetMapping("/careers")
  public Map<String, Object> careers(@AuthenticationPrincipal UserSession user) {
    return academic.careers(user);
  }

  @PostMapping("/careers")
  public ResponseEntity<Map<String, Object>> createCareer(
      @AuthenticationPrincipal UserSession user,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody CareerBody body) {
    return reply(academic.createCareer(user, key, body.codigo(), body.nombre()));
  }

  @PatchMapping("/careers/{careerId}")
  public Map<String, Object> renameCareer(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID careerId,
      @Valid @RequestBody CareerPatch body) {
    return academic.renameCareer(user, careerId, body.nombre());
  }

  @GetMapping("/careers/{careerId}/parallels")
  public Map<String, Object> parallels(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID careerId) {
    return academic.parallels(user, careerId);
  }

  @PostMapping("/careers/{careerId}/parallels")
  public ResponseEntity<Map<String, Object>> createParallel(
      @AuthenticationPrincipal UserSession user,
      @RequestHeader("Idempotency-Key") UUID key,
      @PathVariable UUID careerId,
      @Valid @RequestBody ParallelBody body) {
    return reply(academic.createParallel(user, key, careerId, body.codigo()));
  }

  @PutMapping("/parallels/{parallelId}/enrollments")
  public Map<String, Object> enrollments(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID parallelId,
      @Valid @RequestBody Cedulas body) {
    return academic.replaceEnrollments(user, parallelId, body.cedulas());
  }

  @PutMapping("/parallels/{parallelId}/teachers")
  public Map<String, Object> teachers(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID parallelId,
      @Valid @RequestBody Correos body) {
    return academic.replaceTeachers(user, parallelId, body.correos());
  }
}
