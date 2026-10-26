package ec.edu.unl.geoasist.espacios.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.espacios.dominio.SpaceCode;
import ec.edu.unl.geoasist.espacios.persistencia.SpaceRepository;
import ec.edu.unl.geoasist.espacios.persistencia.SpaceRepository.Row;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageSpaces {
  private static final Set<String> TYPES =
      Set.of("AULA", "LABORATORIO", "AUDITORIO", "EXTERIOR", "OTRO");

  /** Campos nulos = sin cambio en edición; en alta se aplican los valores por defecto. */
  public record Command(
      String code,
      String name,
      String type,
      Double latitude,
      Double longitude,
      Double radius,
      Double precision) {}

  private final SpaceRepository spaces;

  public ManageSpaces(SpaceRepository spaces) {
    this.spaces = spaces;
  }

  @Transactional
  public Map<String, Object> create(UserSession user, Command c) {
    requireWriter(user);
    String code = c.code() == null ? null : c.code().strip();
    String normalized = SpaceCode.normalize(code);
    Row value =
        checked(
            new Row(
                UUID.randomUUID(),
                code,
                blankToNull(c.name()),
                c.type() == null ? "OTRO" : c.type(),
                c.latitude(),
                c.longitude(),
                c.radius() == null ? 20 : c.radius(),
                c.precision() == null ? 30 : c.precision(),
                user.accountId()));
    try {
      var existing = spaces.idByCode(normalized);
      if (existing.isPresent()) throw exists(existing.get());
      spaces.insert(value, normalized);
    } catch (DuplicateKeyException e) {
      throw exists(spaces.idByCode(normalized).orElseThrow());
    }
    spaces.audit(value.id(), user.accountId(), "CREAR_ESPACIO", Map.of(), dto(value, user));
    return dto(value, user);
  }

  @Transactional
  public Map<String, Object> update(UserSession user, UUID id, Command c) {
    requireWriter(user);
    Row old = spaces.find(id).orElseThrow(ApiFailure::missing);
    if (!editable(old, user)) throw ApiFailure.forbidden();
    if ((c.latitude() == null) != (c.longitude() == null)) throw ApiFailure.invalid();
    Row value =
        checked(
            new Row(
                id,
                old.code(),
                c.name() == null ? old.name() : blankToNull(c.name()),
                c.type() == null ? old.type() : c.type(),
                c.latitude() == null ? old.latitude() : c.latitude(),
                c.longitude() == null ? old.longitude() : c.longitude(),
                c.radius() == null ? old.radius() : c.radius(),
                c.precision() == null ? old.precision() : c.precision(),
                old.creator()));
    spaces.update(value);
    spaces.audit(id, user.accountId(), "EDITAR_ESPACIO", dto(old, user), dto(value, user));
    return dto(value, user);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> search(UserSession user, String q, int page, int size) {
    requireWriter(user);
    if (page < 0 || size < 1 || size > 100) throw ApiFailure.invalid();
    String text = q == null ? "" : q.strip();
    String byCode = "%" + escape(text.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT)) + "%";
    String byName = "%" + escape(text) + "%";
    long total = spaces.count(byCode, byName);
    List<Map<String, Object>> items =
        spaces.search(byCode, byName, size, (long) page * size).stream()
            .map(r -> dto(r, user))
            .toList();
    return Map.of("items", items, "page", page, "size", size, "total", total);
  }

  private static String escape(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static void requireWriter(UserSession user) {
    user.requireAnyRole("DOCENTE", "ENCARGADO", "ADMINISTRADOR");
  }

  private static boolean editable(Row r, UserSession u) {
    return u.has("ADMINISTRADOR") || u.accountId().equals(r.creator());
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  private static Row checked(Row r) {
    boolean ok =
        r.name() != null
            && r.name().length() <= 200
            && TYPES.contains(r.type())
            && (r.latitude() == null) == (r.longitude() == null)
            && (r.latitude() == null
                || (Math.abs(r.latitude()) <= 90 && Math.abs(r.longitude()) <= 180))
            && Double.isFinite(r.radius())
            && r.radius() > 0
            && Double.isFinite(r.precision())
            && r.precision() > 0;
    if (!ok) throw ApiFailure.invalid();
    return r;
  }

  private static ApiFailure exists(UUID id) {
    return new ApiFailure(409, "ESPACIO_CODIGO_EXISTE", "Ya existe un espacio con ese código.")
        .with("espacio_id", id);
  }

  private static Map<String, Object> dto(Row r, UserSession u) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", r.id());
    m.put("codigo", r.code());
    m.put("nombre", r.name());
    m.put("tipo", r.type());
    if (r.latitude() != null) {
      m.put("latitud", r.latitude());
      m.put("longitud", r.longitude());
    }
    m.put("radio_defecto_m", r.radius());
    m.put("precision_defecto_m", r.precision());
    m.put("editable", editable(r, u));
    return m;
  }
}
