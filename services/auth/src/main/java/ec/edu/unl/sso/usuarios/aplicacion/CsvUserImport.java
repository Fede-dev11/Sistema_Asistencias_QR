package ec.edu.unl.sso.usuarios.aplicacion;

import ec.edu.unl.sso.compartido.AdminAudit;
import ec.edu.unl.sso.compartido.Secrets;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository.Usuario;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Importación CSV {@code correo,cedula,nombres,roles,carreras}; roles y carreras con '|'. */
@Service
public class CsvUserImport {
  static final int MAX_BYTES = 2 * 1024 * 1024, MAX_ROWS = 5000;
  static final List<String> HEADER = List.of("correo", "cedula", "nombres", "roles", "carreras");

  /** Estado: CREADO (con temporal), ACTUALIZADO, SIN_CAMBIOS o RECHAZADO. */
  public record Fila(int fila, String correo, String estado, String detalle, String temporal) {}

  public record Informe(
      int procesadas, int creadas, int actualizadas, int sinCambios, List<Fila> filas) {
    public long rechazadas() {
      return filas.stream().filter(f -> f.estado().equals("RECHAZADO")).count();
    }
  }

  /** Error del archivo completo: no se aplica ningún cambio. */
  public static class ArchivoInvalido extends RuntimeException {
    ArchivoInvalido(String m) {
      super(m);
    }
  }

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final AdminAudit audit;

  CsvUserImport(UserRepository users, PasswordEncoder encoder, AdminAudit audit) {
    this.users = users;
    this.encoder = encoder;
    this.audit = audit;
  }

  /** Lector CSV: UTF-8, comillas dobles con escape "", separador "," o ";" según el encabezado. */
  static List<List<String>> parse(byte[] data) {
    if (data.length > MAX_BYTES) throw new ArchivoInvalido("El archivo supera 2 MB.");
    String text;
    try {
      text =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(data))
              .toString();
    } catch (CharacterCodingException e) {
      throw new ArchivoInvalido("El archivo debe estar codificado en UTF-8.");
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
    if (quoted) throw new ArchivoInvalido("El archivo tiene comillas sin cerrar.");
    if (content || cell.length() > 0) {
      row.add(cell.toString());
      rows.add(row);
    }
    if (rows.size() - 1 > MAX_ROWS) throw new ArchivoInvalido("Máximo 5 000 filas.");
    return rows;
  }

  @Transactional
  public Informe importar(UUID actor, byte[] data) {
    List<List<String>> rows = parse(data);
    if (rows.isEmpty()
        || !rows.getFirst().stream()
            .map(h -> h.strip().toLowerCase(Locale.ROOT))
            .toList()
            .equals(HEADER)) {
      throw new ArchivoInvalido("El encabezado debe ser " + String.join(",", HEADER) + ".");
    }
    List<Fila> filas = new ArrayList<>();
    int creadas = 0, actualizadas = 0, sin = 0;
    for (int i = 1; i < rows.size(); i++) {
      List<String> r = rows.get(i);
      int line = i + 1;
      String mail = r.isEmpty() ? "" : r.getFirst().strip().toLowerCase(Locale.ROOT);
      if (r.size() != HEADER.size()) {
        filas.add(new Fila(line, mail, "RECHAZADO", "Número de columnas incorrecto.", null));
        continue;
      }
      try {
        Usuario v =
            ManageUsers.validar(
                UUID.randomUUID(),
                r.get(0),
                r.get(1),
                r.get(2),
                partes(r.get(3)),
                partes(r.get(4)));
        var existente = users.porCorreo(v.correo());
        if (existente.isEmpty()) {
          if (users.porCedula(v.cedula()).isPresent()) {
            throw new IllegalArgumentException("La cédula ya pertenece a otro usuario.");
          }
          String temporal = Secrets.temporaryPassword();
          users.insertar(
              new Usuario(
                  v.id(),
                  v.correo(),
                  v.cedula(),
                  v.nombres(),
                  encoder.encode(temporal),
                  true,
                  true,
                  v.roles(),
                  v.carreras()));
          filas.add(new Fila(line, v.correo(), "CREADO", "Usuario creado.", temporal));
          creadas++;
          continue;
        }
        Usuario e = existente.get();
        if (!e.cedula().equals(v.cedula()) && users.porCedula(v.cedula()).isPresent()) {
          throw new IllegalArgumentException("La cédula ya pertenece a otro usuario.");
        }
        boolean igual =
            e.cedula().equals(v.cedula())
                && e.nombres().equals(v.nombres())
                && new HashSet<>(e.roles()).equals(new HashSet<>(v.roles()))
                && new HashSet<>(e.carreras()).equals(new HashSet<>(v.carreras()));
        if (igual) {
          filas.add(new Fila(line, e.correo(), "SIN_CAMBIOS", "Sin cambios.", null));
          sin++;
        } else {
          users.actualizar(
              new Usuario(
                  e.id(),
                  e.correo(),
                  v.cedula(),
                  v.nombres(),
                  null,
                  e.activo(),
                  e.debeCambiarPassword(),
                  v.roles(),
                  v.carreras()));
          filas.add(new Fila(line, e.correo(), "ACTUALIZADO", "Usuario actualizado.", null));
          actualizadas++;
        }
      } catch (IllegalArgumentException e) {
        filas.add(new Fila(line, mail, "RECHAZADO", e.getMessage(), null));
      }
    }
    Informe informe = new Informe(rows.size() - 1, creadas, actualizadas, sin, List.copyOf(filas));
    audit.registrar(
        actor,
        "USUARIOS_IMPORTAR",
        null,
        null,
        Map.of(
            "procesadas",
            informe.procesadas(),
            "creadas",
            creadas,
            "actualizadas",
            actualizadas,
            "rechazadas",
            informe.rechazadas()));
    return informe;
  }

  private static List<String> partes(String raw) {
    return Arrays.stream(raw.split("\\|")).map(String::strip).filter(s -> !s.isEmpty()).toList();
  }
}
