package ec.edu.unl.geoasist.academico;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class CsvImportTest extends AdminSupport {
  private String mail() {
    return "imp-" + UUID.randomUUID() + "@example.test";
  }

  @Test
  void importsValidRowsReportsInvalidOnesAndIsIdempotent() throws Exception {
    String a = admin();
    StringBuilder csv = new StringBuilder("cedula,nombres,correo\n");
    for (int i = 0; i < 100; i++)
      csv.append(fictionalId())
          .append(",\"Apellido, Nombre ")
          .append(i)
          .append("\",")
          .append(mail())
          .append("\n");
    String dup = mail();
    csv.append("0100000018,Cédula mala,").append(mail()).append("\n"); // fila 102
    csv.append(fictionalId()).append(",Correo malo,no-es-correo\n"); // fila 103
    String firstDup = fictionalId();
    csv.append(firstDup).append(",Primero,").append(dup).append("\n");
    csv.append(fictionalId()).append(",Correo repetido,").append(dup).append("\n"); // fila 105
    String text = csv.toString();
    var first = importCsv(a, "students", text, 200);
    assertEquals(104, first.get("procesadas").asInt());
    assertEquals(101, first.get("creadas").asInt());
    var rejected = first.get("rechazadas");
    assertEquals(3, rejected.size());
    assertEquals(102, rejected.get(0).get("fila").asInt());
    assertEquals(103, rejected.get(1).get("fila").asInt());
    assertEquals(105, rejected.get(2).get("fila").asInt());
    assertFalse(rejected.get(0).get("motivo").asText().isBlank());
    long people = db.queryForObject("SELECT count(*) FROM personas", Long.class);
    var again = importCsv(a, "students", text, 200);
    assertEquals(0, again.get("creadas").asInt());
    assertEquals(101, again.get("sin_cambios").asInt());
    assertEquals(people, db.queryForObject("SELECT count(*) FROM personas", Long.class));
    // El estudiante importado inicia sesión con la contraseña de configuración, no del CSV.
    token(text.lines().skip(1).findFirst().orElseThrow().split(",")[3].strip());
  }

  @Test
  void semicolonSeparatorAndTeachersAreAccepted() throws Exception {
    String a = admin();
    String m = mail();
    var r =
        importCsv(
            a,
            "teachers",
            "cedula;nombres;correo;rol\n"
                + fictionalId()
                + ";Docente Uno;"
                + m
                + ";ENCARGADO\n"
                + fictionalId()
                + ";Docente Dos;"
                + mail()
                + ";DIRECTOR_INSTITUCIONAL\n",
            200);
    assertEquals(1, r.get("creadas").asInt());
    assertEquals(1, r.get("rechazadas").size());
    assertEquals(
        List.of("ENCARGADO"),
        db.queryForList(
            "SELECT rol FROM roles_cuenta r JOIN cuentas c ON c.id=r.cuenta_id WHERE c.identificador=?",
            String.class,
            m));
  }

  @Test
  void badHeaderChangesNothingAndLimitsAre413() throws Exception {
    String a = admin();
    long people = db.queryForObject("SELECT count(*) FROM personas", Long.class);
    importCsv(a, "students", "cedula,nombre,correo\n" + fictionalId() + ",X," + mail() + "\n", 400);
    importCsv(a, "otro", "cedula,nombres,correo\n", 400);
    assertEquals(people, db.queryForObject("SELECT count(*) FROM personas", Long.class));
    StringBuilder many = new StringBuilder("cedula,nombres,correo\n");
    for (int i = 0; i < 5001; i++) many.append("0100000017,X,x@example.test\n");
    importCsv(a, "students", many.toString(), 413);
    importCsv(a, "students", "cedula,nombres,correo\n" + "x".repeat(2 * 1024 * 1024), 413);
  }

  @Test
  void enrollmentImportReportsUnknownReferences() throws Exception {
    String a = admin();
    String cedula = fictionalId();
    importCsv(
        a, "students", "cedula,nombres,correo\n" + cedula + ",Matriculable," + mail() + "\n", 200);
    String csv =
        "cedula,carrera,paralelo\n"
            + cedula
            + ",CARRERA-1,A\n"
            + cedula
            + ",NOEXISTE,A\n"
            + fictionalId()
            + ",CARRERA-1,A\n";
    var r = importCsv(a, "enrollments", csv, 200);
    assertEquals(1, r.get("creadas").asInt());
    assertEquals(2, r.get("rechazadas").size());
    var again = importCsv(a, "enrollments", csv, 200);
    assertEquals(0, again.get("creadas").asInt());
    assertEquals(1, again.get("sin_cambios").asInt());
  }
}
