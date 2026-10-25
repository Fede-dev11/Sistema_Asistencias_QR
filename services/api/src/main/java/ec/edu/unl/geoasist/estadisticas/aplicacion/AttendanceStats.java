package ec.edu.unl.geoasist.estadisticas.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuthorization;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.estadisticas.persistencia.StatsRepository;
import ec.edu.unl.geoasist.estadisticas.persistencia.StatsRepository.Counts;
import ec.edu.unl.geoasist.estadisticas.persistencia.StatsRepository.Viewer;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceStats {
  private final ActivityRepository activities;
  private final ActivityAuthorization scope;
  private final StatsRepository stats;

  public AttendanceStats(
      ActivityRepository activities, ActivityAuthorization scope, StatsRepository stats) {
    this.activities = activities;
    this.scope = scope;
    this.stats = stats;
  }

  /** Porcentaje con 2 decimales; null ("sin datos") si no hay sesiones computables. */
  public static BigDecimal percentage(Counts c) {
    return c.computable() == 0
        ? null
        : BigDecimal.valueOf(c.attended())
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(c.computable()), 2, RoundingMode.HALF_UP);
  }

  private static Map<String, Object> dto(Counts c, BigDecimal threshold) {
    var m = new LinkedHashMap<String, Object>();
    BigDecimal pct = percentage(c);
    m.put("sesiones_computables", c.computable());
    m.put("asistencias", c.attended());
    m.put("ausencias", c.absent());
    m.put("ausencias_justificadas", c.justified());
    m.put("porcentaje", pct);
    if (threshold != null) m.put("bajo_umbral", pct != null && pct.compareTo(threshold) < 0);
    return m;
  }

  private static void page(int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw ApiFailure.invalid();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> activity(UserSession user, UUID id, int page, int size) {
    page(page, size);
    var activity = scope.read(user, id);
    if (!"CLASE".equals(activity.mode())) throw ApiFailure.conflict();
    BigDecimal threshold = stats.threshold(id);
    var items = new ArrayList<Map<String, Object>>();
    for (var s : stats.byStudent(id, size, (long) page * size)) {
      var m = new LinkedHashMap<String, Object>();
      m.put("persona_id", s.person());
      m.put("cedula", s.cedula());
      m.put("nombres", s.names());
      m.putAll(dto(s.counts(), threshold));
      items.add(m);
    }
    var result = new LinkedHashMap<String, Object>();
    result.put("umbral_asistencia", threshold);
    result.put("resumen", dto(stats.totals(id), null));
    result.put("items", items);
    result.put("page", page);
    result.put("size", size);
    result.put("total", stats.countStudents(id));
    return result;
  }

  private Viewer viewer(UserSession user) {
    user.requireAnyRole("DOCENTE", "ENCARGADO", "DIRECTOR_CARRERA", "DIRECTOR_INSTITUCIONAL");
    return new Viewer(
        user.accountId(),
        user.has("DIRECTOR_INSTITUCIONAL"),
        user.responsibleRole(),
        user.has("DIRECTOR_CARRERA"));
  }

  private static void range(Instant from, Instant to) {
    if (from != null && to != null && !from.isBefore(to)) throw ApiFailure.invalid();
  }

  private static List<Map<String, Object>> groups(
      List<StatsRepository.Group> rows, boolean parallel) {
    var out = new ArrayList<Map<String, Object>>();
    for (var g : rows) {
      var m = new LinkedHashMap<String, Object>();
      m.put(parallel ? "paralelo_id" : "carrera_id", g.id());
      if (parallel) m.put("carrera_id", g.careerId());
      m.put("carrera", g.careerCode());
      m.put(parallel ? "paralelo" : "nombre", g.code());
      m.putAll(dto(g.counts(), null));
      out.add(m);
    }
    return out;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> parallels(UserSession user, UUID career, Instant from, Instant to) {
    Viewer v = viewer(user);
    range(from, to);
    // Quien solo dirige otras carreras no puede pedir esta; un responsable ve únicamente sus clases.
    if (career != null
        && !v.institutional()
        && !v.responsible()
        && !stats.directsCareer(user.accountId(), career)) throw ApiFailure.forbidden();
    return Map.of("items", groups(stats.byParallel(v, career, from, to), true));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> careers(UserSession user, Instant from, Instant to) {
    Viewer v = viewer(user);
    range(from, to);
    return Map.of("items", groups(stats.byCareer(v, from, to), false));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> absences(UserSession user, UUID session, int page, int size) {
    page(page, size);
    scope.read(user, activities.activityOfSession(session));
    var items = new ArrayList<Map<String, Object>>();
    for (var a : stats.absences(session, size, (long) page * size)) {
      var m = new LinkedHashMap<String, Object>();
      m.put("persona_id", a.person());
      m.put("cedula", a.cedula());
      m.put("nombres", a.names());
      m.put("estado", a.state());
      items.add(m);
    }
    return Map.of(
        "items", items, "page", page, "size", size, "total", stats.countAbsences(session));
  }

  /** Historial propio: la persona sale de la sesión, nunca de la petición. */
  @Transactional(readOnly = true)
  public Map<String, Object> myHistory(UserSession user, int page, int size) {
    page(page, size);
    var items = new ArrayList<Map<String, Object>>();
    for (var h : stats.mine(user.personId(), size, (long) page * size)) {
      var m = new LinkedHashMap<String, Object>();
      m.put("sesion_id", h.session());
      m.put("actividad_id", h.activity());
      m.put("actividad", h.name());
      m.put("inicio", h.start());
      m.put("resultado", h.result());
      m.put("justificacion", h.justification());
      items.add(m);
    }
    return Map.of(
        "items", items, "page", page, "size", size, "total", stats.countMine(user.personId()));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> myStats(UserSession user) {
    var items = new ArrayList<Map<String, Object>>();
    for (var a : stats.myActivities(user.personId())) {
      var m = new LinkedHashMap<String, Object>();
      m.put("actividad_id", a.activity());
      m.put("actividad", a.name());
      m.put("umbral_asistencia", a.threshold());
      m.putAll(dto(a.counts(), a.threshold()));
      items.add(m);
    }
    return Map.of("items", items);
  }
}
