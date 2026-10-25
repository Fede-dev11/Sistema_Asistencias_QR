package ec.edu.unl.geoasist.asistencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import ec.edu.unl.geoasist.soporte.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenEventTest extends ClassSupport {
  @Test void selfRegistrationCreatesExternalAndPreservesExistingName() throws Exception {
    String owner=token(ScenarioFixtures.TEACHER);
    var created=ok(send("POST","/api/v1/activities",owner,Map.of("modalidad","EVENTO","nombre","Abierto",
        "espacio_id",SPACE,"acceso","ABIERTO","inicio","2026-10-19T00:00:00Z","fin","2026-10-21T00:00:00Z")),201);
    UUID session=UUID.fromString(created.get("sesiones").get(0).get("id").asText());
    start(session,owner);
    String qr=qr(session,owner);
    GuestScenario event=new GuestScenario(UUID.fromString(created.get("id").asText()),session,owner,qr);
    String cedula=fictionalId();
    String secret=open(event).get("registro_invitado_token").asText();
    ok(api.perform(get("/api/v1/events/open/"+session)),200);
    api.perform(post("/api/v1/attendance/attempts").header("Authorization","Bearer "+token(ScenarioFixtures.DIRECTOR))
        .header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
        .content(json.writeValueAsString(Map.of("qr_token",qr,"dispositivo_id",UUID.randomUUID()))))
        .andExpect(r->assertEquals(201,r.getResponse().getStatus()));
    guestSubmit(secret,cedula,"Nombre escrito",UUID.randomUUID(),UUID.randomUUID()).andExpect(r->assertEquals(201,r.getResponse().getStatus()));
    assertEquals("AUTORREGISTRO",db.queryForObject("SELECT forma FROM asistencias WHERE sesion_id=?",String.class,session));
    assertEquals("EXTERNA",db.queryForObject("SELECT tipo FROM personas WHERE cedula_normalizada=?",String.class,cedula));
    assertEquals("Nombre escrito",db.queryForObject("SELECT nombres_registro FROM asistencias WHERE sesion_id=?",String.class,session));

    String existing=fictionalId(); UUID person=UUID.randomUUID();
    db.update("INSERT INTO personas VALUES (?,?,?,'EXTERNA')",person,existing,"Nombre original");
    guestSubmit(open(event).get("registro_invitado_token").asText(),existing,"Nombre del formulario",UUID.randomUUID(),UUID.randomUUID())
        .andExpect(r->assertEquals(201,r.getResponse().getStatus()));
    assertEquals("Nombre original",db.queryForObject("SELECT nombres FROM personas WHERE id=?",String.class,person));
  }

  @Test void accountCedulaRequiresCredentialsAndClosedRejectsNames() throws Exception {
    var open=scenario("EVENTO",201); db.update("UPDATE actividades SET acceso='ABIERTO' WHERE id=?",open[0]);
    db.update("INSERT INTO configuracion_invitados(actividad_id,habilitado,tolerancia_invitado_segundos) VALUES (?,true,120)",open[0]);
    String owner=token("encargado@example.test"); start(open[1],owner);
    GuestScenario event=new GuestScenario(open[0],open[1],owner,qr(open[1],owner));
    String secret=open(event).get("registro_invitado_token").asText();
    guestSubmit(secret,"0100000033","Con cuenta",UUID.randomUUID(),UUID.randomUUID())
        .andExpect(r->assertEquals(403,r.getResponse().getStatus()));

    String cedula=fictionalId(); var closed=guests(120,cedula);
    guestSubmit(open(closed).get("registro_invitado_token").asText(),cedula,"No permitido",UUID.randomUUID(),UUID.randomUUID())
        .andExpect(r->assertEquals(400,r.getResponse().getStatus()));
  }
}
