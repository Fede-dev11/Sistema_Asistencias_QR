package ec.edu.unl.sso.compartido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CedulaTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "0100000017",
        "0100000025",
        "0100000033",
        "0100000041",
        "0100000058",
        "2400000002"
      })
  void acceptsSeedCedulas(String value) {
    assertEquals(value, Cedula.of(value).value());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"", "abc", "010000001", "01000000170", "0100000018", "2500000001", "0160000011"})
  void rejectsInvalid(String value) {
    assertThrows(IllegalArgumentException.class, () -> Cedula.of(value));
  }

  @Test
  void rejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> Cedula.of(null));
  }
}
