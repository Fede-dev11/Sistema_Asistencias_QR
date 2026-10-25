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
    return issue(account, clock.instant());
  }

  private Map<String, Object> issue(UUID account, Instant loginOriginal) {
    String token = secrets.issue();
    Instant now = clock.instant(), expiry = now.plus(Duration.ofHours(8));
    accounts.createSession(
        UUID.randomUUID(), account, OpaqueSecrets.hash(token), now, expiry, loginOriginal);
    return Map.of(
        "access_token",
        token,
        "token_type",
        "Bearer",
        "vence_en",
        expiry,
        "roles",
        accounts.roles(account),
        "debe_cambiar_password",
        accounts.mustChangePassword(account));
  }

  /** Rota el secreto: el anterior se revoca y el límite de 30 días cuenta desde el login original. */
  @Transactional
  public Map<String, Object> renew(UserSession user) {
    Instant original = accounts.loginOriginal(user.sessionId());
    if (clock.instant().isAfter(original.plus(Duration.ofDays(30))))
      throw new ApiFailure(
          401, "RENOVACION_VENCIDA", "La sesión superó 30 días. Ingresa con tus credenciales.");
    accounts.revoke(user.sessionId(), clock.instant());
    return issue(user.accountId(), original);
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
