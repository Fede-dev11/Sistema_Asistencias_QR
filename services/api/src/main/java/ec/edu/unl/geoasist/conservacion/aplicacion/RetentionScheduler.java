package ec.edu.unl.geoasist.conservacion.aplicacion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "geoasist.scheduling-enabled", matchIfMissing = true)
public class RetentionScheduler {
  private final AnonymizeExpiredActivity anonymize;
  private final CopySanitizationService copies;
  private final RetentionMonitoring monitoring;

  public RetentionScheduler(
      AnonymizeExpiredActivity anonymize,
      CopySanitizationService copies,
      RetentionMonitoring monitoring) {
    this.anonymize = anonymize;
    this.copies = copies;
    this.monitoring = monitoring;
  }

  @Scheduled(fixedDelayString = "${geoasist.retention-delay-ms:3600000}")
  public void run() {
    try {
      int processed = anonymize.processBatch();
      copies.completeVerifiedTreatments();
      monitoring.succeeded(processed, copies.pendingTreatments());
    } catch (RuntimeException failure) {
      monitoring.failed();
    }
  }
}
