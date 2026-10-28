package ec.edu.unl.sso.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import ec.edu.unl.sso.soporte.AuthFixture;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

class CsvImportTest extends AuthFixture {
  private static final String HEADER = "correo,cedula,nombres,roles,carreras\n";

  private MockHttpServletResponse importar(Cookie admin, byte[] data) throws Exception {
    return mvc.perform(
            multipart("/admin/importar")
                .file(new MockMultipartFile("archivo", "u.csv", "text/csv", data))
                .with(csrf())
                .cookie(admin))
        .andReturn()
        .getResponse();
  }

  private int count() {
    return db.queryForObject(
        "SELECT count(*) FROM usuarios WHERE correo LIKE '%@nuevo.test'", Integer.class);
  }

  @Test
  void reportsPerRowAndIsIdempotentByEmail() throws Exception {
    Cookie admin = adminCookie();
    String csv =
        HEADER
            + "csv1@nuevo.test,"
            + cedula(11)
            + ",Uno Uno,DOCENTE|ENCARGADO,C1|C2\n"
            + "CSV2@nuevo.test,"
            + cedula(12)
            + ",\"Dos, Dos\",ESTUDIANTE,\n"
            + "malo@nuevo.test,0100000018,Tres,ESTUDIANTE,\n"
            + "rol@nuevo.test,"
            + cedula(13)
            + ",Cuatro,PIRATA,\n"
            + "corto@nuevo.test,1\n";
    var res = importar(admin, csv.getBytes(StandardCharsets.UTF_8));
    assertEquals(200, res.getStatus());
    String html = res.getContentAsString();
    assertTrue(
        html.contains("5 procesadas: 2 creadas, 0 actualizadas, 0 sin cambios, 3 rechazadas"));
    assertEquals(2, count());
    assertEquals(
        2,
        db.queryForObject(
            "SELECT count(*) FROM usuario_roles r JOIN usuarios u ON u.id = r.usuario_id WHERE u.correo = 'csv1@nuevo.test'",
            Integer.class));
    assertEquals(
        2,
        db.queryForObject(
            "SELECT count(*) FROM usuarios WHERE correo LIKE 'csv%@nuevo.test' AND debe_cambiar_password",
            Integer.class));

    // segunda importación: sin cambios; y un cambio de nombres actualiza
    String again = importar(admin, csv.getBytes(StandardCharsets.UTF_8)).getContentAsString();
    assertTrue(again.contains("0 creadas, 0 actualizadas, 2 sin cambios, 3 rechazadas"));
    String changed = csv.replace("Uno Uno", "Uno Cambiado");
    assertTrue(
        importar(admin, changed.getBytes(StandardCharsets.UTF_8))
            .getContentAsString()
            .contains("0 creadas, 1 actualizadas, 1 sin cambios"));
    assertEquals(
        "Uno Cambiado",
        db.queryForObject(
            "SELECT nombres FROM usuarios WHERE correo = 'csv1@nuevo.test'", String.class));
    assertEquals(2, count());
  }

  @Test
  void badHeaderFailsWithoutChanges() throws Exception {
    var res =
        importar(
            adminCookie(),
            ("correo,cedula,nombres\nx@nuevo.test," + cedula(14) + ",X\n")
                .getBytes(StandardCharsets.UTF_8));
    assertEquals(400, res.getStatus());
    assertTrue(res.getContentAsString().contains("El encabezado debe ser"));
    assertEquals(0, count());
  }

  @Test
  void enforcesRowAndSizeLimits() throws Exception {
    Cookie admin = adminCookie();
    StringBuilder rows = new StringBuilder(HEADER);
    for (int i = 0; i < 5001; i++)
      rows.append("l")
          .append(i)
          .append("@nuevo.test,")
          .append(cedula(100 + i))
          .append(",N,DOCENTE,\n");
    var tooMany = importar(admin, rows.toString().getBytes(StandardCharsets.UTF_8));
    assertEquals(400, tooMany.getStatus());
    assertTrue(tooMany.getContentAsString().contains("Máximo 5 000 filas"));
    byte[] big = new byte[2 * 1024 * 1024 + 1];
    java.util.Arrays.fill(big, (byte) 'a');
    var tooBig = importar(admin, big);
    assertEquals(400, tooBig.getStatus());
    assertTrue(tooBig.getContentAsString().contains("supera 2 MB"));
    assertEquals(0, count());
  }

  @Test
  void importIsAuditedWithoutPasswords() throws Exception {
    Cookie admin = adminCookie();
    importar(
        admin,
        (HEADER + "aud@nuevo.test," + cedula(15) + ",A,DOCENTE,\n")
            .getBytes(StandardCharsets.UTF_8));
    assertEquals(
        1,
        db.queryForObject(
            "SELECT count(*) FROM auditoria_admin WHERE accion = 'USUARIOS_IMPORTAR'",
            Integer.class));
  }
}
