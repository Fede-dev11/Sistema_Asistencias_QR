package ec.edu.unl.geoasist.asistencia.presentacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/open")
public class OpenEventController {
  private final JdbcTemplate db;

  public OpenEventController(JdbcTemplate db) {
    this.db = db;
  }

  @GetMapping("/{sessionId}")
  public Map<String, Object> get(@PathVariable UUID sessionId) {
    return db.query(
            "SELECT a.nombre,e.nombre espacio,s.metodo_verificacion FROM sesiones s JOIN actividades a ON a.id=s.actividad_id LEFT JOIN espacios e ON e.id=s.espacio_id WHERE s.id=? AND s.estado='ACTIVA' AND a.estado='ABIERTA' AND a.modalidad='EVENTO' AND a.acceso='ABIERTO'",
            (r, n) -> {
              var out = new LinkedHashMap<String, Object>();
              out.put("sesion_id", sessionId);
              out.put("nombre", r.getString("nombre"));
              out.put("espacio", r.getString("espacio"));
              out.put("metodo_verificacion", r.getString("metodo_verificacion"));
              return out;
            }, sessionId).stream().findFirst().orElseThrow(ApiFailure::missing);
  }
}
