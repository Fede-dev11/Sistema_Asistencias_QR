package ec.edu.unl.geoasist.espacios;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SpaceSnapshotTest extends PostgresFixture {
  @Test
  void editingSpaceDefaultsKeepsSessionGeographicSettings() throws Exception {
    UUID space = UUID.randomUUID();
    db.update(
        "INSERT INTO espacios(id,nombre,tipo,codigo,codigo_normalizado,latitud,longitud,creado_por) VALUES (?,?,'AULA','SNAP1','SNAP1',-4.0,-79.0,?)",
        space,
        "Aula snapshot",
        ScenarioFixtures.id(200));
    UUID[] ids = scenario("CLASE", 200);
    UUID closed = UUID.randomUUID();
    db.update(
        "INSERT INTO sesiones(id,actividad_id,espacio_id,estado,ventana_inicio,ventana_fin,iniciada_en,cerrada_en) VALUES (?,?,?,'CERRADA',?,?,?,?)",
        closed,
        ids[0],
        space,
        Timestamp.from(clock.instant().minusSeconds(7200)),
        Timestamp.from(clock.instant().minusSeconds(3600)),
        Timestamp.from(clock.instant().minusSeconds(7100)),
        Timestamp.from(clock.instant().minusSeconds(3700)));
    db.update("UPDATE sesiones SET espacio_id=?,estado='ACTIVA',iniciada_en=? WHERE id=?", space, Timestamp.from(clock.instant()), ids[1]);
    for (UUID s : new UUID[] {closed, ids[1]})
      db.update(
          "INSERT INTO configuracion_geografica(sesion_id,habilitada,radio_m,precision_max_m,antiguedad_max_s,modo,configurada_por,configurada_en) VALUES (?,true,20,30,60,'REVIEW_ONLY',?,?)",
          s,
          ScenarioFixtures.id(200),
          Timestamp.from(clock.instant()));
    api.perform(
            patch("/api/v1/spaces/" + space)
                .header("Authorization", "Bearer " + token(ScenarioFixtures.TEACHER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"radio_defecto_m\":99,\"precision_defecto_m\":88}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("radio_defecto_m").value(99.0));
    for (UUID s : new UUID[] {closed, ids[1]}) {
      assertEquals(
          20.0,
          db.queryForObject(
              "SELECT radio_m FROM configuracion_geografica WHERE sesion_id=?", Double.class, s));
      assertEquals(
          30.0,
          db.queryForObject(
              "SELECT precision_max_m FROM configuracion_geografica WHERE sesion_id=?",
              Double.class,
              s));
    }
  }
}
