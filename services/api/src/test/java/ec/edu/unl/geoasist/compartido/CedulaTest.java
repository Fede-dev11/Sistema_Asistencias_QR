package ec.edu.unl.geoasist.compartido;

import static org.junit.jupiter.api.Assertions.*;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.dominio.Cedula;
import org.junit.jupiter.api.Test;

class CedulaTest {
  @Test
  void acceptsValidProvincesAndTrims() {
    for (String ok : new String[] {"0100000017", "2400000002", "3000000004"})
      assertEquals(ok, Cedula.of(" " + ok + " ").value());
  }

  @Test
  void rejectsInvalidValues() {
    // 0100000018 dígito erróneo; 0160000006 tercer dígito 6 (verificador correcto); 0000000000 y
    // 2500000007 provincia 00/25
    for (String bad :
        new String[] {
          "0100000018",
          "010000001",
          "01000000177",
          "0160000006",
          "0000000000",
          "2500000007",
          "abcdefghij",
          "",
          null
        }) {
      var e = assertThrows(ApiFailure.class, () -> Cedula.of(bad), String.valueOf(bad));
      assertEquals("CEDULA_INVALIDA", e.code());
      assertEquals(400, e.status());
    }
  }
}
