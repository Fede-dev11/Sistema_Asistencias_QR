package ec.edu.unl.geoasist.identidades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.persistencia.IdentityRepository;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevelopmentIdentityProvider implements IdentityProvider {
  private final IdentityRepository accounts;
  private final PasswordEncoder encoder;
  private final String fallback;

  public DevelopmentIdentityProvider(IdentityRepository accounts, PasswordEncoder encoder) {
    this.accounts = accounts;
    this.encoder = encoder;
    fallback = encoder.encode(UUID.randomUUID().toString());
  }

  public UUID authenticate(String email, String password) {
    var row = accounts.account(email);
    boolean matched =
        encoder.matches(password, row.map(r -> (String) r.get("password_hash")).orElse(fallback));
    if (row.isEmpty()
        || !"DEV".equals(row.get().get("proveedor"))
        || !matched
        || !Boolean.TRUE.equals(row.get().get("activa"))) throw ApiFailure.unauthenticated();
    return (UUID) row.get().get("id");
  }
}
