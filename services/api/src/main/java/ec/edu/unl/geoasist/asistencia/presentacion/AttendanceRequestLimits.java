package ec.edu.unl.geoasist.asistencia.presentacion;

import ec.edu.unl.geoasist.compartido.aplicacion.OpaqueSecrets;
import ec.edu.unl.geoasist.identidades.aplicacion.RequestBudget;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AttendanceRequestLimits {
  private final RequestBudget budget;

  public AttendanceRequestLimits(RequestBudget budget) {
    this.budget = budget;
  }

  public void account(UUID person, UUID session) {
    budget.check("attendance:" + person + ":" + session, 30);
  }

  public void opening(String ip, UUID session) {
    budget.check("guest-opening:" + ip + ":" + (session == null ? "unknown" : session), 300);
  }

  public void submission(String ip, String secret) {
    budget.check("guest-submit-ip:" + ip, 300);
    budget.check("guest-submit-start:" + OpaqueSecrets.hash(secret), 6);
  }
}
