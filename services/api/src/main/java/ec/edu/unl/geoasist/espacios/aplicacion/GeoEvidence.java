package ec.edu.unl.geoasist.espacios.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.espacios.persistencia.SpaceRepository;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeoEvidence {
  public record Command(
      String reference,
      Instant performedAt,
      BigDecimal falsePositives,
      BigDecimal falseNegatives) {}

  private final JdbcTemplate db;
  private final Clock clock;
  private final SpaceRepository spaces;

  public GeoEvidence(JdbcTemplate db, Clock clock, SpaceRepository spaces) {
    this.db = db;
    this.clock = clock;
    this.spaces = spaces;
  }

  @Transactional
  public Map<String, Object> create(UserSession user, UUID spaceId, Command command) {
    user.requireAnyRole("ADMINISTRADOR");
    String reference = command.reference() == null ? null : command.reference().strip();
    Instant now = clock.instant();
    validate(reference, command, now);
    var space =
        db
            .query(
                "SELECT latitud,longitud FROM espacios WHERE id=? FOR UPDATE",
                (row, n) -> new Object[] {row.getObject(1), row.getObject(2)},
                spaceId)
            .stream()
            .findFirst()
            .orElseThrow(ApiFailure::missing);
    if (space[0] == null || space[1] == null)
      throw new ApiFailure(
          409, "ESPACIO_SIN_UBICACION", "El espacio no tiene una ubicación registrada.");
    int version =
        db.queryForObject(
            "SELECT coalesce(max(version),0)+1 FROM evidencias_geograficas WHERE espacio_id=?",
            Integer.class,
            spaceId);
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO evidencias_geograficas(id,espacio_id,version,referencia,realizada_en,responsable_id,falsos_positivos,falsos_negativos,verificada_en) VALUES (?,?,?,?,?,?,?,?,?)",
        id,
        spaceId,
        version,
        reference,
        Timestamp.from(command.performedAt()),
        user.accountId(),
        command.falsePositives(),
        command.falseNegatives(),
        Timestamp.from(now));
    Map<String, Object> result =
        dto(
            id,
            spaceId,
            version,
            reference,
            command.performedAt(),
            user.accountId(),
            command.falsePositives(),
            command.falseNegatives(),
            now);
    spaces.audit(spaceId, user.accountId(), "REGISTRAR_EVIDENCIA_GEOGRAFICA", Map.of(), result);
    return result;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(UserSession user, UUID spaceId) {
    user.requireAnyRole("DOCENTE", "ENCARGADO", "ADMINISTRADOR");
    if (spaces.find(spaceId).isEmpty()) throw ApiFailure.missing();
    return db.query(
        "SELECT id,version,referencia,realizada_en,responsable_id,falsos_positivos,falsos_negativos,verificada_en FROM evidencias_geograficas WHERE espacio_id=? ORDER BY version DESC",
        (row, n) ->
            dto(
                row.getObject("id", UUID.class),
                spaceId,
                row.getInt("version"),
                row.getString("referencia"),
                row.getTimestamp("realizada_en").toInstant(),
                row.getObject("responsable_id", UUID.class),
                row.getBigDecimal("falsos_positivos"),
                row.getBigDecimal("falsos_negativos"),
                row.getTimestamp("verificada_en").toInstant()),
        spaceId);
  }

  private static void validate(String reference, Command command, Instant now) {
    if (reference == null || reference.isEmpty() || reference.length() > 500)
      throw new ApiFailure(
          400, "SOLICITUD_INVALIDA", "La referencia debe tener entre 1 y 500 caracteres.");
    if (command.performedAt() == null || command.performedAt().isAfter(now))
      throw new ApiFailure(
          400, "SOLICITUD_INVALIDA", "La fecha de realización no puede estar en el futuro.");
    if (!rate(command.falsePositives()) || !rate(command.falseNegatives()))
      throw new ApiFailure(400, "SOLICITUD_INVALIDA", "Las tasas deben estar entre 0 y 1.");
  }

  private static boolean rate(BigDecimal value) {
    return value != null
        && value.compareTo(BigDecimal.ZERO) >= 0
        && value.compareTo(BigDecimal.ONE) <= 0;
  }

  private static Map<String, Object> dto(
      UUID id,
      UUID spaceId,
      int version,
      String reference,
      Instant performedAt,
      UUID responsibleId,
      BigDecimal falsePositives,
      BigDecimal falseNegatives,
      Instant verifiedAt) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("id", id);
    value.put("espacio_id", spaceId);
    value.put("version", version);
    value.put("referencia", reference);
    value.put("realizada_en", performedAt);
    value.put("responsable_id", responsibleId);
    value.put("falsos_positivos", falsePositives);
    value.put("falsos_negativos", falseNegatives);
    value.put("verificada_en", verifiedAt);
    return value;
  }
}
