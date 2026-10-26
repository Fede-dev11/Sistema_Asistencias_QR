package ec.edu.unl.sso.soporte;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {
  public static final Instant START = Instant.parse("2026-10-20T01:00:00Z");
  private volatile Instant now = START;

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
