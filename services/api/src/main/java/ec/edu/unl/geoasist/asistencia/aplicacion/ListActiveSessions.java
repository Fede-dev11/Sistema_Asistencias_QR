package ec.edu.unl.geoasist.asistencia.aplicacion;

import ec.edu.unl.geoasist.asistencia.persistencia.ActiveSessionsRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListActiveSessions {
  private final ActiveSessionsRepository repository;
  private final Clock clock;

  public ListActiveSessions(ActiveSessionsRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> list(UserSession user, int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw ApiFailure.invalid();
    return repository.list(user.accountId(), user.personId(), clock.instant(), page, size);
  }
}
