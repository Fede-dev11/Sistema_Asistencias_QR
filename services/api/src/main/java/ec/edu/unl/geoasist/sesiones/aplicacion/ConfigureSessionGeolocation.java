package ec.edu.unl.geoasist.sesiones.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuditWriter;
import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuthorization;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.persistencia.AttendanceLocks;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.sesiones.dominio.GeographicCapability;
import ec.edu.unl.geoasist.sesiones.persistencia.SessionRepository;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigureSessionGeolocation {
  public record Command(
      boolean enabled, Double radius, Double maximumPrecision, Integer maximumAgeSeconds) {}

  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityRepository activities;
  private final ActivityAuthorization scope;
  private final SessionRepository sessions;
  private final AttendanceLocks locks;
  private final GeographicCapability proximity;
  private final ActivityAuditWriter audit;

  public ConfigureSessionGeolocation(
      JdbcTemplate db,
      Clock clock,
      ActivityRepository activities,
      ActivityAuthorization scope,
      SessionRepository sessions,
      AttendanceLocks locks,
      GeographicCapability proximity,
      ActivityAuditWriter audit) {
    this.db = db;
    this.clock = clock;
    this.activities = activities;
    this.scope = scope;
    this.sessions = sessions;
    this.locks = locks;
    this.proximity = proximity;
    this.audit = audit;
  }

  @Transactional
  public Map<String, Object> configure(UserSession user, UUID sessionId, Command command) {
    UUID activityId = activities.activityOfSession(sessionId);
    scope.write(user, activityId);
    locks.activity(activityId, false);
    locks.session(sessionId);
    var activity = scope.write(user, activityId);
    var session = sessions.find(sessionId);
    if (!activity.state().equals("ABIERTA") || !session.state().equals("PROGRAMADA"))
      throw ApiFailure.conflict();

    validate(command);
    UUID evidence = null;
    String mode = "OFF";
    if (command.enabled()) {
      if (!proximity.available() || !proximity.configured(sessionId))
        throw new ApiFailure(
            409,
            "CAPACIDAD_GPS_NO_DISPONIBLE",
            "La capacidad geográfica no está disponible para esta sesión.");
      evidence =
          db
              .query(
                  "SELECT eg.id FROM sesiones s JOIN evidencias_geograficas eg ON eg.espacio_id=s.espacio_id WHERE s.id=? ORDER BY eg.version DESC LIMIT 1",
                  (row, number) -> row.getObject(1, UUID.class),
                  sessionId)
              .stream()
              .findFirst()
              .orElse(null);
      mode = evidence == null ? "REVIEW_ONLY" : "AUTO";
    }

    Map<String, Object> before = current(sessionId);
    db.update(
        "INSERT INTO configuracion_geografica(sesion_id,habilitada,radio_m,precision_max_m,antiguedad_max_s,modo,evidencia_id,configurada_por,configurada_en) VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT(sesion_id) DO UPDATE SET habilitada=EXCLUDED.habilitada,radio_m=EXCLUDED.radio_m,precision_max_m=EXCLUDED.precision_max_m,antiguedad_max_s=EXCLUDED.antiguedad_max_s,modo=EXCLUDED.modo,evidencia_id=EXCLUDED.evidencia_id,configurada_por=EXCLUDED.configurada_por,configurada_en=EXCLUDED.configurada_en",
        sessionId,
        command.enabled(),
        command.enabled() ? command.radius() : null,
        command.enabled() ? command.maximumPrecision() : null,
        command.enabled() ? command.maximumAgeSeconds() : null,
        mode,
        evidence,
        user.accountId(),
        java.sql.Timestamp.from(clock.instant()));
    Map<String, Object> after = settings(command);
    audit.write(
        activityId,
        user.accountId(),
        "CONFIGURAR_GEOLOCALIZACION",
        null,
        Map.of("configuracion_geografica", before),
        Map.of("configuracion_geografica", after));
    return status(after, proximity.available(), mode, evidence != null);
  }

  private static void validate(Command value) {
    if (!value.enabled()) {
      if (value.radius() != null
          || value.maximumPrecision() != null
          || value.maximumAgeSeconds() != null) throw ApiFailure.invalid();
      return;
    }
    if (value.radius() == null
        || !Double.isFinite(value.radius())
        || value.radius() <= 0
        || value.maximumPrecision() == null
        || !Double.isFinite(value.maximumPrecision())
        || value.maximumPrecision() <= 0
        || value.maximumAgeSeconds() == null
        || value.maximumAgeSeconds() < 1) throw ApiFailure.invalid();
  }

  private Map<String, Object> current(UUID session) {
    return db
        .query(
            "SELECT habilitada,radio_m,precision_max_m,antiguedad_max_s FROM configuracion_geografica WHERE sesion_id=?",
            (row, number) -> {
              Map<String, Object> value = new LinkedHashMap<>();
              value.put("habilitada", row.getBoolean(1));
              if (row.getBoolean(1)) {
                value.put("radio_m", row.getDouble(2));
                value.put("precision_max_m", row.getDouble(3));
                value.put("antiguedad_max_s", row.getInt(4));
              }
              return value;
            },
            session)
        .stream()
        .findFirst()
        .orElse(Map.of("habilitada", false));
  }

  private static Map<String, Object> settings(Command command) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("habilitada", command.enabled());
    if (command.enabled()) {
      value.put("radio_m", command.radius());
      value.put("precision_max_m", command.maximumPrecision());
      value.put("antiguedad_max_s", command.maximumAgeSeconds());
    }
    return value;
  }

  private static Map<String, Object> status(
      Map<String, Object> config, boolean capability, String mode, boolean evidence) {
    return Map.of(
        "configuracion",
        config,
        "capacidad_disponible",
        capability,
        "modo_efectivo",
        switch (mode) {
          case "REVIEW_ONLY" -> "PILOTO_REVISION";
          case "AUTO" -> "VALIDACION_AUTOMATICA";
          default -> "DESACTIVADA";
        },
        "evidencia_campo_verificada",
        evidence);
  }
}
