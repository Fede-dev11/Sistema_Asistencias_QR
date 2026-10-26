package ec.edu.unl.geoasist.identidades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.AcademicAudit;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.dominio.Cedula;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProvisioning {
  private static final Set<String> KNOWN_ROLES =
      Set.of(
          "DOCENTE",
          "ENCARGADO",
          "ESTUDIANTE",
          "DIRECTOR_INSTITUCIONAL",
          "DIRECTOR_CARRERA",
          "ADMINISTRADOR");
  private final JdbcTemplate db;
  private final AcademicAudit audit;
  private final Clock clock;

  public AccountProvisioning(JdbcTemplate db, AcademicAudit audit, Clock clock) {
    this.db = db;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public UUID provision(VerifiedIdentity identity) {
    if (!identity.activo()) throw inactive();
    String cedula;
    try {
      cedula = Cedula.of(identity.cedula()).value();
    } catch (ApiFailure e) {
      throw new ApiFailure(422, "IDENTIDAD_INVALIDA", "La identidad universitaria no es válida.");
    }
    String correo = identity.correo().strip().toLowerCase(Locale.ROOT);
    UUID person =
        db
            .query(
                "SELECT id FROM personas WHERE cedula_normalizada=? FOR UPDATE",
                (rs, n) -> rs.getObject(1, UUID.class),
                cedula)
            .stream()
            .findFirst()
            .orElseGet(
                () -> {
                  UUID id = UUID.randomUUID();
                  db.update(
                      "INSERT INTO personas(id,cedula_normalizada,nombres,tipo) VALUES (?,?,?,'INSTITUCIONAL')",
                      id,
                      cedula,
                      identity.nombres());
                  return id;
                });
    db.update(
        "UPDATE personas SET nombres=?,tipo='INSTITUCIONAL' WHERE id=?",
        identity.nombres(),
        person);
    var existing =
        db.queryForList(
            "SELECT id,identificador,activa FROM cuentas WHERE persona_id=? FOR UPDATE", person);
    UUID account;
    Map<String, Object> before;
    if (existing.isEmpty()) {
      account = UUID.randomUUID();
      before = Map.of();
      db.update(
          "INSERT INTO cuentas(id,persona_id,proveedor,identificador,password_hash,activa,sincronizada_en) VALUES (?,?,'CAS',?,NULL,true,?)",
          account,
          person,
          correo,
          java.sql.Timestamp.from(clock.instant()));
    } else {
      var row = existing.getFirst();
      account = (UUID) row.get("id");
      if (!Boolean.TRUE.equals(row.get("activa"))) throw inactive();
      before = Map.of("identificador", row.get("identificador"));
      db.update(
          "UPDATE cuentas SET proveedor='CAS',identificador=?,password_hash=NULL,debe_cambiar_password=false,sincronizada_en=? WHERE id=?",
          correo,
          java.sql.Timestamp.from(clock.instant()),
          account);
    }
    Set<String> roles = new LinkedHashSet<>();
    identity.roles().stream()
        .map(r -> r.toUpperCase(Locale.ROOT))
        .filter(KNOWN_ROLES::contains)
        .forEach(roles::add);
    db.update("DELETE FROM roles_cuenta WHERE cuenta_id=?", account);
    roles.forEach(
        role -> db.update("INSERT INTO roles_cuenta(cuenta_id,rol) VALUES (?,?)", account, role));
    db.update("DELETE FROM direcciones_carrera WHERE cuenta_id=?", account);
    if (roles.contains("DIRECTOR_CARRERA"))
      for (String code : new LinkedHashSet<>(identity.carreras()))
        db.update(
            "INSERT INTO direcciones_carrera(cuenta_id,carrera_id,vigente) SELECT ?,id,true FROM carreras WHERE codigo=? ON CONFLICT(cuenta_id,carrera_id) DO UPDATE SET vigente=true",
            account,
            code);
    audit.write(
        account,
        "SISTEMA_PROVISION_CUENTA",
        "CUENTA",
        account,
        before,
        Map.of("identificador", correo, "roles", roles, "carreras", identity.carreras()));
    return account;
  }

  private static ApiFailure inactive() {
    return new ApiFailure(403, "CUENTA_INACTIVA", "La cuenta universitaria está inactiva.");
  }
}
