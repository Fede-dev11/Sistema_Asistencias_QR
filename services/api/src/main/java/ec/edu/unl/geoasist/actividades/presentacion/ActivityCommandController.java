package ec.edu.unl.geoasist.actividades.presentacion;

import ec.edu.unl.geoasist.actividades.aplicacion.*;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityCommandController {
  public record StudentInput(@NotBlank @Pattern(regexp = "[0-9]{10}") String cedula) {}

  private final CloseActivity service;
  private final CreateActivity create;
  private final EditActivity edit;
  private final ReplaceSchedule schedule;
  private final ManageAuthorizedStudents students;

  public ActivityCommandController(
      CloseActivity service,
      CreateActivity create,
      EditActivity edit,
      ReplaceSchedule schedule,
      ManageAuthorizedStudents students) {
    this.service = service;
    this.create = create;
    this.edit = edit;
    this.schedule = schedule;
    this.students = students;
  }

  @PostMapping("/{activityId}/close")
  public ResponseEntity<Map<String, Object>> close(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @RequestHeader("Idempotency-Key") UUID key) {
    var receipt = service.close(user, activityId, key);
    return ResponseEntity.status(receipt.status()).body(receipt.body());
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(
      @AuthenticationPrincipal UserSession user, @RequestBody CreateActivity.Command body) {
    return ResponseEntity.status(201).body(create.create(user, body));
  }

  @PatchMapping("/{activityId}")
  public Map<String, Object> edit(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @RequestBody EditActivity.Changes body) {
    return edit.edit(user, activityId, body);
  }

  @PostMapping("/{activityId}/cancel")
  public Map<String, Object> cancel(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID activityId) {
    return edit.cancel(user, activityId);
  }

  @PutMapping("/{activityId}/schedule")
  public Map<String, Object> schedule(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @RequestBody CreateActivity.Schedule body) {
    return schedule.replace(user, activityId, body.weekly());
  }

  @GetMapping("/{activityId}/students")
  public Map<String, Object> students(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return students.list(user, activityId, page, size);
  }

  @PostMapping("/{activityId}/students")
  public ResponseEntity<Map<String, Object>> addStudent(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @Valid @RequestBody StudentInput body) {
    return ResponseEntity.status(201).body(students.add(user, activityId, body.cedula()));
  }

  @DeleteMapping("/{activityId}/students/{personId}")
  public ResponseEntity<Void> removeStudent(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @PathVariable UUID personId) {
    students.remove(user, activityId, personId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{activityId}/students/sync")
  public Map<String, Object> syncStudents(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID activityId) {
    return students.sync(user, activityId);
  }
}
