package ec.edu.unl.geoasist.academico.aplicacion;

import ec.edu.unl.geoasist.academico.persistencia.AcademicRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.AcademicAudit;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.dominio.Cedula;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.nio.charset.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CsvImport {
  static final int MAX_BYTES = 2 * 1024 * 1024, MAX_ROWS = 5000;
  private static final Map<String, List<String>> HEADERS =
      Map.of(
          "students", List.of("cedula", "nombres", "correo"),
          "teachers", List.of("cedula", "nombres", "correo", "rol"),
          "enrollments", List.of("cedula", "carrera", "paralelo"));

  public record Rejection(int fila, String motivo) {}

  public record ImportReport(
      int procesadas, int creadas, int actualizadas, int sin_cambios, List<Rejection> rechazadas) {}

  private static final class Counter {
    int created, updated, same;
    final List<Rejection> rejected = new ArrayList<>();
  }

  private final AcademicRepository repo;
  private final AcademicAudit audit;

  public CsvImport(AcademicRepository repo, AcademicAudit audit) {
    this.repo = repo;
    this.audit = audit;
  }

  /** Lector CSV: UTF-8, comillas dobles con escape "", separador "," o ";" según el encabezado. */
  static List<List<String>> parse(byte[] data) {
    if (data.length > MAX_BYTES)
      throw new ApiFailure(413, "ARCHIVO_DEMASIADO_GRANDE", "El archivo supera 2 MB.");
    String text;
    try {
      text =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .decode(java.nio.ByteBuffer.wrap(data))
              .toString();
    } catch (CharacterCodingException e) {
      throw ApiFailure.invalid();
    }
    if (text.startsWith("﻿")) text = text.substring(1);
    int eol = text.indexOf('\n');
    String first = eol < 0 ? text : text.substring(0, eol);
    char sep = first.contains(";") && !first.contains(",") ? ';' : ',';
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder cell = new StringBuilder();
    boolean quoted = false, content = false;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (quoted) {
        if (ch != '"') cell.append(ch);
        else if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
          cell.append('"');
          i++;
        } else quoted = false;
      } else if (ch == '"') {
        quoted = true;
        content = true;
      } else if (ch == sep) {
        row.add(cell.toString());
        cell.setLength(0);
        content = true;
      } else if (ch == '\n' || ch == '\r') {
        if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
        if (content || cell.length() > 0) {
          row.add(cell.toString());
          rows.add(row);
        }
        row = new ArrayList<>();
        cell.setLength(0);
        content = false;
      } else {
        cell.append(ch);
        content = true;
      }
    }
    if (quoted) throw ApiFailure.invalid();
    if (content || cell.length() > 0) {
      row.add(cell.toString());
      rows.add(row);
    }
    if (rows.size() - 1 > MAX_ROWS)
      throw new ApiFailure(413, "ARCHIVO_DEMASIADO_GRANDE", "Máximo 5 000 filas.");
    return rows;
  }

  @Transactional
  public ImportReport run(UserSession user, String kind, byte[] data) {
    user.requireAnyRole("ADMINISTRADOR");
    var expected = HEADERS.get(kind);
    if (expected == null) throw ApiFailure.invalid();
    var rows = parse(data);
    if (rows.isEmpty()
        || !rows.getFirst().stream()
            .map(h -> h.strip().toLowerCase(Locale.ROOT))
            .toList()
            .equals(expected))
      throw new ApiFailure(
          400, "ENCABEZADO_INVALIDO", "El encabezado debe ser " + String.join(",", expected) + ".");
    Counter c = new Counter();
    for (int i = 1; i < rows.size(); i++) {
      var r = rows.get(i);
      int line = i + 1;
      if (r.size() != expected.size()) {
        c.rejected.add(new Rejection(line, "Número de columnas incorrecto."));
        continue;
      }
      try {
        if (kind.equals("enrollments")) enrollment(r, c);
        else person(r, kind.equals("students") ? "ESTUDIANTE" : r.get(3).strip(), c);
      } catch (ApiFailure e) {
        c.rejected.add(new Rejection(line, e.getMessage()));
      }
    }
    var report =
        new ImportReport(rows.size() - 1, c.created, c.updated, c.same, List.copyOf(c.rejected));
    audit.write(
        user.accountId(),
        "IMPORTAR_" + kind.toUpperCase(Locale.ROOT),
        "IMPORTACION",
        null,
        Map.of(),
        Map.of(
            "procesadas",
            report.procesadas(),
            "creadas",
            c.created,
            "actualizadas",
            c.updated,
            "rechazadas",
            c.rejected.size()));
    return report;
  }

  private static ApiFailure reject(String why) {
    return new ApiFailure(400, "FILA_INVALIDA", why);
  }

  private void person(List<String> r, String role, Counter c) {
    String cedula;
    try {
      cedula = Cedula.of(r.get(0)).value();
    } catch (ApiFailure e) {
      throw reject("Cédula inválida.");
    }
    String name, mail;
    try {
      name = ManagePeopleAndAccounts.name(r.get(1));
      mail = ManagePeopleAndAccounts.email(r.get(2));
    } catch (ApiFailure e) {
      throw reject("Nombres o correo inválidos.");
    }
    if (!Set.of("ESTUDIANTE", "DOCENTE", "ENCARGADO").contains(role)) throw reject("Rol inválido.");
    var byMail = repo.personOfEmail(mail);
    var existing = repo.personByCedula(cedula);
    if (existing.isEmpty() && byMail.isPresent())
      throw reject("El correo ya pertenece a otra persona.");
    boolean changed = false;
    UUID person;
    if (existing.isEmpty()) {
      person = repo.insertPerson(cedula, name);
      changed = true;
    } else {
      person = (UUID) existing.get().get("id");
      if (byMail.isPresent() && !byMail.get().equals(person))
        throw reject("El correo ya pertenece a otra persona.");
      var account = (Map<?, ?>) existing.get().get("cuenta");
      if (account != null && !mail.equals(account.get("correo")))
        throw reject("La persona ya tiene cuenta con otro correo.");
      if (!name.equals(existing.get().get("nombres"))) {
        repo.renamePerson(person, name);
        changed = true;
      }
    }
    boolean isNew = existing.isEmpty();
    UUID account;
    if (byMail.isPresent()) account = repo.accountByEmail(mail).orElseThrow();
    else {
      account = repo.insertAccount(person, mail);
      changed = true;
    }
    if (repo.addRole(account, role)) changed = true;
    if (isNew) c.created++;
    else if (changed) c.updated++;
    else c.same++;
  }

  private void enrollment(List<String> r, Counter c) {
    UUID person;
    try {
      var found = repo.personByCedula(Cedula.of(r.get(0)).value());
      if (found.isEmpty()) throw reject("No existe la persona.");
      person = (UUID) found.get().get("id");
    } catch (ApiFailure e) {
      throw reject(e.code().equals("CEDULA_INVALIDA") ? "Cédula inválida." : e.getMessage());
    }
    if (!repo.hasRole(person, "ESTUDIANTE")) throw reject("La persona no tiene rol ESTUDIANTE.");
    var career =
        repo.careerByCode(r.get(1).strip()).orElseThrow(() -> reject("No existe la carrera."));
    var parallel =
        repo.parallelByCode(career, r.get(2).strip())
            .orElseThrow(() -> reject("No existe el paralelo."));
    switch (repo.enroll(person, parallel)) {
      case 1 -> c.created++;
      case 2 -> c.updated++;
      default -> c.same++;
    }
  }
}
