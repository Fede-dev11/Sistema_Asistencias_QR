package ec.edu.unl.geoasist.actividades.presentacion;

import ec.edu.unl.geoasist.actividades.aplicacion.*;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activities/{activityId}")
public class GuestManagementController {
  private final ConfigureGuests settings;
  private final ReplaceGuestList list;
  private final ReadActivityConfiguration reads;

  public GuestManagementController(
      ConfigureGuests settings, ReplaceGuestList list, ReadActivityConfiguration reads) {
    this.settings = settings;
    this.list = list;
    this.reads = reads;
  }

  @GetMapping("/guest-settings")
  public Map<String, Object> getSettings(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID activityId) {
    return reads.guestSettings(user, activityId);
  }

  @GetMapping("/guest-list")
  public Map<String, Object> getList(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return reads.guestList(user, activityId, page, size);
  }

  @GetMapping("/schedule")
  public Map<String, Object> getSchedule(
      @AuthenticationPrincipal UserSession user, @PathVariable UUID activityId) {
    return reads.schedule(user, activityId);
  }

  public record Settings(
      @NotNull Boolean habilitado, @NotNull @Min(41) Integer tolerancia_invitado_segundos) {}

  public record Entry(@NotBlank String cedula, @NotBlank String nombres) {
    public Entry {
      cedula = cedula == null ? null : cedula.strip();
      nombres = nombres == null ? null : nombres.strip();
    }

    @Override
    public String toString() {
      return "Entry[datos omitidos]";
    }
  }

  public record Replacement(@NotNull @Size(max = 1000) List<@NotNull @Valid Entry> invitados) {}

  @PutMapping("/guest-settings")
  public Map<String, Object> settings(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @Valid @RequestBody Settings body) {
    return settings.set(user, activityId, body.habilitado(), body.tolerancia_invitado_segundos());
  }

  @PutMapping("/guest-list")
  public Map<String, Object> list(
      @AuthenticationPrincipal UserSession user,
      @PathVariable UUID activityId,
      @Valid @RequestBody Replacement body) {
    return list.replace(
        user,
        activityId,
        body.invitados().stream()
            .map(e -> new ReplaceGuestList.Entry(e.cedula(), e.nombres()))
            .toList());
  }
}
