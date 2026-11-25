package ec.edu.unl.sso.usuarios.aplicacion;

import ec.edu.unl.sso.cas.aplicacion.SsoSessions;
import ec.edu.unl.sso.compartido.AdminAudit;
import ec.edu.unl.sso.compartido.Cedula;
import ec.edu.unl.sso.compartido.Secrets;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository.Usuario;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administración de usuarios (FR-008). Toda acción queda en auditoria_admin. */
@Service
public class ManageUsers {
  public static final List<String> ROLES =
      List.of(
          "ESTUDIANTE",
          "DOCENTE",
          "ENCARGADO",
          "DIRECTOR_INSTITUCIONAL",
          "DIRECTOR_CARRERA",
          "ADMINISTRADOR");
  private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+");
  private static final Pattern CAREER = Pattern.compile("[A-Za-z0-9_.-]{1,40}");

  /** Usuario creado y su contraseña temporal (se muestra una sola vez). */
  public record Creado(Usuario usuario, String temporal) {}

  private final UserRepository users;
  private final SsoSessions sessions;
  private final PasswordEncoder encoder;
  private final AdminAudit audit;

  ManageUsers(
      UserRepository users, SsoSessions sessions, PasswordEncoder encoder, AdminAudit audit) {
    this.users = users;
    this.sessions = sessions;
    this.encoder = encoder;
    this.audit = audit;
  }

  /** Normaliza y valida; lanza IllegalArgumentException con mensaje en español. */
  public static Usuario validar(
      UUID id,
      String correo,
      String cedula,
      String nombres,
      List<String> roles,
      List<String> carreras) {
    String mail = correo == null ? "" : correo.strip().toLowerCase();
    if (mail.length() > 254 || !EMAIL.matcher(mail).matches()) {
      throw new IllegalArgumentException("El correo no es válido.");
    }
    String name = nombres == null ? "" : nombres.strip();
    if (name.isEmpty() || name.length() > 200) {
      throw new IllegalArgumentException("Los nombres deben tener entre 1 y 200 caracteres.");
    }
    List<String> r =
        roles == null ? List.of() : roles.stream().map(String::strip).distinct().sorted().toList();
    if (r.isEmpty() || !ROLES.containsAll(r)) {
      throw new IllegalArgumentException("Selecciona al menos un rol de la lista.");
    }
    List<String> c =
        carreras == null
            ? List.of()
            : carreras.stream()
                .map(String::strip)
                .filter(x -> !x.isEmpty())
                .distinct()
                .sorted()
                .toList();
    if (!c.stream().allMatch(x -> CAREER.matcher(x).matches())) {
      throw new IllegalArgumentException(
          "Los códigos de carrera admiten letras, números, punto, guion y guion bajo (máx. 40).");
    }
    return new Usuario(id, mail, Cedula.of(cedula).value(), name, null, true, true, r, c);
  }

  /** Separa una lista escrita en un formulario (comas, espacios o barras). */
  public static List<String> lista(String raw) {
    return raw == null
        ? List.of()
        : Arrays.stream(raw.split("[,|\\s]+")).filter(s -> !s.isBlank()).toList();
  }

  @Transactional
  public Creado crear(
      UUID actor,
      String correo,
      String cedula,
      String nombres,
      List<String> roles,
      List<String> carreras) {
    UUID id = UUID.randomUUID();
    Usuario v = validar(id, correo, cedula, nombres, roles, carreras);
    unico(v);
    String temporal = Secrets.temporaryPassword();
    try {
      users.insertar(
          new Usuario(
              id,
              v.correo(),
              v.cedula(),
              v.nombres(),
              encoder.encode(temporal),
              true,
              true,
              v.roles(),
              v.carreras()));
    } catch (DuplicateKeyException e) {
      throw new IllegalArgumentException("El correo o la cédula ya existen.");
    }
    audit.registrar(actor, "USUARIO_CREAR", id, null, datos(v));
    return new Creado(users.porId(id).orElseThrow(), temporal);
  }

  @Transactional
  public Usuario editar(
      UUID actor,
      UUID id,
      String correo,
      String cedula,
      String nombres,
      List<String> roles,
      List<String> carreras) {
    Usuario antes = existente(id);
    Usuario v = validar(id, correo, cedula, nombres, roles, carreras);
    unico(v);
    try {
      users.actualizar(v);
    } catch (DuplicateKeyException e) {
      throw new IllegalArgumentException("El correo o la cédula ya existen.");
    }
    audit.registrar(actor, "USUARIO_EDITAR", id, datos(antes), datos(v));
    return users.porId(id).orElseThrow();
  }

  @Transactional
  public void fijarActivo(UUID actor, UUID id, boolean activo) {
    Usuario u = existente(id);
    if (!activo && id.equals(actor)) {
      throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
    }
    users.fijarActivo(id, activo);
    if (!activo) sessions.cerrarTodas(id);
    audit.registrar(
        actor,
        activo ? "USUARIO_ACTIVAR" : "USUARIO_DESACTIVAR",
        id,
        Map.of("activo", u.activo()),
        Map.of("activo", activo));
  }

  /** Restablece con una contraseña temporal de 16 caracteres y cierra todas las sesiones. */
  @Transactional
  public String restablecer(UUID actor, UUID id) {
    existente(id);
    String temporal = Secrets.temporaryPassword();
    users.fijarPasswordTemporal(id, encoder.encode(temporal));
    sessions.cerrarTodas(id);
    audit.registrar(
        actor, "USUARIO_RESTABLECER_PASSWORD", id, null, Map.of("debeCambiarPassword", true));
    return temporal;
  }

  private Usuario existente(UUID id) {
    return users.porId(id).orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
  }

  private static Map<String, Object> datos(Usuario u) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("correo", u.correo());
    m.put("cedula", u.cedula());
    m.put("nombres", u.nombres());
    m.put("roles", u.roles());
    m.put("carreras", u.carreras());
    return m;
  }

  private void unico(Usuario v) {
    if (users.porCorreo(v.correo()).filter(o -> !o.id().equals(v.id())).isPresent()) {
      throw new IllegalArgumentException("Ya existe un usuario con ese correo.");
    }
    if (users.porCedula(v.cedula()).filter(o -> !o.id().equals(v.id())).isPresent()) {
      throw new IllegalArgumentException("Ya existe un usuario con esa cédula.");
    }
  }
}
