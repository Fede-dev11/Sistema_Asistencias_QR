package ec.edu.unl.geoasist.identidades.aplicacion;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevelopmentSeed implements ApplicationRunner {
  private final JdbcTemplate db;
  private final PasswordEncoder passwords;
  private final String password;

  public DevelopmentSeed(
      JdbcTemplate db,
      PasswordEncoder passwords,
      @Value("${geoasist.dev-seed-password}") String password) {
    this.db = db;
    this.passwords = passwords;
    this.password = password;
  }

  public static UUID id(int n) {
    return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(n));
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (password.length() < 12)
      throw new IllegalArgumentException("DEV_SEED_PASSWORD requiere al menos 12 caracteres.");
    String[] emails = {
      "docente",
      "encargado",
      "estudiante",
      "director",
      "director-carrera",
      "otro-docente",
      "otro-estudiante",
      "inactiva"
    };
    String[] roles = {
      "DOCENTE",
      "ENCARGADO",
      "ESTUDIANTE",
      "DIRECTOR_INSTITUCIONAL",
      "DIRECTOR_CARRERA",
      "DOCENTE",
      "ESTUDIANTE",
      "ESTUDIANTE"
    };
    String encoded = passwords.encode(password);
    for (int i = 0; i < emails.length; i++) {
      db.update(
          "INSERT INTO personas VALUES (?,?,?, 'INSTITUCIONAL') ON CONFLICT DO NOTHING",
          id(100 + i),
          "%010d".formatted(i + 1),
          "Persona ficticia " + (i + 1));
      db.update(
          "INSERT INTO cuentas VALUES (?,?, 'DEV',?,?,?) ON CONFLICT DO NOTHING",
          id(200 + i),
          id(100 + i),
          emails[i] + "@example.test",
          encoded,
          i != 7);
      db.update(
          "INSERT INTO roles_cuenta VALUES (?,?) ON CONFLICT DO NOTHING", id(200 + i), roles[i]);
    }
    for (int i = 1; i <= 2; i++) {
      db.update(
          "INSERT INTO carreras VALUES (?,?,?) ON CONFLICT DO NOTHING",
          id(i),
          "CARRERA-" + i,
          "Carrera ficticia " + i);
      db.update(
          "INSERT INTO paralelos VALUES (?,?,?) ON CONFLICT DO NOTHING", id(10 + i), id(i), "A");
    }
    db.update(
        "INSERT INTO direcciones_carrera VALUES (?,?,true) ON CONFLICT DO NOTHING", id(204), id(1));
    for (int i = 1; i <= 3; i++) {
      UUID activity = id(300 + i);
      db.update(
          "INSERT INTO actividades(id,nombre,modalidad,responsable_id) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",
          activity,
          "Actividad ficticia " + i,
          i == 1 ? "CLASE" : "EVENTO",
          id(i == 1 ? 200 : i == 2 ? 201 : 205));
      db.update(
          "INSERT INTO actividad_carreras VALUES (?,?) ON CONFLICT DO NOTHING",
          activity,
          id(i == 3 ? 2 : 1));
      db.update(
          "INSERT INTO actividad_paralelos VALUES (?,?,?) ON CONFLICT DO NOTHING",
          activity,
          id(i == 3 ? 2 : 1),
          id(i == 3 ? 12 : 11));
      db.update(
          "INSERT INTO sesiones(id,actividad_id,ventana_inicio,ventana_fin) VALUES (?,?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 year') ON CONFLICT DO NOTHING",
          id(400 + i),
          activity);
    }
    db.update(
        "INSERT INTO estudiantes_autorizados VALUES (?,?,true) ON CONFLICT DO NOTHING",
        id(301),
        id(102));
  }
}
