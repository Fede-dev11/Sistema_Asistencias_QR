package ec.edu.unl.geoasist.compartido.aplicacion;

import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerTime {
  @Bean
  @ConditionalOnMissingBean
  public Clock serverClock() {
    return Clock.tick(Clock.systemUTC(), java.time.Duration.ofNanos(1000));
  }

  public static Instant received(jakarta.servlet.http.HttpServletRequest request) {
    return ((Instant) request.getAttribute("received_at"))
        .truncatedTo(java.time.temporal.ChronoUnit.MICROS);
  }
}
