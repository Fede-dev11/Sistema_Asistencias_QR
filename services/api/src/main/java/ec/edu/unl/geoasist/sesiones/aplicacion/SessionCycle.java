package ec.edu.unl.geoasist.sesiones.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuditWriter;
import ec.edu.unl.geoasist.actividades.aplicacion.SpaceMethodRules;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.aplicacion.RecordAbsences;
import ec.edu.unl.geoasist.sesiones.persistencia.SessionRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ciclo automático (FR-010/FR-011): inicia y cierra sesiones; cada transición en su transacción.
 */
@Service
public class SessionCycle {
  static final long RETRY_SECONDS = 60;
  private static final String PICK =
      """
      SELECT s.id,s.actividad_id,s.espacio_id,s.metodo_verificacion
      FROM sesiones s JOIN actividades a ON a.id=s.actividad_id
      WHERE a.estado='ABIERTA' AND s.estado=? AND %s
      ORDER BY s.ventana_inicio,s.id LIMIT 1 FOR UPDATE OF s SKIP LOCKED
      """;
  private static final String DUE_START =
      "a.ciclo_automatico AND s.ventana_inicio<=? AND s.ventana_fin>? AND (s.siguiente_intento_en IS NULL OR s.siguiente_intento_en<=?)";
  private static final String DUE_CLOSE = "a.ciclo_automatico AND s.ventana_fin<=?";
  private static final String DUE_LATE = "s.ventana_fin + interval '2 hours'<=?";

  private final JdbcTemplate db;
  private final Clock clock;
  private final TransactionTemplate tx;
  private final ActivityAuditWriter audit;
  private final RecordAbsences absences;
  private final SpaceMethodRules rules;
  private final SessionRepository sessions;

  public SessionCycle(
      JdbcTemplate db,
      Clock clock,
      PlatformTransactionManager transactions,
      ActivityAuditWriter audit,
      RecordAbsences absences,
      SpaceMethodRules rules,
      SessionRepository sessions) {
    this.db = db;
    this.clock = clock;
    this.tx = new TransactionTemplate(transactions);
    this.audit = audit;
    this.absences = absences;
    this.rules = rules;
    this.sessions = sessions;
  }

  /** Procesa todo lo vencido; seguro ante ejecuciones simultáneas (SKIP LOCKED). */
  public void tick() {
    Timestamp now = Timestamp.from(clock.instant());
    while (Boolean.TRUE.equals(tx.execute(s -> startOne(now)))) {}
    while (Boolean.TRUE.equals(tx.execute(s -> closeOne(DUE_CLOSE, now, "SISTEMA_CIERRE")))) {}
    while (Boolean.TRUE.equals(
        tx.execute(s -> closeOne(DUE_LATE, now, "SISTEMA_CIERRE_TARDIO")))) {}
  }

  private boolean startOne(Timestamp now) {
    var row =
        db.queryForList(PICK.formatted(DUE_START), "PROGRAMADA", now, now, now).stream()
            .findFirst()
            .orElse(null);
    if (row == null) return false;
    UUID id = (UUID) row.get("id");
    UUID activity = (UUID) row.get("actividad_id");
    try {
      rules.check((UUID) row.get("espacio_id"), (String) row.get("metodo_verificacion"));
      sessions.requireGpsConfigured(id);
    } catch (ApiFailure e) {
      db.update(
          "UPDATE sesiones SET siguiente_intento_en=? WHERE id=?",
          Timestamp.from(now.toInstant().plusSeconds(RETRY_SECONDS)),
          id);
      audit.write(
          activity,
          null,
          "SISTEMA_INICIO_FALLIDO",
          e.getMessage(),
          Map.of("estado", "PROGRAMADA", "sesion_id", id),
          Map.of("estado", "PROGRAMADA", "codigo", e.code()));
      return true;
    }
    db.update("UPDATE sesiones SET estado='ACTIVA',iniciada_en=? WHERE id=?", now, id);
    audit.write(
        activity,
        null,
        "SISTEMA_INICIO",
        null,
        Map.of("estado", "PROGRAMADA", "sesion_id", id),
        Map.of("estado", "ACTIVA"));
    return true;
  }

  private boolean closeOne(String due, Timestamp now, String action) {
    var row = db.queryForList(PICK.formatted(due), "ACTIVA", now).stream().findFirst().orElse(null);
    if (row == null) return false;
    UUID id = (UUID) row.get("id");
    db.update("UPDATE sesiones SET estado='CERRADA',cerrada_en=? WHERE id=?", now, id);
    absences.record(id);
    audit.write(
        (UUID) row.get("actividad_id"),
        null,
        action,
        null,
        Map.of("estado", "ACTIVA", "sesion_id", id),
        Map.of("estado", "CERRADA"));
    return true;
  }
}
