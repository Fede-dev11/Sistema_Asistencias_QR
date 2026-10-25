package ec.edu.unl.geoasist.academico.aplicacion;

import ec.edu.unl.geoasist.academico.persistencia.AcademicRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.*;
import ec.edu.unl.geoasist.compartido.aplicacion.IdempotencyService.Receipt;
import ec.edu.unl.geoasist.compartido.dominio.Cedula;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageCareersAndParallels {
  static final int MAX_LIST = 5000;
  private final AcademicRepository repo;
  private final AcademicAudit audit;
  private final IdempotencyService replay;

  public ManageCareersAndParallels(
      AcademicRepository repo, AcademicAudit audit, IdempotencyService replay) {
    this.repo = repo;
    this.audit = audit;
    this.replay = replay;
  }

  static String code(String raw) {
    String v = raw == null ? "" : raw.strip();
    if (v.isEmpty() || v.length() > 40) throw ApiFailure.invalid();
    return v;
  }

  public Map<String, Object> careers(UserSession user) {
    user.requireAnyRole("ADMINISTRADOR");
    return Map.of("items", repo.careers());
  }

  @Transactional
  public Receipt createCareer(UserSession user, UUID key, String codigo, String nombre) {
    user.requireAnyRole("ADMINISTRADOR");
    String c = code(codigo), n = ManagePeopleAndAccounts.name(nombre);
    return replay.run(
        "ADMIN_CARRERA",
        user.accountId().toString(),
        key,
        null,
        Map.of("codigo", c, "nombre", n),
        () -> {
          if (repo.careerByCode(c).isPresent())
            throw new ApiFailure(409, "CARRERA_EXISTE", "Ya existe una carrera con ese código.");
          UUID id = repo.insertCareer(c, n);
          audit.write(
              user.accountId(),
              "CREAR_CARRERA",
              "CARRERA",
              id,
              Map.of(),
              Map.of("codigo", c, "nombre", n));
          return new Receipt(201, repo.career(id).orElseThrow());
        });
  }

  @Transactional
  public Map<String, Object> renameCareer(UserSession user, UUID id, String nombre) {
    user.requireAnyRole("ADMINISTRADOR");
    var before = repo.career(id).orElseThrow(ApiFailure::missing);
    String n = ManagePeopleAndAccounts.name(nombre);
    repo.renameCareer(id, n);
    audit.write(
        user.accountId(),
        "EDITAR_CARRERA",
        "CARRERA",
        id,
        Map.of("nombre", before.get("nombre")),
        Map.of("nombre", n));
    return repo.career(id).orElseThrow();
  }

  public Map<String, Object> parallels(UserSession user, UUID career) {
    user.requireAnyRole("ADMINISTRADOR");
    repo.career(career).orElseThrow(ApiFailure::missing);
    return Map.of("items", repo.parallels(career));
  }

  @Transactional
  public Receipt createParallel(UserSession user, UUID key, UUID career, String codigo) {
    user.requireAnyRole("ADMINISTRADOR");
    String c = code(codigo);
    return replay.run(
        "ADMIN_PARALELO",
        user.accountId().toString(),
        key,
        null,
        Map.of("carrera_id", career, "codigo", c),
        () -> {
          repo.career(career).orElseThrow(ApiFailure::missing);
          if (repo.parallelByCode(career, c).isPresent())
            throw new ApiFailure(409, "PARALELO_EXISTE", "Ya existe ese paralelo en la carrera.");
          UUID id = repo.insertParallel(career, c);
          audit.write(
              user.accountId(),
              "CREAR_PARALELO",
              "PARALELO",
              id,
              Map.of(),
              Map.of("carrera_id", career, "codigo", c));
          return new Receipt(201, Map.of("id", id, "codigo", c, "matriculados", 0, "docentes", 0));
        });
  }

  private static Map<String, String> rejected(String key, String motivo) {
    return Map.of("entrada", key, "motivo", motivo);
  }

  /** Reemplaza los matriculados vigentes; devuelve las cédulas rechazadas con su motivo. */
  @Transactional
  public Map<String, Object> replaceEnrollments(
      UserSession user, UUID parallel, List<String> cedulas) {
    user.requireAnyRole("ADMINISTRADOR");
    if (!repo.parallelExists(parallel)) throw ApiFailure.missing();
    if (cedulas == null || cedulas.size() > MAX_LIST) throw ApiFailure.invalid();
    Set<UUID> accepted = new LinkedHashSet<>();
    List<Map<String, String>> rejections = new ArrayList<>();
    for (String raw : cedulas) {
      try {
        var person = repo.personByCedula(Cedula.of(raw).value());
        if (person.isEmpty()) rejections.add(rejected(raw, "No existe la persona."));
        else if (!repo.hasRole((UUID) person.get().get("id"), "ESTUDIANTE"))
          rejections.add(rejected(raw, "La persona no tiene rol ESTUDIANTE."));
        else accepted.add((UUID) person.get().get("id"));
      } catch (ApiFailure e) {
        rejections.add(rejected(raw, "Cédula inválida."));
      }
    }
    accepted.forEach(p -> repo.enroll(p, parallel));
    repo.retainEnrollments(parallel, accepted);
    audit.write(
        user.accountId(),
        "REEMPLAZAR_MATRICULAS",
        "PARALELO",
        parallel,
        Map.of(),
        Map.of("matriculados", accepted.size(), "rechazadas", rejections.size()));
    return Map.of("matriculados", accepted.size(), "rechazadas", rejections);
  }

  /** Reemplaza los docentes asignados (por correo; cuentas activas DOCENTE o ENCARGADO). */
  @Transactional
  public Map<String, Object> replaceTeachers(
      UserSession user, UUID parallel, List<String> correos) {
    user.requireAnyRole("ADMINISTRADOR");
    if (!repo.parallelExists(parallel)) throw ApiFailure.missing();
    if (correos == null || correos.size() > MAX_LIST) throw ApiFailure.invalid();
    Set<UUID> accepted = new LinkedHashSet<>();
    List<Map<String, String>> rejections = new ArrayList<>();
    for (String raw : correos) {
      var account = repo.accountByEmail(raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT));
      if (account.isEmpty()) {
        rejections.add(rejected(raw, "No existe la cuenta."));
        continue;
      }
      var roles = repo.roles(account.get());
      if (!roles.contains("DOCENTE") && !roles.contains("ENCARGADO"))
        rejections.add(rejected(raw, "La cuenta no tiene rol DOCENTE ni ENCARGADO."));
      else accepted.add(account.get());
    }
    accepted.forEach(a -> repo.assignTeacher(a, parallel));
    repo.retainTeachers(parallel, accepted);
    audit.write(
        user.accountId(),
        "REEMPLAZAR_DOCENTES",
        "PARALELO",
        parallel,
        Map.of(),
        Map.of("docentes", accepted.size(), "rechazadas", rejections.size()));
    return Map.of("docentes", accepted.size(), "rechazadas", rejections);
  }

  public Map<String, Object> myParallels(UserSession user) {
    user.requireAnyRole("DOCENTE", "ENCARGADO");
    return Map.of("items", repo.parallelsOfTeacher(user.accountId()));
  }
}
