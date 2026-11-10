package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.sesiones.persistencia.SessionRepository.Session;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Plazo único de correcciones (FR-005..FR-007): sesión ACTIVA o CERRADA hace menos de N días. */
@Component
public class CorrectionWindow {
  private final Clock clock;
  private final long days;

  public CorrectionWindow(Clock clock, @Value("${geoasist.correccion.dias:7}") long days) {
    this.clock = clock;
    this.days = days;
  }

  public void require(Session session) {
    if ("ACTIVA".equals(session.state())) return;
    if ("CERRADA".equals(session.state()) && session.closed() != null) {
      if (session.closed().isBefore(clock.instant().minus(Duration.ofDays(days))))
        throw new ApiFailure(
            409, "PLAZO_CORRECCION_VENCIDO", "El plazo de corrección de la sesión venció.");
      return;
    }
    throw new ApiFailure(409, "SESION_INACTIVA", "La sesión no admite correcciones.");
  }
}
