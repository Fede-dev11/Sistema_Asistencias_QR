package ec.edu.unl.geoasist.sesiones;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.sesiones.aplicacion.SessionCycle;
import ec.edu.unl.geoasist.soporte.AbsenceSupport;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SessionCycleConcurrencyTest extends AbsenceSupport {
  @Autowired SessionCycle cycle;

  @Test
  void twoSimultaneousTicksTransitionEachSessionOnce() throws Exception {
    var ids = scenario("CLASE", 200);
    db.update("UPDATE actividades SET ciclo_automatico=true WHERE id=?", ids[0]);
    authorize(ids[0], true, S1);
    List<UUID> sessions = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      UUID id = UUID.randomUUID();
      db.update(
          "INSERT INTO sesiones(id,actividad_id,ventana_inicio,ventana_fin) VALUES (?,?,?,?)",
          id,
          ids[0],
          Timestamp.from(clock.instant().minusSeconds(60)),
          Timestamp.from(clock.instant().plusSeconds(3600)));
      sessions.add(id);
    }
    run();
    for (UUID s : sessions)
      assertEquals(
          "ACTIVA", db.queryForObject("SELECT estado FROM sesiones WHERE id=?", String.class, s));
    assertEquals(21, audited(ids[0], "SISTEMA_INICIO"), "20 nuevas + la del escenario");
    clock.set(clock.instant().plusSeconds(86401));
    run();
    for (UUID s : sessions) assertEquals(1, absences(s));
    assertEquals(21, audited(ids[0], "SISTEMA_CIERRE"));
  }

  private int audited(UUID activity, String action) {
    return db.queryForObject(
        "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=? AND accion=?",
        Integer.class,
        activity,
        action);
  }

  private void run() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    var go = new CountDownLatch(1);
    try {
      List<Future<?>> calls = new ArrayList<>();
      for (int i = 0; i < 2; i++)
        calls.add(
            pool.submit(
                () -> {
                  go.await();
                  cycle.tick();
                  return null;
                }));
      go.countDown();
      for (var c : calls) c.get(60, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }
  }
}
