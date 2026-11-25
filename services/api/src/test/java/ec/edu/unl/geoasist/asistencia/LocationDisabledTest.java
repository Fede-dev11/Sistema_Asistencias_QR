package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class LocationDisabledTest extends PostgresFixture {
  @Test
  void offNeedsNoPostgisAndStoresNoCoordinates() throws Exception {
    String cedula = fictionalId();
    var event = guests(120, cedula);
    String secret = open(event).get("registro_invitado_token").asText();
    guestSubmit(secret, cedula, UUID.randomUUID()).andExpect(status().isCreated());
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM pg_extension WHERE extname='postgis'", Integer.class));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM intentos_asistencia WHERE sesion_id=? AND (distancia_m IS NOT NULL OR precision_m IS NOT NULL)",
            Integer.class,
            event.session()));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM information_schema.columns WHERE table_name IN ('asistencias','intentos_asistencia') AND column_name IN ('latitud','longitud')",
            Integer.class));
    api.perform(
            post("/api/v1/guest-registrations/submit")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "registro_invitado_token",
                            secret,
                            "cedula",
                            cedula,
                            "dispositivo_id",
                            UUID.randomUUID(),
                            "ubicacion",
                            Map.of(
                                "latitud",
                                0,
                                "longitud",
                                0,
                                "precision_m",
                                1,
                                "medido_en",
                                clock.instant().toString())))))
        .andExpect(status().isBadRequest());
  }
}
