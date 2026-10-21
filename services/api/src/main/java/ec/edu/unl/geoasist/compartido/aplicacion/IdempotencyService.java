package ec.edu.unl.geoasist.compartido.aplicacion;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class IdempotencyService {
  public record Receipt(int status, Map<String, Object> body) {}

  private final JdbcTemplate db;
  private final OpaqueSecrets secrets;
  private final ObjectMapper json;
  private final Clock clock;

  public IdempotencyService(
      JdbcTemplate db, OpaqueSecrets secrets, ObjectMapper json, Clock clock) {
    this.db = db;
    this.secrets = secrets;
    this.json = json;
    this.clock = clock;
  }

  private static Object canonical(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> sorted = new TreeMap<>();
      map.forEach((k, v) -> sorted.put(k.toString(), canonical(v)));
      return sorted;
    }
    if (value instanceof List<?> list)
      return list.stream().map(IdempotencyService::canonical).toList();
    return value;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public Receipt run(
      String operation,
      String actor,
      UUID key,
      UUID activity,
      Object canonicalPayload,
      Supplier<Receipt> action) {
    String fingerprint =
        secrets.hmac(
            json.writeValueAsString(
                canonical(
                    json.readValue(json.writeValueAsString(canonicalPayload), Object.class))));
    db.queryForList(
        "SELECT pg_advisory_xact_lock(hashtextextended(?,0))", operation + ":" + actor + ":" + key);
    var existing =
        db.queryForList(
            "SELECT huella,estado_http,resultado::text FROM idempotencias WHERE operacion=? AND actor=? AND clave=? AND vence_en>?",
            operation,
            actor,
            key,
            java.sql.Timestamp.from(clock.instant()));
    if (!existing.isEmpty()) {
      var row = existing.getFirst();
      if (!fingerprint.equals(row.get("huella")))
        throw new ApiFailure(
            409, "IDEMPOTENCIA_CONFLICTO", "La clave ya fue usada con otros datos.");
      return new Receipt(
          ((Integer) row.get("estado_http")) == 201 ? 200 : (Integer) row.get("estado_http"),
          json.readValue(
              (String) row.get("resultado"), new TypeReference<Map<String, Object>>() {}));
    }
    db.update(
        "DELETE FROM idempotencias WHERE operacion=? AND actor=? AND clave=?",
        operation,
        actor,
        key);
    Receipt result = action.get();
    Instant now = clock.instant();
    db.update(
        "INSERT INTO idempotencias(operacion,actor,clave,huella,actividad_id,estado_http,resultado,creada_en,vence_en,actor_cuenta_id,inicio_id) VALUES (?,?,?,?,?,?,?::jsonb,?,?,?,?)",
        operation,
        actor,
        key,
        fingerprint,
        activity,
        result.status(),
        json.writeValueAsString(result.body()),
        java.sql.Timestamp.from(now),
        java.sql.Timestamp.from(now.plus(Duration.ofHours(24))),
        actor.startsWith("guest:") ? null : UUID.fromString(actor),
        actor.startsWith("guest:") ? UUID.fromString(actor.substring(6)) : null);
    return result;
  }
}
