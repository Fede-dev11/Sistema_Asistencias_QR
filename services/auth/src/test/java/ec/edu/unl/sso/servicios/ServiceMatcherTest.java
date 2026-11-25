package ec.edu.unl.sso.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.unl.sso.servicios.aplicacion.ServiceMatcher;
import ec.edu.unl.sso.soporte.AuthFixture;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class ServiceMatcherTest extends AuthFixture {
  @Autowired ServiceMatcher matcher;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://localhost.evil",
        "https://localhost.evil/auth/callback",
        "http://localhost.evil:3000/auth/callback",
        "//evil",
        "//evil/auth/callback",
        "http://localhost@evil",
        "http://localhost:3000@evil/auth/callback",
        "http://evil@localhost:3000/auth/callback",
        "http://localhost:3000@localhost:3000/auth/callback",
        "HTTP://LOCALHOST:3000/auth/callback",
        "http://LOCALHOST:3000/auth/callback",
        "http://localhost:3001/auth/callback",
        "http://localhost/auth/callback",
        "https://localhost:3000/auth/callback",
        "http://localhost:3000/auth/callbackevil",
        "http://localhost:3000/auth/../evil",
        "http://localhost:3000/auth/%2e%2e/evil",
        "http://localhost:3000\\@evil/auth/callback",
        "http://localhost:3000/auth/callback#frag",
        "http://localhost:3000/other",
        "http://localhost:3000/auth/callback\r\nSet-Cookie: x=1",
        "javascript:alert(1)",
        "data:text/html,x",
        "http://127.0.0.1:3000/auth/callback",
        "http://localhost:8082",
        " http://localhost:3000/auth/callback",
        "",
        "geoasist://evil/callback",
        "geoasist://auth/other",
        "geoasist://evil",
        "geoasist://auth@evil/callback",
        "geoasist://auth/callbackX",
        "geoasist://auth/callback@evil.com",
        "geoasist2://auth/callback",
        "GEOASIST://auth/callback",
        "otro://auth/callback"
      })
  void rejectsOpenRedirectAttempts(String service) {
    assertEquals(Optional.empty(), matcher.match(service));
  }

  @Test
  void rejectsNull() {
    assertEquals(Optional.empty(), matcher.match(null));
  }

  @Test
  void acceptsRegisteredReturns() {
    assertEquals(
        "geoasist-web", matcher.match("http://localhost:3000/auth/callback").get().nombre());
    assertEquals(
        "geoasist-web", matcher.match("http://localhost:3000/auth/callback?x=1").get().nombre());
    assertEquals("ejemplo", matcher.match("http://localhost:8082/").get().nombre());
    assertEquals("ejemplo", matcher.match("http://localhost:8082/pagina/otra?q=1").get().nombre());
  }

  @Test
  void acceptsMobileSchemeOnlyWithExactPrefix() {
    assertEquals(
        "geoasist-movil", matcher.match("geoasist://auth/callback?n=abc123").get().nombre());
    assertTrue(matcher.match("geoasist://auth/callback/sub").isPresent());
    assertEquals("geoasist-movil", matcher.match("geoasist://auth/callback").get().nombre());
  }
}
