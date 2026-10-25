package ec.edu.unl.geoasist.conservacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.Test;

class RetentionLifecycleTest extends PostgresFixture {
  @Test
  void expiredHeadersAndAllNominalChildrenDisappearWhileGlobalAccountSurvives() throws Exception {
    var expired = scenario("EVENTO", 201);
    var current = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    UUID person = ScenarioFixtures.id(102);
    UUID attempt = UUID.randomUUID();
    UUID attendance = UUID.randomUUID();
    db.update("INSERT INTO estudiantes_autorizados VALUES (?,?,true)", expired[0], person);
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,'Nombre ficticio','PENDIENTE_REVISION','UBICACION_INCIERTA','Motivo ficticio','CREDENCIALES',?)",
        attempt,
        expired[1],
        person,
        Timestamp.from(clock.instant()));
    db.update(
        "INSERT INTO asistencias VALUES (?,?,?,?,?,'REVISION','Nombre ficticio')",
        attendance,
        expired[1],
        person,
        attempt,
        Timestamp.from(clock.instant()));
    db.update(
        "INSERT INTO revisiones_asistencia VALUES (?,?,'APROBAR','Razón ficticia',?,?)",
        attempt,
        ScenarioFixtures.id(201),
        Timestamp.from(clock.instant()),
        attendance);
    db.update(
        "INSERT INTO auditoria_actividad VALUES (?,?,?,'REVISAR_INTENTO',?,?,'Razón ficticia','{}','{}')",
        UUID.randomUUID(),
        expired[0],
        ScenarioFixtures.id(201),
        Timestamp.from(clock.instant()),
        UUID.randomUUID());
    expire(expired[0]);
    api.perform(
            get("/api/v1/sessions/" + expired[1] + "/attendances/export")
                .header("Authorization", "Bearer " + owner))
        .andExpect(r -> assertEquals(410, r.getResponse().getStatus()));
    runBatch();
    runBatch();
    for (String table : List.of("actividades", "sesiones", "intentos_asistencia", "asistencias")) {
      UUID id =
          switch (table) {
            case "actividades" -> expired[0];
            case "sesiones" -> expired[1];
            case "intentos_asistencia" -> attempt;
            default -> attendance;
          };
      assertEquals(
          0, db.queryForObject("SELECT count(*) FROM " + table + " WHERE id=?", Integer.class, id));
    }
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM revisiones_asistencia WHERE intento_id=?",
            Integer.class,
            attempt));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_actividad WHERE actividad_id=?",
            Integer.class,
            expired[0]));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM cuentas WHERE persona_id=?", Integer.class, person));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM actividades WHERE id=?", Integer.class, current[0]));
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM tratamientos_conservacion WHERE actividad_id=?",
            Integer.class,
            expired[0]));
    assertTrue(
        db.queryForObject(
                "SELECT count(*) FROM tratamientos_conservacion WHERE fase='COPIAS_PENDIENTES'",
                Integer.class)
            >= 1);
    api.perform(
            get("/api/v1/sessions/" + expired[1] + "/attendances")
                .header("Authorization", "Bearer " + owner))
        .andExpect(r -> assertEquals(404, r.getResponse().getStatus()));
  }

  @Test
  void statisticsSuppressSmallCellsAndNeverPublishActivityOrPersonLinks() {
    var expired = scenario("CLASE", 200);
    expire(expired[0]);
    runBatch();
    assertEquals(
        0,
        db.queryForObject(
            "SELECT count(*) FROM estadisticas_anonimas WHERE modalidad='CLASE'", Integer.class));
    var columns =
        db.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name='estadisticas_anonimas'",
            String.class);
    assertEquals(Set.of("anio", "modalidad", "version", "participantes"), new HashSet<>(columns));
  }

  @Test
  void recoverableCopyAndUnverifiedInventoryPreventCompletion() {
    var expired = scenario("EVENTO", 201);
    expire(expired[0]);
    UUID copy = UUID.randomUUID();
    db.update(
        "INSERT INTO catalogo_copias(id,soporte,referencia,creada_en) VALUES (?,'WAL',?,?)",
        copy,
        "fixture/" + copy,
        Timestamp.from(clock.instant()));
    runBatch();
    UUID treatment =
        db.queryForObject(
            "SELECT id FROM tratamientos_conservacion ORDER BY procesado_en DESC LIMIT 1",
            UUID.class);
    db.update(
        "UPDATE politica_copias SET inventario_verificado_en=?,respaldo_saneado_en=?,evidencia_retiro='fixture-isolated' WHERE id=1",
        Timestamp.from(clock.instant()),
        Timestamp.from(clock.instant()));
    db.queryForObject("SELECT completar_tratamientos()", Integer.class);
    assertEquals(
        "COPIAS_PENDIENTES",
        db.queryForObject(
            "SELECT fase FROM tratamientos_conservacion WHERE id=?", String.class, treatment));
    db.update(
        "UPDATE catalogo_copias SET estado='RETIRADA',verificada_en=?,evidencia='fixture-deletion-verified' WHERE id=?",
        Timestamp.from(clock.instant()),
        copy);
    db.queryForObject("SELECT completar_tratamientos()", Integer.class);
    assertEquals(
        "COMPLETADO",
        db.queryForObject(
            "SELECT fase FROM tratamientos_conservacion WHERE id=?", String.class, treatment));
    db.update(
        "UPDATE politica_copias SET inventario_verificado_en=NULL,respaldo_saneado_en=NULL,evidencia_retiro=NULL WHERE id=1");
  }

  @Test
  void annualGroupIsDeduplicatedAndOrphanGuestsAreRemovedWithoutAnotherPurpose() {
    var first = scenario("EVENTO", 201);
    var second = scenario("EVENTO", 201);
    List<UUID> people = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      UUID person = UUID.randomUUID();
      people.add(person);
      db.update(
          "INSERT INTO personas VALUES (?,?,?,'EXTERNA')",
          person,
          fictionalId(),
          "Invitado ficticio " + i);
      for (var ids : List.of(first, second)) {
        UUID attempt = UUID.randomUUID();
        db.update(
            "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,'Invitado ficticio','ACEPTADO','ASISTENCIA_CONFIRMADA','Registrada','INVITADO_LISTADO',?)",
            attempt,
            ids[1],
            person,
            Timestamp.from(clock.instant()));
        db.update(
            "INSERT INTO asistencias VALUES (?,?,?,?,?,'INVITADO_LISTADO','Invitado ficticio')",
            UUID.randomUUID(),
            ids[1],
            person,
            attempt,
            Timestamp.from(clock.instant()));
      }
    }
    for (var ids : List.of(first, second)) {
      db.update(
          "UPDATE actividades SET estado='CERRADA',cerrada_en='2010-03-01T00:00:00Z',anonimizar_en='2015-03-01T00:00:00Z' WHERE id=?",
          ids[0]);
    }
    runBatch();
    runBatch();
    assertEquals(
        10,
        db.queryForObject(
            "SELECT participantes FROM estadisticas_anonimas WHERE anio=2010 AND modalidad='EVENTO'",
            Integer.class));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM estadisticas_anonimas WHERE anio=2010 AND modalidad='EVENTO'",
            Integer.class));
    for (UUID person : people)
      assertEquals(
          0, db.queryForObject("SELECT count(*) FROM personas WHERE id=?", Integer.class, person));
  }

  @Test
  void cleanupFailureRollsBackAndAccessStaysBlockedBeforeRetry() throws Exception {
    var ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    expire(ids[0]);
    String function = "fallo_retencion_" + UUID.randomUUID().toString().replace("-", "");
    db.execute(
        "CREATE FUNCTION "
            + function
            + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF OLD.id='"
            + ids[0]
            + "'::uuid THEN RAISE EXCEPTION 'Fallo ficticio'; END IF; RETURN OLD; END $$");
    db.execute(
        "CREATE TRIGGER "
            + function
            + " BEFORE DELETE ON actividades FOR EACH ROW EXECUTE FUNCTION "
            + function
            + "()");
    try {
      assertThrows(org.springframework.dao.DataAccessException.class, this::runBatch);
      assertEquals(
          1,
          db.queryForObject("SELECT count(*) FROM actividades WHERE id=?", Integer.class, ids[0]));
      assertEquals(
          0,
          db.queryForObject(
              "SELECT count(*) FROM tratamientos_conservacion WHERE actividad_id=?",
              Integer.class,
              ids[0]));
      api.perform(
              get("/api/v1/sessions/" + ids[1] + "/attendances")
                  .header("Authorization", "Bearer " + owner))
          .andExpect(r -> assertEquals(410, r.getResponse().getStatus()));
    } finally {
      db.execute("DROP TRIGGER " + function + " ON actividades");
      db.execute("DROP FUNCTION " + function + "()");
    }
    runBatch();
    assertEquals(
        0, db.queryForObject("SELECT count(*) FROM actividades WHERE id=?", Integer.class, ids[0]));
  }

  @Test
  void sharedSpacesSurviveRetention() {
    var ids = scenario("EVENTO", 201);
    UUID space = UUID.randomUUID();
    db.update(
        "INSERT INTO espacios(id,nombre,tipo,codigo,codigo_normalizado) VALUES (?,'Aula compartida','AULA','RET1','RET1')",
        space);
    db.update("UPDATE sesiones SET espacio_id=? WHERE id=?", space, ids[1]);
    expire(ids[0]);
    runBatch();
    assertEquals(
        0, db.queryForObject("SELECT count(*) FROM actividades WHERE id=?", Integer.class, ids[0]));
    assertEquals(
        1, db.queryForObject("SELECT count(*) FROM espacios WHERE id=?", Integer.class, space));
  }

  @Test
  void orphanSelfRegisteredExternalIsRemovedWithExpiredEvent() {
    var ids=scenario("EVENTO",201); UUID person=UUID.randomUUID(); UUID attempt=UUID.randomUUID();
    db.update("INSERT INTO personas VALUES (?,?,?,'EXTERNA')",person,fictionalId(),"Autorregistrada");
    db.update("INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,'Nombre escrito','ACEPTADO','ASISTENCIA_CONFIRMADA','Registrada','AUTORREGISTRO',?)",
        attempt,ids[1],person,Timestamp.from(clock.instant()));
    db.update("INSERT INTO asistencias(id,sesion_id,persona_id,intento_confirmado_id,registrada_en,forma,nombres_registro) VALUES (?,?,?,?,?,'AUTORREGISTRO','Nombre escrito')",
        UUID.randomUUID(),ids[1],person,attempt,Timestamp.from(clock.instant()));
    expire(ids[0]); runBatch();
    assertEquals(0,db.queryForObject("SELECT count(*) FROM personas WHERE id=?",Integer.class,person));
  }

  private void expire(UUID activity) {
    db.update(
        "UPDATE actividades SET estado='CERRADA',cerrada_en=?,anonimizar_en=? WHERE id=?",
        Timestamp.from(clock.instant().atZone(java.time.ZoneOffset.UTC).minusYears(5).toInstant()),
        Timestamp.from(clock.instant()),
        activity);
  }

  private void runBatch() {
    db.queryForObject(
        "SELECT anonimizar_vencidas(?,100)", Integer.class, Timestamp.from(clock.instant()));
  }
}
