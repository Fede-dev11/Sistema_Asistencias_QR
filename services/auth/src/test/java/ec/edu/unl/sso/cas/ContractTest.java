package ec.edu.unl.sso.cas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ec.edu.unl.sso.soporte.AuthFixture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/** auth-openapi.yaml y la aplicación deben describir las mismas operaciones y respuestas. */
class ContractTest extends AuthFixture {
  private static final Path YAML =
      Path.of("../../specs/004-autenticacion-sso/contracts/auth-openapi.yaml");

  @Autowired RequestMappingHandlerMapping mappings;

  @SuppressWarnings("unchecked")
  private Map<String, Object> spec() throws Exception {
    return new Yaml().load(Files.readString(YAML));
  }

  @Test
  void documentedOperationsAreExactlyTheImplementedOnes() throws Exception {
    Set<String> documented = new HashSet<>();
    ((Map<String, Map<String, Object>>) spec().get("paths"))
        .forEach(
            (path, ops) ->
                ops.keySet().stream()
                    .filter(k -> List.of("get", "post").contains(k))
                    .forEach(m -> documented.add(m.toUpperCase() + " " + path)));
    Set<String> implemented = new HashSet<>();
    for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
      for (String p : info.getPathPatternsCondition().getPatternValues()) {
        if (!p.startsWith("/cas") && !p.startsWith("/api")) continue;
        info.getMethodsCondition().getMethods().forEach(m -> implemented.add(m + " " + p));
      }
    }
    assertEquals(implemented, documented);
  }

  @Test
  @SuppressWarnings("unchecked")
  void failureCodesMatchTheContract() throws Exception {
    Map<String, Object> schemas =
        (Map<String, Object>) ((Map<String, Object>) spec().get("components")).get("schemas");
    Map<String, Object> failure =
        (Map<String, Object>)
            ((Map<String, Object>)
                    ((Map<String, Object>)
                            ((Map<String, Object>) schemas.get("ServiceResponse"))
                                .get("properties"))
                        .get("serviceResponse"))
                .get("properties");
    List<String> codes =
        (List<String>)
            ((Map<String, Object>)
                    ((Map<String, Object>)
                            ((Map<String, Object>) failure.get("authenticationFailure"))
                                .get("properties"))
                        .get("code"))
                .get("enum");
    assertEquals(List.of("INVALID_REQUEST", "INVALID_TICKET", "INVALID_SERVICE"), codes);
  }

  @Test
  void realResponsesUseDocumentedShapeAndStatuses() throws Exception {
    String t = ticket(tgc("docente@example.test"), WEB);
    String json =
        mvc.perform(
                get("/cas/p3/serviceValidate")
                    .param("service", WEB)
                    .param("ticket", t)
                    .param("format", "JSON"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    for (String attr : List.of("correo", "cedula", "nombres", "roles", "carreras", "activo")) {
      assertTrue(json.contains("\"" + attr + "\":["), attr);
    }
    assertEquals(
        401,
        mvc.perform(get("/api/v1/users/status").param("correo", "docente@example.test"))
            .andReturn()
            .getResponse()
            .getStatus());
    var ok =
        mvc.perform(
                get("/api/v1/users/status")
                    .param("correo", "docente@example.test")
                    .header("X-Service-Key", "fixture-service-key"))
            .andReturn()
            .getResponse();
    assertEquals(200, ok.getStatus());
    assertEquals("{\"activo\":true}", ok.getContentAsString());
    assertEquals(
        404,
        mvc.perform(
                get("/api/v1/users/status")
                    .param("correo", "nadie@example.test")
                    .header("X-Service-Key", "fixture-service-key"))
            .andReturn()
            .getResponse()
            .getStatus());
    assertEquals(
        401,
        mvc.perform(
                get("/api/v1/users/status")
                    .param("correo", "docente@example.test")
                    .header("X-Service-Key", "otra"))
            .andReturn()
            .getResponse()
            .getStatus());
  }
}
