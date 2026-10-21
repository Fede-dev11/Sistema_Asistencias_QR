package ec.edu.unl.geoasist.conservacion.aplicacion;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RetentionMonitoring {
  private static final Logger LOG = LoggerFactory.getLogger(RetentionMonitoring.class);
  private final AtomicLong batches = new AtomicLong();
  private final AtomicLong failures = new AtomicLong();

  public void succeeded(int processed, long pending) {
    batches.incrementAndGet();
    if (processed > 0 || pending > 0)
      LOG.info(
          "Conservación: actividades tratadas={}, tratamientos con copias pendientes={}",
          processed,
          pending);
  }

  public void failed() {
    failures.incrementAndGet();
    // No incluir SQL, IDs, nombres, excepciones ni cuerpos que puedan contener datos nominales.
    LOG.error(
        "Conservación: lote fallido; acceso vencido sigue bloqueado; requiere reintento operativo.");
  }

  public long batches() {
    return batches.get();
  }

  public long failures() {
    return failures.get();
  }
}
