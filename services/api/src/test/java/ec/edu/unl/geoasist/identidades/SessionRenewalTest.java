package ec.edu.unl.geoasist.identidades;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.geoasist.soporte.PostgresFixture;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SessionRenewalTest extends PostgresFixture {
  String renew(String bearer, int status) throws Exception {
    var r = api.perform(post("/api/v1/auth/renew").header("Authorization", "Bearer " + bearer));
    r.andExpect(status().is(status));
    return status == 200
        ? json.readTree(r.andReturn().getResponse().getContentAsString()).get("access_token").asText()
        : null;
  }

  void me(String bearer, int status) throws Exception {
    api.perform(get("/api/v1/me").header("Authorization", "Bearer " + bearer))
        .andExpect(status().is(status));
  }

  @Test
  void renewalRotatesSecretAndOldOneStopsWorking() throws Exception {
    String first = token("estudiante@example.test");
    String second = renew(first, 200);
    me(first, 401);
    me(second, 200);
    renew(first, 401);
  }

  @Test
  void revokedOrExpiredSessionCannotRenew() throws Exception {
    String t = token("estudiante@example.test");
    api.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + t));
    renew(t, 401);
    String u = token("estudiante@example.test");
    clock.set(clock.instant().plus(Duration.ofHours(9)));
    renew(u, 401);
  }

  @Test
  void renewsUpToThirtyDaysFromOriginalLogin() throws Exception {
    var start = clock.instant();
    String t = token("estudiante@example.test");
    for (int k = 1; k <= 4 * 29 + 3; k++) {
      clock.set(start.plus(Duration.ofHours(6L * k)));
      t = renew(t, 200);
    }
    clock.set(start.plus(Duration.ofDays(30)).plus(Duration.ofMinutes(1)));
    api.perform(post("/api/v1/auth/renew").header("Authorization", "Bearer " + t))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("code").value("RENOVACION_VENCIDA"));
  }
}
