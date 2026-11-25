package ec.edu.unl.sso.compartido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.unl.sso.soporte.AuthFixture;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RateLimiterTest extends AuthFixture {
  @Autowired RateLimiter limiter;

  @Test
  void blocksAfterLimitAndReopensNextMinute() {
    for (int i = 0; i < 5; i++) assertTrue(limiter.allow("t", "a@x", 5));
    assertFalse(limiter.allow("t", "a@x", 5));
    assertTrue(limiter.allow("t", "otro@x", 5));
    assertEquals(60, limiter.retryAfterSeconds());
    clock.set(Instant.parse("2026-10-20T01:01:00Z"));
    assertTrue(limiter.allow("t", "a@x", 5));
  }

  @Test
  void secretsAreHashedAndRandom() {
    assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        Secrets.sha256Hex("abc"));
    assertEquals(43, Secrets.random().length());
    assertNotEquals(Secrets.random(), Secrets.random());
  }
}
