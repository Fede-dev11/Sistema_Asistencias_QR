package ec.edu.unl.geoasist.espacios;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.espacios.dominio.SpaceCode;
import org.junit.jupiter.api.Test;

class SpaceCodeTest {
  @Test
  void normalizesCaseSpacesAndHyphens() {
    assertEquals("A334", SpaceCode.normalize("a-334"));
    assertEquals("A334", SpaceCode.normalize(" A 334 "));
  }

  @Test
  void rejectsEmptyAndTooLong() {
    for (String bad : new String[] {null, "", " - ", "x".repeat(41)})
      assertThrows(ApiFailure.class, () -> SpaceCode.normalize(bad));
    assertEquals(40, SpaceCode.normalize("x".repeat(40)).length());
  }
}
