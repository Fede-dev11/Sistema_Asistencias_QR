package ec.edu.unl.geoasist.soporte;

import java.time.*;

public class MutableClock extends Clock {
  private volatile Instant now = Instant.parse("2026-10-20T01:00:00Z");

  public void set(Instant instant) {
    now = instant;
  }

  @Override
  public Instant instant() {
    return now;
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    if (!zone.equals(ZoneOffset.UTC)) throw new IllegalArgumentException();
    return this;
  }
}
