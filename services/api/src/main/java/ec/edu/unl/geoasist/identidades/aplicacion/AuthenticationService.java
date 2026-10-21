package ec.edu.unl.geoasist.identidades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.aplicacion.OpaqueSecrets;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.identidades.persistencia.IdentityRepository;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
  private final IdentityRepository accounts;
  private final ObjectProvider<IdentityProvider> provider;
  private final OpaqueSecrets secrets;
  private final Clock clock;

  public AuthenticationService(
      IdentityRepository accounts,
      ObjectProvider<IdentityProvider> provider,
      OpaqueSecrets secrets,
      Clock clock) {
    this.accounts = accounts;
    this.provider = provider;
    this.secrets = secrets;
    this.clock = clock;
  }

  @Transactional
  public Map<String, Object> login(String email, String password) {
    IdentityProvider identity = provider.getIfAvailable();
    if (identity == null) throw ApiFailure.unauthenticated();
    UUID account = identity.authenticate(email, password);
    String token = secrets.issue();
    Instant now = clock.instant(), expiry = now.plus(Duration.ofHours(8));
    accounts.createSession(UUID.randomUUID(), account, OpaqueSecrets.hash(token), now, expiry);
    return Map.of(
        "access_token",
        token,
        "token_type",
        "Bearer",
        "vence_en",
        expiry,
        "roles",
        accounts.roles(account));
  }

  public UserSession authenticate(String token) {
    return accounts
        .session(OpaqueSecrets.hash(token), clock.instant())
        .orElseThrow(ApiFailure::unauthenticated);
  }

  @Transactional
  public void logout(UserSession user) {
    accounts.revoke(user.sessionId(), clock.instant());
  }
}
