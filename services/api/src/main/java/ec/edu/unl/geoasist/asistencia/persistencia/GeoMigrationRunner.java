package ec.edu.unl.geoasist.asistencia.persistencia;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@DependsOn("flywayInitializer")
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "geoasist.geography-enabled", havingValue = "true")
public class GeoMigrationRunner implements ApplicationRunner {
  private final DataSource dataSource;

  public GeoMigrationRunner(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void run(ApplicationArguments args) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/geo-migration")
        .table("flyway_geo_schema_history")
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .load()
        .migrate();
  }
}
