package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.unl.geoasist.asistencia.dominio.AttendanceOutcome;
import ec.edu.unl.geoasist.asistencia.dominio.GeoMeasurement;
import ec.edu.unl.geoasist.asistencia.dominio.GeographicPolicy;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GeographicPolicyTest {
  private static final Instant RECEIVED = Instant.parse("2026-10-20T01:00:00Z");
  private final GeographicPolicy policy = new GeographicPolicy();

  @Test
  void offAcceptsOnlyWithoutMeasurement() {
    assertEquals(
        AttendanceOutcome.Result.ACEPTADO,
        policy.decide(GeographicPolicy.Config.off(), null, null, 0, RECEIVED).result());
  }

  @Test
  void missingLocationAlwaysBlocksAndImpreciseMeasurementsCanBeReviewed() {
    var pilot = GeographicPolicy.Config.reviewOnly(20, 10, 60);
    var missing = policy.decide(pilot, null, null, 0, RECEIVED);
    assertEquals(AttendanceOutcome.Result.REPETIR, missing.result());
    assertEquals("UBICACION_REQUERIDA", missing.code());
    assertEquals(
        AttendanceOutcome.Result.REPETIR, policy.decide(pilot, null, null, 1, RECEIVED).result());
    assertEquals(
        "PRECISION_INSUFICIENTE",
        policy.decide(pilot, new GeoMeasurement(0, 0, 11, RECEIVED), 5d, 0, RECEIVED).code());
    assertEquals(
        "UBICACION_ANTIGUA",
        policy
            .decide(pilot, new GeoMeasurement(0, 0, 3, RECEIVED.minusSeconds(61)), 5d, 0, RECEIVED)
            .code());
    assertEquals(
        "UBICACION_INVALIDA",
        policy
            .decide(pilot, new GeoMeasurement(Double.NaN, 0, 3, RECEIVED), null, 0, RECEIVED)
            .code());
  }

  @Test
  void validatesCoordinateAndAgeBoundariesWithoutEscalatingInvalidMeasurements() {
    var config = GeographicPolicy.Config.automatic(20, 10, 60, true);
    for (var value :
        java.util.List.of(
            new GeoMeasurement(91, 0, 1, RECEIVED),
            new GeoMeasurement(0, -181, 1, RECEIVED),
            new GeoMeasurement(0, 0, -1, RECEIVED),
            new GeoMeasurement(0, 0, Double.POSITIVE_INFINITY, RECEIVED),
            new GeoMeasurement(0, 0, 1, null))) {
      var outcome = policy.decide(config, value, 0d, 3, RECEIVED);
      assertEquals("UBICACION_INVALIDA", outcome.code());
      assertEquals(AttendanceOutcome.Result.REPETIR, outcome.result());
    }
    for (var measured :
        java.util.List.of(
            RECEIVED.minusSeconds(60).minusNanos(1), RECEIVED.plusSeconds(5).plusNanos(1))) {
      var outcome = policy.decide(config, new GeoMeasurement(0, 0, 1, measured), 0d, 3, RECEIVED);
      assertEquals("UBICACION_ANTIGUA", outcome.code());
      assertEquals(AttendanceOutcome.Result.REPETIR, outcome.result());
    }
    for (var measured : java.util.List.of(RECEIVED.minusSeconds(60), RECEIVED.plusSeconds(5))) {
      assertEquals(
          AttendanceOutcome.Result.ACEPTADO,
          policy
              .decide(config, new GeoMeasurement(90, -180, 0, measured), 0d, 0, RECEIVED)
              .result());
    }
  }

  @Test
  void reviewOnlyNeverAcceptsOrRejectsAutomatically() {
    var outcome =
        policy.decide(
            GeographicPolicy.Config.reviewOnly(20, 10, 60),
            new GeoMeasurement(-4.03, -79.20, 3, RECEIVED.minusSeconds(2)),
            2d,
            0,
            RECEIVED);
    assertEquals(AttendanceOutcome.Result.PENDIENTE_REVISION, outcome.result());
    assertEquals("REVISION_UBICACION", outcome.code());
  }

  @Test
  void automaticModeUsesTheWholeAccuracyCircleOnlyWithServerEvidence() {
    var automatic = GeographicPolicy.Config.automatic(20, 10, 60, true);
    var inside = new GeoMeasurement(-4.03, -79.20, 3, RECEIVED);
    assertEquals(
        AttendanceOutcome.Result.ACEPTADO,
        policy.decide(automatic, inside, 16d, 0, RECEIVED).result());
    assertEquals(
        AttendanceOutcome.Result.RECHAZADO,
        policy.decide(automatic, inside, 24d, 0, RECEIVED).result());
    assertEquals(
        AttendanceOutcome.Result.REPETIR,
        policy.decide(automatic, inside, 19d, 0, RECEIVED).result());
    assertEquals(
        AttendanceOutcome.Result.PENDIENTE_REVISION,
        policy
            .decide(GeographicPolicy.Config.automatic(20, 10, 60, false), inside, 2d, 0, RECEIVED)
            .result());
  }
}
