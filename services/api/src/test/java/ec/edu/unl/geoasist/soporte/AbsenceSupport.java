package ec.edu.unl.geoasist.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Ayudas de 003 para ausencias y estadísticas: estudiantes 102, 107 (paralelo 11) y 106 (12). */
public abstract class AbsenceSupport extends PostgresFixture {
  protected static final UUID S1 = ScenarioFixtures.id(102);
  protected static final UUID S2 = ScenarioFixtures.id(107);
  protected static final UUID S3 = ScenarioFixtures.id(106);
  private static final AtomicInteger YEARS = new AtomicInteger(2040);

  /** Año propio por prueba: las estadísticas por periodo no se mezclan con otras pruebas. */
  protected static Instant day(int year, int day) {
    return Instant.parse("%d-01-01T13:00:00Z".formatted(year)).plusSeconds(86400L * day);
  }

  protected static int nextYear() {
    return YEARS.incrementAndGet();
  }

  protected void authorize(UUID activity, boolean valid, UUID... people) {
    for (UUID p : people)
      db.update(
          "INSERT INTO estudiantes_autorizados VALUES (?,?,?) ON CONFLICT(actividad_id,persona_id) DO UPDATE SET vigente=?",
          activity,
          p,
          valid,
          valid);
  }

  /** Sesión ACTIVA adicional en el instante dado. */
  protected UUID activeSession(UUID activity, Instant start) {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO sesiones(id,actividad_id,estado,ventana_inicio,ventana_fin,iniciada_en) VALUES (?,?,'ACTIVA',?,?,?)",
        id,
        activity,
        Timestamp.from(start),
        Timestamp.from(start.plusSeconds(7200)),
        Timestamp.from(start));
    return id;
  }

  protected void attend(UUID session, UUID person) {
    UUID attempt = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,'Nombre ficticio','ACEPTADO','ASISTENCIA_CONFIRMADA','Registrada','CREDENCIALES',?)",
        attempt,
        session,
        person,
        Timestamp.from(clock.instant()));
    db.update(
        "INSERT INTO asistencias(id,sesion_id,persona_id,intento_id,registrada_en,forma,nombres_registro) VALUES (?,?,?,?,?,'CREDENCIALES','Nombre ficticio')",
        UUID.randomUUID(),
        session,
        person,
        attempt,
        Timestamp.from(clock.instant()));
  }

  protected void close(UUID session, String bearer) throws Exception {
    api.perform(
            post("/api/v1/sessions/" + session + "/close")
                .header("Authorization", "Bearer " + bearer)
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(status().isOk());
  }

  protected int absences(UUID session) {
    return db.queryForObject(
        "SELECT count(*) FROM ausencias WHERE sesion_id=?", Integer.class, session);
  }
}
