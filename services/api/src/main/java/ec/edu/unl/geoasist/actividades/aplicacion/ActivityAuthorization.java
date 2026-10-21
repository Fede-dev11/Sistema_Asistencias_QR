package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.actividades.dominio.Activity;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.aplicacion.NominalAccessGuard;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ActivityAuthorization {
  private final ActivityRepository activities;
  private final NominalAccessGuard retention;

  public ActivityAuthorization(ActivityRepository activities, NominalAccessGuard retention) {
    this.activities = activities;
    this.retention = retention;
  }

  public boolean responsible(UserSession u, Activity a) {
    return u.responsibleRole() && u.accountId().equals(a.responsible());
  }

  public boolean canRead(UserSession u, Activity a) {
    return responsible(u, a)
        || u.has("DIRECTOR_INSTITUCIONAL")
        || (u.has("DIRECTOR_CARRERA") && activities.directs(u.accountId(), a.id()));
  }

  public Activity read(UserSession user, UUID id) {
    Activity a = activities.find(id);
    if (!canRead(user, a)) throw ApiFailure.forbidden();
    retention.check(a.anonymizeAt());
    return a;
  }

  public Activity write(UserSession user, UUID id) {
    Activity a = activities.find(id);
    if (!responsible(user, a)) throw ApiFailure.forbidden();
    retention.check(a.anonymizeAt());
    return a;
  }
}
