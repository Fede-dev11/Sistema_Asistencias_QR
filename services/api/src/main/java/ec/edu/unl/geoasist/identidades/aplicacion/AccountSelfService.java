package ec.edu.unl.geoasist.identidades.aplicacion;

import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.identidades.persistencia.IdentityRepository;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AccountSelfService {
  private final IdentityRepository accounts;

  public AccountSelfService(IdentityRepository accounts) {
    this.accounts = accounts;
  }

  public Map<String, Object> profile(UserSession user) {
    var row = accounts.accountRow(user.accountId());
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("cuenta_id", user.accountId());
    out.put("persona_id", user.personId());
    out.put("nombres", row.get("nombres"));
    out.put("correo", row.get("identificador"));
    out.put("roles", new TreeSet<>(user.roles()));
    out.put("debe_cambiar_password", user.mustChangePassword());
    out.put("paralelos", accounts.parallels(user.accountId(), user.personId()));
    return out;
  }
}
