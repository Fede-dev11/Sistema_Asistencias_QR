package ec.edu.unl.geoasist.academico.aplicacion;

import ec.edu.unl.geoasist.academico.persistencia.AcademicRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.*;
import ec.edu.unl.geoasist.compartido.aplicacion.AcademicAudit;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.compartido.dominio.Cedula;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.time.Clock;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagePeopleAndAccounts {
  public static final Set<String> ROLES =
      Set.of(
          "DOCENTE",
          "ENCARGADO",
          "ESTUDIANTE",
          "DIRECTOR_INSTITUCIONAL",
          "DIRECTOR_CARRERA",
          "ADMINISTRADOR");
  public static final java.util.regex.Pattern EMAIL =
      java.util.regex.Pattern.compile("[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]+");

  private final AcademicRepository repo;
  private final AcademicAudit audit;
  private final IdempotencyService replay;
  private final Clock clock;

  public ManagePeopleAndAccounts(
      AcademicRepository repo, AcademicAudit audit, IdempotencyService replay, Clock clock) {
    this.repo = repo;
    this.audit = audit;
    this.replay = replay;
    this.clock = clock;
  }

  public static String name(String raw) {
    String v = raw == null ? "" : raw.strip();
    if (v.isEmpty() || v.length() > 200) throw ApiFailure.invalid();
    return v;
  }

  public static String email(String raw) {
    String v = raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
    if (v.length() > 254 || !EMAIL.matcher(v).matches()) throw ApiFailure.invalid();
    return v;
  }

  public Map<String, Object> list(UserSession user, String q, int page, int size) {
    user.requireAnyRole("ADMINISTRADOR");
    if (page < 0 || size < 1 || size > 100) throw ApiFailure.invalid();
    String term = q == null ? "" : q.strip();
    return Map.of(
        "items",
        repo.people(term, size, page * size),
        "page",
        page,
        "size",
        size,
        "total",
        repo.countPeople(term));
  }

  public Map<String, Object> person(UserSession user, UUID id) {
    user.requireAnyRole("ADMINISTRADOR");
    return repo.person(id).orElseThrow(ApiFailure::missing);
  }

  @Transactional
  public Receipt createPerson(UserSession user, UUID key, String cedula, String nombres) {
    user.requireAnyRole("ADMINISTRADOR");
    String id = Cedula.of(cedula).value();
    String full = name(nombres);
    return replay.run(
        "ADMIN_PERSONA",
        user.accountId().toString(),
        key,
        null,
        Map.of("cedula", id, "nombres", full),
        () -> {
          if (repo.personByCedula(id).isPresent())
            throw new ApiFailure(409, "PERSONA_EXISTE", "Ya existe una persona con esa cédula.");
          UUID created = repo.insertPerson(id, full);
          audit.write(
              user.accountId(),
              "CREAR_PERSONA",
              "PERSONA",
              created,
              Map.of(),
              Map.of("nombres", full));
          return new Receipt(201, repo.person(created).orElseThrow());
        });
  }

  @Transactional
  public Map<String, Object> rename(UserSession user, UUID id, String nombres) {
    user.requireAnyRole("ADMINISTRADOR");
    var before = repo.person(id).orElseThrow(ApiFailure::missing);
    String full = name(nombres);
    repo.renamePerson(id, full);
    audit.write(
        user.accountId(),
        "EDITAR_PERSONA",
        "PERSONA",
        id,
        Map.of("nombres", before.get("nombres")),
        Map.of("nombres", full));
    return repo.person(id).orElseThrow();
  }

  static Set<String> validRoles(Collection<String> roles) {
    if (roles == null || roles.isEmpty() || !ROLES.containsAll(roles)) throw ApiFailure.invalid();
    return new TreeSet<>(roles);
  }

  @Transactional
  public Receipt createAccount(
      UserSession user, UUID key, UUID personId, String correo, List<String> roles) {
    user.requireAnyRole("ADMINISTRADOR");
    String mail = email(correo);
    Set<String> assigned = validRoles(roles);
    return replay.run(
        "ADMIN_CUENTA",
        user.accountId().toString(),
        key,
        null,
        Map.of("persona_id", personId, "correo", mail, "roles", assigned),
        () -> {
          var person = repo.person(personId).orElseThrow(ApiFailure::missing);
          if (person.get("cuenta") != null)
            throw new ApiFailure(409, "PERSONA_CON_CUENTA", "La persona ya tiene una cuenta.");
          if (repo.accountByEmail(mail).isPresent())
            throw new ApiFailure(409, "CORREO_EXISTE", "El correo ya está en uso.");
          UUID account;
          try {
            account = repo.insertAccount(personId, mail);
          } catch (DuplicateKeyException e) {
            throw new ApiFailure(409, "CORREO_EXISTE", "El correo ya está en uso.");
          }
          repo.replaceRoles(account, assigned);
          audit.write(
              user.accountId(),
              "CREAR_CUENTA",
              "CUENTA",
              account,
              Map.of(),
              Map.of("persona_id", personId, "correo", mail, "roles", assigned));
          return new Receipt(201, repo.person(personId).orElseThrow());
        });
  }

  @Transactional
  public Map<String, Object> updateAccount(
      UserSession user, UUID accountId, List<String> roles, Boolean activa) {
    user.requireAnyRole("ADMINISTRADOR");
    if (!repo.accountExists(accountId)) throw ApiFailure.missing();
    if (roles == null && activa == null) throw ApiFailure.invalid();
    boolean self = accountId.equals(user.accountId());
    Set<String> before = repo.roles(accountId);
    Map<String, Object> old = new LinkedHashMap<>(), next = new LinkedHashMap<>();
    if (roles != null) {
      Set<String> assigned = validRoles(roles);
      if (self && !assigned.contains("ADMINISTRADOR")) throw ApiFailure.conflict();
      repo.replaceRoles(accountId, assigned);
      old.put("roles", before);
      next.put("roles", assigned);
    }
    if (activa != null) {
      if (self && !activa) throw ApiFailure.conflict();
      repo.setActive(accountId, activa);
      next.put("activa", activa);
      if (!activa) repo.revokeSessions(accountId, java.sql.Timestamp.from(clock.instant()));
    }
    audit.write(user.accountId(), "EDITAR_CUENTA", "CUENTA", accountId, old, next);
    return repo.person(repo.personIdOfAccount(accountId)).orElseThrow();
  }
}
