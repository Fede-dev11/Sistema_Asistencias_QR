package ec.edu.unl.geoasist.identidades.dominio;

import java.util.Set;
import java.util.UUID;

public record UserSession(UUID sessionId, UUID accountId, UUID personId, Set<String> roles) {
  public UserSession {
    roles = Set.copyOf(roles);
  }

  public boolean has(String role) {
    return roles.contains(role);
  }

  public boolean responsibleRole() {
    return has("DOCENTE") || has("ENCARGADO");
  }
}
