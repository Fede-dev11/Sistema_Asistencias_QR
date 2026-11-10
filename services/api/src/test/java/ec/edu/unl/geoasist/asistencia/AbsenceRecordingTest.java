package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ec.edu.unl.geoasist.soporte.AbsenceSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class AbsenceRecordingTest extends AbsenceSupport {
  @Test
  void closingRecordsAbsencesWithTheCurrentListAndKeepsThemAfterListChanges() throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    authorize(ids[0], true, S1, S2, S3);
    start(ids[1], owner);
    attend(ids[1], S1);
    UUID pending = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,'Nombre ficticio','PENDIENTE_REVISION','UBICACION_INCIERTA','Pendiente','CREDENCIALES',?)",
        pending,
        ids[1],
        S2,
        Timestamp.from(clock.instant()));
    assertEquals(0, absences(ids[1]), "nada antes del cierre");
    close(ids[1], owner);
    // US1-1 y US1-2: S2 con intento pendiente y S3 sin intento quedan ausentes.
    assertEquals(
        Set.of(S2, S3),
        new HashSet<>(
            db.queryForList(
                "SELECT persona_id FROM ausencias WHERE sesion_id=?", UUID.class, ids[1])));
    // US1-3: quitar a S3 de la lista no borra su ausencia.
    authorize(ids[0], false, S3);
    assertEquals(2, absences(ids[1]));
  }

  @Test
  void nonVigentStudentsAreNotMarkedAbsentAndEventsHaveNoAbsences() throws Exception {
    var cls = scenario("CLASE", 200);
    String teacher = token(ScenarioFixtures.TEACHER);
    authorize(cls[0], true, S1);
    authorize(cls[0], false, S2);
    start(cls[1], teacher);
    close(cls[1], teacher);
    assertEquals(
        List.of(S1),
        db.queryForList("SELECT persona_id FROM ausencias WHERE sesion_id=?", UUID.class, cls[1]));

    // US1-7
    var event = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    authorize(event[0], true, S1);
    start(event[1], owner);
    close(event[1], owner);
    assertEquals(0, absences(event[1]));
  }

  @Test
  void closingTheActivityRecordsAbsencesForEveryOpenSessionButNotForPastClosedOnes()
      throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    authorize(ids[0], true, S1);
    UUID active = activeSession(ids[0], clock.instant());
    UUID closed = activeSession(ids[0], clock.instant());
    close(closed, owner);
    // S2 se autoriza después del cierre de `closed`: su ausencia pasada no se recalcula.
    authorize(ids[0], true, S2);
    api.perform(
            post("/api/v1/activities/" + ids[0] + "/close")
                .header("Authorization", "Bearer " + owner)
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    assertEquals(2, absences(active));
    assertEquals(1, absences(closed));
    assertEquals(2, absences(ids[1]), "la sesión programada también se cierra");
  }

  @Test
  void simultaneousClosesRecordAbsencesOnlyOnce() throws Exception {
    var ids = scenario("CLASE", 200);
    String owner = token(ScenarioFixtures.TEACHER);
    authorize(ids[0], true, S1, S2, S3);
    start(ids[1], owner);
    attend(ids[1], S1);
    var pool = Executors.newFixedThreadPool(2);
    var go = new CountDownLatch(1);
    try {
      List<Future<Integer>> calls = new ArrayList<>();
      for (int i = 0; i < 2; i++)
        calls.add(
            pool.submit(
                () -> {
                  go.await();
                  return api.perform(
                          post("/api/v1/sessions/" + ids[1] + "/close")
                              .header("Authorization", "Bearer " + owner)
                              .header("Idempotency-Key", UUID.randomUUID().toString()))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      go.countDown();
      var statuses = new ArrayList<Integer>();
      for (var c : calls) statuses.add(c.get());
      Collections.sort(statuses);
      assertEquals(List.of(200, 409), statuses);
    } finally {
      pool.shutdownNow();
    }
    assertEquals(2, absences(ids[1]));
  }
}
