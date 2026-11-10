package ec.edu.unl.geoasist.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.web.servlet.MvcResult;

/** Ayudas de 003 US2: llamadas con cuerpo JSON y atajos de datos. */
public abstract class CorrectionSupport extends AbsenceSupport {
  protected MvcResult send(String path, String bearer, UUID key, Map<String, ?> body)
      throws Exception {
    return api.perform(
            post("/api/v1" + path)
                .header("Authorization", "Bearer " + bearer)
                .header("Idempotency-Key", key.toString())
                .contentType("application/json")
                .content(json.writeValueAsString(body)))
        .andReturn();
  }

  protected MvcResult send(String path, String bearer, Map<String, ?> body) throws Exception {
    return send(path, bearer, UUID.randomUUID(), body);
  }

  protected String fetch(String path, String bearer) throws Exception {
    return api.perform(get("/api/v1" + path).header("Authorization", "Bearer " + bearer))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  protected static int status(MvcResult r) {
    return r.getResponse().getStatus();
  }

  protected String text(MvcResult r) throws Exception {
    return r.getResponse().getContentAsString();
  }

  protected String code(MvcResult r) throws Exception {
    return json.readTree(text(r)).path("code").asText();
  }

  protected UUID pendingAttempt(UUID session, UUID person) {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO intentos_asistencia(id,sesion_id,persona_id,nombres_referencia,resultado,code,mensaje,canal,recibido_en) VALUES (?,?,?,'Nombre ficticio','PENDIENTE_REVISION','UBICACION_INCIERTA','Pendiente','CREDENCIALES',?)",
        id,
        session,
        person,
        Timestamp.from(clock.instant()));
    return id;
  }

  protected int count(String sql, Object... args) {
    return db.queryForObject(sql, Integer.class, args);
  }
}
