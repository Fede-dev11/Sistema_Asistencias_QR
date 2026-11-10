package ec.edu.unl.sso.compartido;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.unl.sso.soporte.AuthFixture;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RetentionJobTest extends AuthFixture {
  @Autowired RetentionJob job;

  private void ingreso(OffsetDateTime en) {
    db.update(
        "INSERT INTO registro_ingresos(id, correo, resultado, origen, en) VALUES (gen_random_uuid(), 'a@example.test', 'OK', '127.0.0.1', ?)",
        en);
  }

  @Test
  void deletesOldLoginsAndExpiredOrUsedTicketsOnly() throws Exception {
    OffsetDateTime now = clock.instant().atOffset(ZoneOffset.UTC);
    ingreso(now.minusDays(91));
    ingreso(now.minusDays(89));
    // un ticket vigente (se emite ahora) y otro vencido por el paso del reloj
    String tgc = tgc("docente@example.test");
    ticket(tgc, EXAMPLE);
    clock.set(clock.instant().plusSeconds(60));
    ticket(tgc, EXAMPLE);
    assertEquals(3, db.queryForObject("SELECT count(*) FROM tickets", Integer.class));

    job.purgar();

    assertEquals(
        2,
        db.queryForObject(
            "SELECT count(*) FROM registro_ingresos WHERE en > ?",
            Integer.class,
            now.minusDays(90)));
    assertEquals(1, db.queryForObject("SELECT count(*) FROM tickets", Integer.class));
    clock.set(clock.instant().plus(Duration.ofDays(100)));
    job.purgar();
    assertEquals(0, db.queryForObject("SELECT count(*) FROM registro_ingresos", Integer.class));
  }
}
