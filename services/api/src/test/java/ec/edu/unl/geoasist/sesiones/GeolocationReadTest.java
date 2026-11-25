package ec.edu.unl.geoasist.sesiones;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.soporte.ClassSupport;
import ec.edu.unl.geoasist.soporte.ScenarioFixtures;
import java.util.*;
import org.junit.jupiter.api.Test;

class GeolocationReadTest extends ClassSupport {
  @Test
  void defaultsThenSavedConfigurationWithEvidenceAndClosedReadable() throws Exception {
    UUID[] ids = scenario("EVENTO", 201);
    String owner = token("encargado@example.test");
    String path = "/api/v1/sessions/" + ids[1] + "/geolocation";
    var off = ok(send("GET", path, owner, null), 200);
    assertFalse(off.get("habilitada").asBoolean());
    assertEquals("OFF", off.get("modo").asText());
    for (String k : List.of("radio_m", "precision_max_m", "antiguedad_max_s", "evidencia_id"))
      assertTrue(off.get(k).isNull(), k);
    assertFalse(off.get("evidencia_vigente").asBoolean());

    UUID space = UUID.randomUUID();
    db.update("INSERT INTO espacios(id,nombre,tipo) VALUES (?,'Espacio GPS','AULA')", space);
    db.update("UPDATE sesiones SET espacio_id=? WHERE id=?", space, ids[1]);
    UUID evidence = UUID.randomUUID();
    db.update(
        "INSERT INTO evidencias_geograficas VALUES (?,?,1,'ensayo',now(),?,0.01,0.02,now())",
        evidence,
        space,
        ScenarioFixtures.id(201));
    db.update(
        "INSERT INTO configuracion_geografica(sesion_id,habilitada,radio_m,precision_max_m,antiguedad_max_s,modo,evidencia_id,configurada_por,configurada_en) VALUES (?,true,25,35,45,'AUTO',?,?,now())",
        ids[1],
        evidence,
        ScenarioFixtures.id(201));
    var on = ok(send("GET", path, owner, null), 200);
    assertTrue(on.get("habilitada").asBoolean());
    assertEquals(25.0, on.get("radio_m").asDouble());
    assertEquals(35.0, on.get("precision_max_m").asDouble());
    assertEquals(45, on.get("antiguedad_max_s").asInt());
    assertEquals("AUTO", on.get("modo").asText());
    assertEquals(evidence.toString(), on.get("evidencia_id").asText());
    assertTrue(on.get("evidencia_vigente").asBoolean());

    db.update("UPDATE sesiones SET estado='CERRADA',cerrada_en=now() WHERE id=?", ids[1]);
    ok(send("GET", path, owner, null), 200);
    ok(send("GET", path, token(ScenarioFixtures.DIRECTOR), null), 200);
    ok(send("GET", path, token("otro-docente@example.test"), null), 403);
    ok(send("GET", "/api/v1/sessions/" + UUID.randomUUID() + "/geolocation", owner, null), 404);
  }
}
