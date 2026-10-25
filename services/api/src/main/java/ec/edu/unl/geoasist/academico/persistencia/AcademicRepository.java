package ec.edu.unl.geoasist.academico.persistencia;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AcademicRepository {
  private static final String PERSON =
      "SELECT p.id,p.cedula_normalizada AS cedula,p.nombres,p.tipo,c.id AS cuenta_id,c.identificador AS correo,c.activa,"
          + "(SELECT string_agg(rol,',' ORDER BY rol) FROM roles_cuenta r WHERE r.cuenta_id=c.id) AS roles "
          + "FROM personas p LEFT JOIN cuentas c ON c.persona_id=p.id ";
  private final JdbcTemplate db;

  public AcademicRepository(JdbcTemplate db) {
    this.db = db;
  }

  private static Map<String, Object> shape(Map<String, Object> row) {
    Map<String, Object> out = new LinkedHashMap<>();
    for (String k : List.of("id", "cedula", "nombres", "tipo")) out.put(k, row.get(k));
    if (row.get("cuenta_id") == null) out.put("cuenta", null);
    else {
      String roles = (String) row.get("roles");
      Map<String, Object> account = new LinkedHashMap<>();
      account.put("id", row.get("cuenta_id"));
      account.put("correo", row.get("correo"));
      account.put("activa", row.get("activa"));
      account.put("roles", roles == null ? List.of() : List.of(roles.split(",")));
      out.put("cuenta", account);
    }
    return out;
  }

  public List<Map<String, Object>> people(String q, int limit, int offset) {
    return db
        .queryForList(
            PERSON
                + "WHERE position(lower(?) in lower(p.nombres))>0 OR position(? in p.cedula_normalizada)>0 ORDER BY p.nombres,p.id LIMIT ? OFFSET ?",
            q,
            q,
            limit,
            offset)
        .stream()
        .map(AcademicRepository::shape)
        .toList();
  }

  public long countPeople(String q) {
    return db.queryForObject(
        "SELECT count(*) FROM personas WHERE position(lower(?) in lower(nombres))>0 OR position(? in cedula_normalizada)>0",
        Long.class,
        q,
        q);
  }

  public Optional<Map<String, Object>> person(UUID id) {
    return db.queryForList(PERSON + "WHERE p.id=?", id).stream()
        .findFirst()
        .map(AcademicRepository::shape);
  }

  public Optional<Map<String, Object>> personByCedula(String cedula) {
    return db.queryForList(PERSON + "WHERE p.cedula_normalizada=?", cedula).stream()
        .findFirst()
        .map(AcademicRepository::shape);
  }

  public UUID insertPerson(String cedula, String nombres) {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO personas VALUES (?,?,?,'INSTITUCIONAL')", id, cedula, nombres);
    return id;
  }

  public void renamePerson(UUID id, String nombres) {
    db.update("UPDATE personas SET nombres=? WHERE id=?", nombres, id);
  }

  public Optional<UUID> accountByEmail(String email) {
    return db.queryForList("SELECT id FROM cuentas WHERE identificador=?", UUID.class, email)
        .stream()
        .findFirst();
  }

  public Optional<UUID> personOfEmail(String email) {
    return db.queryForList("SELECT persona_id FROM cuentas WHERE identificador=?", UUID.class, email)
        .stream()
        .findFirst();
  }

  public UUID personIdOfAccount(UUID account) {
    return db.queryForObject("SELECT persona_id FROM cuentas WHERE id=?", UUID.class, account);
  }

  public boolean accountExists(UUID id) {
    return !db.queryForList("SELECT 1 FROM cuentas WHERE id=?", id).isEmpty();
  }

  public UUID insertAccount(UUID person, String email, String hash) {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO cuentas VALUES (?,?,'DEV',?,?,true)", id, person, email, hash);
    return id;
  }

  public Set<String> roles(UUID account) {
    return new TreeSet<>(
        db.queryForList("SELECT rol FROM roles_cuenta WHERE cuenta_id=?", String.class, account));
  }

  public boolean addRole(UUID account, String role) {
    return db.update(
            "INSERT INTO roles_cuenta VALUES (?,?) ON CONFLICT DO NOTHING", account, role)
        > 0;
  }

  public void replaceRoles(UUID account, Set<String> roles) {
    db.update("DELETE FROM roles_cuenta WHERE cuenta_id=?", account);
    roles.forEach(r -> addRole(account, r));
  }

  public void setActive(UUID account, boolean active) {
    db.update("UPDATE cuentas SET activa=? WHERE id=?", active, account);
  }

  public void setPassword(UUID account, String hash) {
    db.update("UPDATE cuentas SET password_hash=? WHERE id=?", hash, account);
  }

  public void requirePasswordChange(UUID account) {
    db.update("UPDATE cuentas SET debe_cambiar_password=true WHERE id=?", account);
  }

  public void revokeSessions(UUID account, java.sql.Timestamp now) {
    db.update(
        "UPDATE sesiones_acceso SET revocada_en=? WHERE cuenta_id=? AND revocada_en IS NULL",
        now,
        account);
  }

  public boolean hasRole(UUID person, String role) {
    return !db.queryForList(
            "SELECT 1 FROM roles_cuenta r JOIN cuentas c ON c.id=r.cuenta_id WHERE c.persona_id=? AND r.rol=?",
            person,
            role)
        .isEmpty();
  }

  // Carreras y paralelos
  public List<Map<String, Object>> careers() {
    return db.queryForList("SELECT id,codigo,nombre FROM carreras ORDER BY codigo");
  }

  public Optional<Map<String, Object>> career(UUID id) {
    return db.queryForList("SELECT id,codigo,nombre FROM carreras WHERE id=?", id).stream()
        .findFirst();
  }

  public Optional<UUID> careerByCode(String code) {
    return db.queryForList("SELECT id FROM carreras WHERE codigo=?", UUID.class, code).stream()
        .findFirst();
  }

  public UUID insertCareer(String code, String name) {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO carreras VALUES (?,?,?)", id, code, name);
    return id;
  }

  public void renameCareer(UUID id, String name) {
    db.update("UPDATE carreras SET nombre=? WHERE id=?", name, id);
  }

  public List<Map<String, Object>> parallels(UUID career) {
    return db.queryForList(
        "SELECT p.id,p.codigo,(SELECT count(*) FROM matriculas m WHERE m.paralelo_id=p.id AND m.vigente) AS matriculados,"
            + "(SELECT count(*) FROM docentes_paralelo d WHERE d.paralelo_id=p.id AND d.vigente) AS docentes "
            + "FROM paralelos p WHERE p.carrera_id=? ORDER BY p.codigo",
        career);
  }

  public Optional<UUID> parallelByCode(UUID career, String code) {
    return db.queryForList(
            "SELECT id FROM paralelos WHERE carrera_id=? AND codigo=?", UUID.class, career, code)
        .stream()
        .findFirst();
  }

  public boolean parallelExists(UUID id) {
    return !db.queryForList("SELECT 1 FROM paralelos WHERE id=?", id).isEmpty();
  }

  public UUID insertParallel(UUID career, String code) {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO paralelos VALUES (?,?,?)", id, career, code);
    return id;
  }

  // Matrículas y docentes: 1 = creada, 2 = reactivada, 0 = sin cambios
  public int enroll(UUID person, UUID parallel) {
    if (db.update(
            "INSERT INTO matriculas VALUES (?,?,true) ON CONFLICT DO NOTHING", person, parallel)
        > 0) return 1;
    return db.update(
            "UPDATE matriculas SET vigente=true WHERE persona_id=? AND paralelo_id=? AND NOT vigente",
            person,
            parallel)
        > 0
        ? 2
        : 0;
  }

  public void retainEnrollments(UUID parallel, Collection<UUID> keep) {
    db.update(
        "UPDATE matriculas SET vigente=false WHERE paralelo_id=? AND vigente AND NOT (persona_id = ANY(?))",
        parallel,
        keep.toArray(new UUID[0]));
  }

  public void assignTeacher(UUID account, UUID parallel) {
    db.update(
        "INSERT INTO docentes_paralelo VALUES (?,?,true) ON CONFLICT(cuenta_id,paralelo_id) DO UPDATE SET vigente=true",
        account,
        parallel);
  }

  public void retainTeachers(UUID parallel, Collection<UUID> keep) {
    db.update(
        "UPDATE docentes_paralelo SET vigente=false WHERE paralelo_id=? AND vigente AND NOT (cuenta_id = ANY(?))",
        parallel,
        keep.toArray(new UUID[0]));
  }

  public List<Map<String, Object>> parallelsOfTeacher(UUID account) {
    return db.queryForList(
        "SELECT p.id AS paralelo_id,p.codigo,c.id AS carrera_id,c.codigo AS carrera_codigo,c.nombre AS carrera_nombre,"
            + "(SELECT count(*) FROM matriculas m WHERE m.paralelo_id=p.id AND m.vigente) AS matriculados "
            + "FROM docentes_paralelo d JOIN paralelos p ON p.id=d.paralelo_id JOIN carreras c ON c.id=p.carrera_id "
            + "WHERE d.cuenta_id=? AND d.vigente ORDER BY c.codigo,p.codigo",
        account);
  }
}
