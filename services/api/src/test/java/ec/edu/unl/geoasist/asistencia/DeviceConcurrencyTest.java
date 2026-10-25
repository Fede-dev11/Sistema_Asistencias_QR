package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class DeviceConcurrencyTest extends PostgresFixture {
  @Test
  void fiftyPeopleOnOneDeviceCreateExactlyOneAttendance() throws Exception {
    int runs = Integer.getInteger("geoasist.device-concurrency.runs", 20);
    var pool = Executors.newFixedThreadPool(50);
    try {
      for (int round = 0; round < runs; round++) {
        String[] ids = new String[50];
        for (int i = 0; i < ids.length; i++) ids[i] = fictionalId();
        var event = guests(120, ids);
        String[] starts = new String[50];
        for (int i = 0; i < starts.length; i++)
          starts[i] = open(event).get("registro_invitado_token").asText();
        UUID device = UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
          int index = i;
          results.add(pool.submit(() -> {
            ready.await();
            return guestSubmit(starts[index], ids[index], UUID.randomUUID(), device)
                .andReturn().getResponse().getStatus();
          }));
        }
        ready.countDown();
        int accepted = 0;
        for (Future<Integer> result : results) if (result.get(60, TimeUnit.SECONDS) == 201) accepted++;
        assertEquals(1, accepted);
        assertEquals(1, db.queryForObject(
            "SELECT count(*) FROM asistencias WHERE sesion_id=?", Integer.class, event.session()));
      }
    } finally {
      pool.shutdownNow();
    }
  }
}
