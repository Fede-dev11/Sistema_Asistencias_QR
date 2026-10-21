package ec.edu.unl.geoasist.identidades.aplicacion;

import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAssociationService {
  private final AttendanceLocks locks;
  private final JdbcTemplate db;

  public AccountAssociationService(AttendanceLocks locks, JdbcTemplate db) {
    this.locks = locks;
    this.db = db;
  }

  /** Puerto interno: no añade autorregistro ni endpoint de alta de cuenta. */
  @Transactional
  public UUID associate(UUID person, String provider, String identifier, String encodedPassword) {
    locks.person(person);
    UUID account = UUID.randomUUID();
    db.update(
        "INSERT INTO cuentas VALUES (?,?,?,?,?,true)",
        account,
        person,
        provider,
        identifier,
        encodedPassword);
    return account;
  }
}
