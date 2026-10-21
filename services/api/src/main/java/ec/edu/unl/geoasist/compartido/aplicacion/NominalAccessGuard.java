package ec.edu.unl.geoasist.compartido.aplicacion;

import ec.edu.unl.geoasist.compartido.dominio.RetentionPolicy;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class NominalAccessGuard {
  private final Clock clock;

  public NominalAccessGuard(Clock clock) {
    this.clock = clock;
  }

  public void check(Instant deadline) {
    if (RetentionPolicy.expired(deadline, clock.instant()))
      throw new ApiFailure(410, "DATOS_ANONIMIZADOS", "El plazo de conservación nominal terminó.");
  }
}
