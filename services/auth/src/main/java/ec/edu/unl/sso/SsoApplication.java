package ec.edu.unl.sso;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SsoApplication {
  public static void main(String[] args) {
    SpringApplication.run(SsoApplication.class, args);
  }

  /** Reloj inyectable: las pruebas lo reemplazan para controlar el tiempo. */
  @Bean
  @ConditionalOnMissingBean
  Clock clock() {
    return Clock.systemUTC();
  }
}
