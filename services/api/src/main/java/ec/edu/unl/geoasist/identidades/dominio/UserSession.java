package ec.edu.unl.geoasist.identidades.dominio;

import java.util.Set;
import java.util.UUID;

public record UserSession(
    UUID sessionId, UUID accountId, UUID personId, Set<String> roles, boolean mustChangePassword) {
  public UserSession {
    roles = Set.copyOf(roles);
  }

  public UserSession(UUID sessionId, UUID accountId, UUID personId, Set<String> roles) {
    this(sessionId, accountId, personId, roles, false);
  }

  public boolean has(String role) {
    return roles.contains(role);
  }

  public void requireAnyRole(String... allowed) {
    for (String role : allowed) if (has(role)) return;
    throw ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure.forbidden();
  }

  public boolean responsibleRole() {
    return has("DOCENTE") || has("ENCARGADO");
  }
}
