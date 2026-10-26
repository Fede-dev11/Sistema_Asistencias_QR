package ec.edu.unl.geoasist.reportes.aplicacion;

import ec.edu.unl.geoasist.actividades.aplicacion.*;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import ec.edu.unl.geoasist.reportes.persistencia.AttendanceReadRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportAttendancesCsv {
  private final ActivityRepository activities;
  private final ActivityAuthorization scope;
  private final AttendanceReadRepository rows;
  private final ActivityAuditWriter audit;

  public ExportAttendancesCsv(
      ActivityRepository activities,
      ActivityAuthorization scope,
      AttendanceReadRepository rows,
      ActivityAuditWriter audit) {
    this.activities = activities;
    this.scope = scope;
    this.rows = rows;
    this.audit = audit;
  }

  @Transactional
  public String export(UserSession user, UUID session, String state) {
    FindAttendances.validate(0, 100, state);
    UUID activity = activities.activityOfSession(session);
    scope.write(user, activity);
    StringBuilder csv =
        new StringBuilder(
            "asistencia_id,persona_id,cedula,nombres,estado,registrada_en,attempt_id,code,mensaje,forma,razon_manual\n");
    for (var row : rows.all(session, state)) {
      csv.append(cell(row.attendance() == null ? "" : row.attendance().toString())).append(',');
      csv.append(cell(row.person().toString())).append(',');
      csv.append(cell(row.cedula())).append(',');
      csv.append(cell(row.names())).append(',');
      csv.append(cell(row.state())).append(',');
      csv.append(cell(row.registered().toString())).append(',');
      csv.append(cell(row.attempt().toString())).append(',');
      csv.append(cell(row.code())).append(',');
      csv.append(cell(row.message())).append(',');
      csv.append(cell(row.form())).append(',');
      csv.append(cell(row.manualReason())).append('\n');
    }
    audit.write(
        activity,
        user.accountId(),
        "EXPORTAR_ASISTENCIAS",
        null,
        Map.of(),
        Map.of("estado", state == null ? "TODOS" : state));
    return csv.toString();
  }

  static String cell(String value) {
    String safe = value == null ? "" : value;
    if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
    if (safe.indexOf(',') >= 0
        || safe.indexOf('"') >= 0
        || safe.indexOf('\n') >= 0
        || safe.indexOf('\r') >= 0) return '"' + safe.replace("\"", "\"\"") + '"';
    return safe;
  }
}
