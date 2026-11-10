package ec.edu.unl.geoasist.actividades.aplicacion;

import ec.edu.unl.geoasist.actividades.dominio.Activity;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindVisibleActivities {
  private final JdbcTemplate db;
  private final Clock clock;
  private final ActivityAuthorization scope;
  private final ActivityRepository activities;

  public FindVisibleActivities(
      JdbcTemplate db, Clock clock, ActivityAuthorization scope, ActivityRepository activities) {
    this.db = db;
    this.clock = clock;
    this.scope = scope;
    this.activities = activities;
  }

  public Map<String, Object> dto(Activity a) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", a.id());
    out.put("nombre", a.name());
    out.put("tipo", a.mode());
    out.put("estado", a.state());
    out.put("cerrada_en", a.closedAt());
    out.put("anonimizar_en", a.anonymizeAt());
    out.put("responsable_id", a.responsible());
    var details =
        db.queryForMap(
            "SELECT acceso,espacio_id,metodo_verificacion FROM actividades WHERE id=?", a.id());
    out.put("acceso", details.get("acceso"));
    out.put("espacio_id", details.get("espacio_id"));
    out.put("metodo_verificacion", details.get("metodo_verificacion"));
    out.put(
        "carrera_ids",
        db.queryForList(
            "SELECT carrera_id FROM actividad_carreras WHERE actividad_id=? ORDER BY carrera_id",
            UUID.class,
            a.id()));
    out.put(
        "paralelo_ids",
        db.queryForList(
            "SELECT paralelo_id FROM actividad_paralelos WHERE actividad_id=? ORDER BY paralelo_id",
            UUID.class,
            a.id()));
    out.putAll(
        db.queryForMap(
            "SELECT ciclo_automatico,umbral_asistencia FROM actividades WHERE id=?", a.id()));
    return out;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> get(UserSession user, UUID id) {
    return dto(scope.read(user, id));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> list(UserSession user, int page, int size, String mode, UUID career) {
    if (page < 0
        || size < 1
        || size > 100
        || (mode != null && !Set.of("CLASE", "EVENTO").contains(mode))) throw ApiFailure.invalid();
    if (!user.responsibleRole()
        && !user.has("DIRECTOR_INSTITUCIONAL")
        && !user.has("DIRECTOR_CARRERA")) throw ApiFailure.forbidden();
    if (career != null && !user.has("DIRECTOR_INSTITUCIONAL")) {
      boolean allowed =
          Boolean.TRUE.equals(
                  db.queryForObject(
                      "SELECT EXISTS(SELECT 1 FROM direcciones_carrera WHERE cuenta_id=? AND carrera_id=? AND vigente)",
                      Boolean.class,
                      user.accountId(),
                      career))
              && user.has("DIRECTOR_CARRERA");
      if (!allowed && user.responsibleRole())
        allowed =
            Boolean.TRUE.equals(
                db.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM actividades a JOIN actividad_carreras ac ON ac.actividad_id=a.id WHERE a.responsable_id=? AND ac.carrera_id=?)",
                    Boolean.class,
                    user.accountId(),
                    career));
      if (!allowed) throw ApiFailure.forbidden();
    }
    String where =
        " WHERE (a.anonimizar_en IS NULL OR a.anonimizar_en>?) AND (? OR (? AND a.responsable_id=?) OR (? AND EXISTS(SELECT 1 FROM direcciones_carrera d JOIN actividad_carreras ac ON ac.carrera_id=d.carrera_id WHERE d.cuenta_id=? AND d.vigente AND ac.actividad_id=a.id)))";
    List<Object> params =
        new ArrayList<>(
            List.of(
                java.sql.Timestamp.from(clock.instant()),
                user.has("DIRECTOR_INSTITUCIONAL"),
                user.responsibleRole(),
                user.accountId(),
                user.has("DIRECTOR_CARRERA"),
                user.accountId()));
    if (mode != null) {
      where += " AND a.modalidad=?";
      params.add(mode);
    }
    if (career != null) {
      where +=
          " AND EXISTS(SELECT 1 FROM actividad_carreras ac WHERE ac.actividad_id=a.id AND ac.carrera_id=?)";
      params.add(career);
    }
    Long total =
        db.queryForObject(
            "SELECT count(*) FROM actividades a" + where, Long.class, params.toArray());
    params.add(size);
    params.add((long) page * size);
    List<Map<String, Object>> items =
        db
            .queryForList(
                "SELECT a.id FROM actividades a" + where + " ORDER BY a.id LIMIT ? OFFSET ?",
                UUID.class,
                params.toArray())
            .stream()
            .map(activities::find)
            .map(this::dto)
            .toList();
    return Map.of("items", items, "page", page, "size", size, "total", total);
  }
}
