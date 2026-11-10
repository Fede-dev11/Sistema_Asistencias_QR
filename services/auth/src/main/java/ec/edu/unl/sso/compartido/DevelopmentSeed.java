package ec.edu.unl.sso.compartido;

import ec.edu.unl.sso.usuarios.persistencia.UserRepository;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository.Usuario;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Datos ficticios de desarrollo: mismos correos y cédulas que la semilla de GeoAsist. La carrera
 * ("CARRERA-1") es el código que usan las semillas de GeoAsist.
 */
@Component
@Profile("dev")
class DevelopmentSeed implements ApplicationRunner {
  private static final String[] EMAILS = {
    "docente@example.test",
    "encargado@example.test",
    "estudiante@example.test",
    "director@example.test",
    "director-carrera@example.test",
    "otro-docente@example.test",
    "otro-estudiante@example.test",
    "inactiva@example.test"
  };
  private static final String[] CEDULAS = {
    "0100000017", "0100000025", "0100000033", "0100000041",
    "0100000058", "0100000066", "0100000074", "0100000082"
  };
  private static final String[] ROLES = {
    "DOCENTE",
    "ENCARGADO",
    "ESTUDIANTE",
    "DIRECTOR_INSTITUCIONAL",
    "DIRECTOR_CARRERA",
    "DOCENTE",
    "ESTUDIANTE",
    "ESTUDIANTE"
  };

  private final UserRepository users;
  private final JdbcClient db;
  private final PasswordEncoder encoder;
  private final String password;
  private final String serviceKey;
  private final String webReturn;

  DevelopmentSeed(
      UserRepository users,
      JdbcClient db,
      PasswordEncoder encoder,
      @Value("${sso.initial-password}") String password,
      @Value("${sso.service-key}") String serviceKey,
      @Value("${sso.web-return}") String webReturn) {
    this.users = users;
    this.db = db;
    this.encoder = encoder;
    this.password = password;
    this.serviceKey = serviceKey;
    this.webReturn = webReturn;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (password.length() < 12) {
      throw new IllegalArgumentException("DEV_SEED_PASSWORD requiere al menos 12 caracteres.");
    }
    String hash = encoder.encode(password);
    for (int i = 0; i < EMAILS.length; i++) {
      boolean career = ROLES[i].equals("DIRECTOR_CARRERA");
      seed(
          EMAILS[i],
          CEDULAS[i],
          "Persona ficticia " + (i + 1),
          hash,
          i != 7,
          List.of(ROLES[i]),
          career ? List.of("CARRERA-1") : List.of());
    }
    seed(
        "admin@dev.geoasist.local",
        "2400000002",
        "Administrador ficticio",
        hash,
        true,
        List.of("ADMINISTRADOR"),
        List.of());
    // Sin SSO_SERVICE_KEY la clave queda aleatoria (consulta de estado inutilizable).
    String keyHash = Secrets.sha256Hex(serviceKey.isBlank() ? Secrets.random() : serviceKey);
    service("geoasist-web", webReturn, keyHash);
    service("geoasist-movil", "geoasist://auth/callback", keyHash);
    service("ejemplo", "http://localhost:8082/", Secrets.sha256Hex(Secrets.random()));
  }

  private void seed(
      String correo,
      String cedula,
      String nombres,
      String hash,
      boolean activo,
      List<String> roles,
      List<String> carreras) {
    UUID id = UUID.nameUUIDFromBytes(correo.getBytes(StandardCharsets.UTF_8));
    users.insertar(new Usuario(id, correo, cedula, nombres, hash, activo, false, roles, carreras));
  }

  private void service(String nombre, String retorno, String keyHash) {
    db.sql(
            "INSERT INTO servicios(id, nombre, retornos, clave_hash, activo) "
                + "VALUES (?,?,string_to_array(?, ','),?,true) "
                + "ON CONFLICT (nombre) DO UPDATE SET retornos = EXCLUDED.retornos, clave_hash = EXCLUDED.clave_hash")
        .params(
            UUID.nameUUIDFromBytes(nombre.getBytes(StandardCharsets.UTF_8)),
            nombre,
            retorno,
            keyHash)
        .update();
  }
}
