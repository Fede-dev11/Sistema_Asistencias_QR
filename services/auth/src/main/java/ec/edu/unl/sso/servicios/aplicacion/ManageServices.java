package ec.edu.unl.sso.servicios.aplicacion;

import ec.edu.unl.sso.compartido.AdminAudit;
import ec.edu.unl.sso.compartido.Secrets;
import ec.edu.unl.sso.servicios.persistencia.ServiceRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicios registrados (FR-010): prefijos de retorno validados con las reglas de ServiceMatcher.
 */
@Service
public class ManageServices {
  private final JdbcClient db;
  private final ServiceRegistry registry;
  private final AdminAudit audit;

  ManageServices(JdbcClient db, ServiceRegistry registry, AdminAudit audit) {
    this.db = db;
    this.registry = registry;
    this.audit = audit;
  }

  /** Separados por líneas o espacios; lanza IllegalArgumentException si alguno no es válido. */
  static List<String> retornos(String raw) {
    List<String> list =
        raw == null
            ? List.of()
            : Arrays.stream(raw.strip().split("\\s+"))
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    if (list.isEmpty() || list.size() > 20) {
      throw new IllegalArgumentException("Indica entre 1 y 20 direcciones de retorno.");
    }
    for (String p : list) {
      if (!ServiceMatcher.validPrefix(p)) {
        throw new IllegalArgumentException("Dirección de retorno no permitida: " + p);
      }
    }
    return list;
  }

  private static String nombre(String raw) {
    String n = raw == null ? "" : raw.strip();
    if (n.isEmpty() || n.length() > 100) {
      throw new IllegalArgumentException("El nombre debe tener entre 1 y 100 caracteres.");
    }
    return n;
  }

  /** Devuelve la clave en claro (solo se muestra una vez). */
  @Transactional
  public String crear(UUID actor, String nombre, String retornos) {
    String n = nombre(nombre);
    List<String> r = retornos(retornos);
    String key = Secrets.random();
    UUID id = UUID.randomUUID();
    try {
      db.sql(
              "INSERT INTO servicios(id, nombre, retornos, clave_hash, activo) VALUES (?,?,?,?,true)")
          .params(id, n, r.toArray(String[]::new), Secrets.sha256Hex(key))
          .update();
    } catch (DuplicateKeyException e) {
      throw new IllegalArgumentException("Ya existe un servicio con ese nombre.");
    }
    audit.registrar(actor, "SERVICIO_CREAR", id, null, Map.of("nombre", n, "retornos", r));
    return key;
  }

  @Transactional
  public void editar(UUID actor, UUID id, String nombre, String retornos, boolean activo) {
    var antes =
        registry.todos().stream()
            .filter(s -> s.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("El servicio no existe."));
    String n = nombre(nombre);
    List<String> r = retornos(retornos);
    try {
      db.sql("UPDATE servicios SET nombre = ?, retornos = ?, activo = ? WHERE id = ?")
          .params(n, r.toArray(String[]::new), activo, id)
          .update();
    } catch (DuplicateKeyException e) {
      throw new IllegalArgumentException("Ya existe un servicio con ese nombre.");
    }
    audit.registrar(
        actor,
        "SERVICIO_EDITAR",
        id,
        Map.of("nombre", antes.nombre(), "retornos", antes.retornos(), "activo", antes.activo()),
        Map.of("nombre", n, "retornos", r, "activo", activo));
  }

  /** Genera una clave nueva (la anterior deja de valer) y la devuelve en claro. */
  @Transactional
  public String regenerarClave(UUID actor, UUID id) {
    String key = Secrets.random();
    int n =
        db.sql("UPDATE servicios SET clave_hash = ? WHERE id = ?")
            .params(Secrets.sha256Hex(key), id)
            .update();
    if (n == 0) throw new IllegalArgumentException("El servicio no existe.");
    audit.registrar(actor, "SERVICIO_REGENERAR_CLAVE", id, null, null);
    return key;
  }
}
