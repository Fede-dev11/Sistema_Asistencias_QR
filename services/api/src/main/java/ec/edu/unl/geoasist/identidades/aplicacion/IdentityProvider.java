package ec.edu.unl.geoasist.identidades.aplicacion;

import java.util.UUID;

public interface IdentityProvider {
  UUID authenticate(String email, String password);
}
