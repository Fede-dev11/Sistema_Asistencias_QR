package ec.edu.unl.geoasist.soporte;

import java.util.UUID;

public final class ScenarioFixtures {
  private ScenarioFixtures() {}

  public static UUID id(int n) {
    return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(n));
  }

  public static final String PASSWORD = "fixture-password";
  public static final String TEACHER = "docente@example.test";
  public static final String STUDENT = "estudiante@example.test";
  public static final String DIRECTOR = "director@example.test";
}
