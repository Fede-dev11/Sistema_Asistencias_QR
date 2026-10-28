package ec.edu.unl.sso.servicios.persistencia;

import ec.edu.unl.sso.compartido.Secrets;
import java.sql.Array;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Servicios registrados (tabla servicios). */
@Repository
public class ServiceRegistry {
  /** retornos: prefijos de URL de retorno permitidos. */
  public record Servicio(UUID id, String nombre, List<String> retornos, boolean activo) {}

  private final JdbcClient db;

  ServiceRegistry(JdbcClient db) {
    this.db = db;
  }

  public List<Servicio> activos() {
    return todos().stream().filter(Servicio::activo).toList();
  }

  public List<Servicio> todos() {
    return db.sql("SELECT id, nombre, retornos, activo FROM servicios ORDER BY nombre")
        .query(
            (rs, i) -> {
              Array a = rs.getArray("retornos");
              return new Servicio(
                  rs.getObject("id", UUID.class),
                  rs.getString("nombre"),
                  List.of((String[]) a.getArray()),
                  rs.getBoolean("activo"));
            })
        .list();
  }

  public Optional<Servicio> activoPorId(UUID id) {
    return activos().stream().filter(s -> s.id().equals(id)).findFirst();
  }

  /** Servicio activo cuya clave (X-Service-Key) tiene este hash. */
  public Optional<UUID> idPorClave(String key) {
    return db.sql("SELECT id FROM servicios WHERE activo AND clave_hash = ? LIMIT 1")
        .param(Secrets.sha256Hex(key))
        .query(UUID.class)
        .optional();
  }
}
