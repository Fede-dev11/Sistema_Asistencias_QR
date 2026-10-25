package ec.edu.unl.geoasist.asistencia.presentacion;

import ec.edu.unl.geoasist.asistencia.dominio.GeoMeasurement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class AttendanceInputs {
  private AttendanceInputs() {}

  public record Location(
      @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
      @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
      @NotNull @DecimalMin("0") Double precision_m,
      @NotNull Instant medido_en) {
    public GeoMeasurement measurement() {
      return new GeoMeasurement(latitud, longitud, precision_m, medido_en);
    }

    @Override
    public String toString() {
      return "Location[ubicación omitida]";
    }
  }

  public record AccountAttempt(
      @NotBlank @Size(min = 32, max = 512) String qr_token, @Valid Location ubicacion) {
    @Override
    public String toString() {
      return "AccountAttempt[datos omitidos]";
    }
  }

  public record GuestStart(@NotBlank @Size(min = 32, max = 512) String qr_token) {
    @Override
    public String toString() {
      return "GuestStart[secreto omitido]";
    }
  }

  public record GuestSubmit(
      @NotBlank @Size(min = 32, max = 512) String registro_invitado_token,
      @NotBlank String cedula,
      @Valid Location ubicacion) {
    public GuestSubmit {
      cedula = cedula == null ? null : cedula.strip();
    }

    @Override
    public String toString() {
      return "GuestSubmit[datos omitidos]";
    }
  }
}
