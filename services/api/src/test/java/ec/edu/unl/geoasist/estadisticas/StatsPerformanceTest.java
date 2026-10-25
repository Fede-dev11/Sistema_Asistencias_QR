package ec.edu.unl.geoasist.estadisticas;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.geoasist.soporte.AbsenceSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** SC-003: 1 000 estudiantes y un semestre (30 sesiones) en menos de 3 s. Ejecutar con -DexcludedGroups= -Dgroups=performance. */
@Tag("performance")
class StatsPerformanceTest extends AbsenceSupport {
  private long timed(String path, String bearer) throws Exception {
    long t = System.nanoTime();
    api.perform(get(path).header("Authorization", "Bearer " + bearer)).andExpect(status().isOk());
    return (System.nanoTime() - t) / 1_000_000;
  }

  @Test
  void statsOfThousandStudentsOverASemesterTakeUnderThreeSeconds() throws Exception {
    String owner = token(ScenarioFixtures.TEACHER);
    var ids = scenario("CLASE", 200);
    db.update("INSERT INTO actividad_paralelos VALUES (?,?,?)", ids[0], ScenarioFixtures.id(1), ScenarioFixtures.id(11));
    db.update("DELETE FROM sesiones WHERE id=?", ids[1]);
    db.update(
        "INSERT INTO personas SELECT gen_random_uuid(),'99'||lpad(g::text,8,'0'),'Estudiante '||g,'INSTITUCIONAL' FROM generate_series(1,1000) g");
    db.update(
        "INSERT INTO estudiantes_autorizados SELECT ?,id,true FROM personas WHERE cedula_normalizada LIKE '99%'",
        ids[0]);
    int year = nextYear();
    for (int i = 0; i < 30; i++) {
      UUID s = UUID.randomUUID();
      var start = java.sql.Timestamp.from(day(year, i * 3));
      db.update(
          "INSERT INTO sesiones(id,actividad_id,estado,ventana_inicio,ventana_fin,iniciada_en,cerrada_en) VALUES (?,?,'CERRADA',?,?,?,?)",
          s, ids[0], start, java.sql.Timestamp.from(day(year, i * 3).plusSeconds(7200)), start,
          java.sql.Timestamp.from(day(year, i * 3).plusSeconds(7200)));
      // 80 % asiste (con intento), 20 % ausente.
      db.update(
          "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) SELECT gen_random_uuid(),?,id,'N','ACEPTADO','ASISTENCIA_CONFIRMADA','R','CREDENCIALES',now() FROM personas WHERE cedula_normalizada LIKE '99%' AND (hashtext(id::text)+?)%5<>0",
          s, i);
      db.update(
          "INSERT INTO asistencias(id,sesion_id,persona_id,intento_id,registrada_en,forma,nombres_registro) SELECT gen_random_uuid(),sesion_id,persona_id,id,now(),'CREDENCIALES','N' FROM intentos_asistencia WHERE sesion_id=?",
          s);
      db.update(
          "INSERT INTO ausencias SELECT ?,id,'AUSENTE',now() FROM personas WHERE cedula_normalizada LIKE '99%' AND (hashtext(id::text)+?)%5=0",
          s, i);
    }
    long activity = timed("/api/v1/activities/" + ids[0] + "/stats?size=100", owner);
    long parallels = timed("/api/v1/stats/parallels", owner);
    long careers = timed("/api/v1/stats/careers", token("director@example.test"));
    System.out.println("SC-003 ms activity=" + activity + " parallels=" + parallels + " careers=" + careers);
    assertTrue(Math.max(activity, Math.max(parallels, careers)) < 3000);
  }
}
