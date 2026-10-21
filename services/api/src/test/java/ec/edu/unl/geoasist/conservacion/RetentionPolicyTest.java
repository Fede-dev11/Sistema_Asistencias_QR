package ec.edu.unl.geoasist.conservacion;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.compartido.dominio.RetentionPolicy;
import java.time.*;
import org.junit.jupiter.api.Test;

class RetentionPolicyTest {
  @Test
  void calendarAnniversaryAndStrictDeadline() {
    Instant closed = Instant.parse("2024-02-29T10:15:30Z"),
        deadline = Instant.parse("2029-02-28T10:15:30Z");
    assertEquals(deadline, RetentionPolicy.expires(closed));
    assertFalse(RetentionPolicy.expired(deadline, deadline.minusNanos(1)));
    assertTrue(RetentionPolicy.expired(deadline, deadline));
    assertFalse(RetentionPolicy.expired(null, deadline));
  }
}
