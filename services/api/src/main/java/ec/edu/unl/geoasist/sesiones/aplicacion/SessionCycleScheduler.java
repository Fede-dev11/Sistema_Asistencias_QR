package ec.edu.unl.geoasist.sesiones.aplicacion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "geoasist.scheduling-enabled", matchIfMissing = true)
public class SessionCycleScheduler {
  private final SessionCycle cycle;

  public SessionCycleScheduler(SessionCycle cycle) {
    this.cycle = cycle;
  }

  @Scheduled(fixedDelay = 60000)
  public void tick() {
    cycle.tick();
  }
}
