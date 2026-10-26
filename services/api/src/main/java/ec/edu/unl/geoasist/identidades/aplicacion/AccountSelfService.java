package ec.edu.unl.geoasist.identidades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.identidades.persistencia.IdentityRepository;
import java.time.Clock;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountSelfService {
  private final IdentityRepository accounts;
  private final PasswordEncoder encoder;
  private final RequestBudget budget;
  private final Clock clock;

  public AccountSelfService(
      IdentityRepository accounts, PasswordEncoder encoder, RequestBudget budget, Clock clock) {
    this.accounts = accounts;
    this.encoder = encoder;
    this.budget = budget;
    this.clock = clock;
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

  @Transactional
  public void changePassword(UserSession user, String current, String next) {
    if (current == null || next == null) throw ApiFailure.invalid();
    var row = accounts.accountRow(user.accountId());
    String hash = (String) row.get("password_hash");
    if (hash == null || !encoder.matches(current, hash)) {
      budget.check("login-account:" + row.get("identificador"), 5);
      throw new ApiFailure(
          400, "PASSWORD_ACTUAL_INCORRECTA", "La contraseña actual no es correcta.");
    }
    if (next.length() < 12 || next.length() > 128 || next.equals(current))
      throw new ApiFailure(
          400,
          "PASSWORD_DEBIL",
          "La nueva contraseña debe tener 12 a 128 caracteres y ser distinta.");
    accounts.setPassword(user.accountId(), encoder.encode(next));
    accounts.revokeOthers(user.accountId(), user.sessionId(), clock.instant());
  }
}
