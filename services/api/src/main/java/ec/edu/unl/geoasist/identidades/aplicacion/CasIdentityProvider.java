package ec.edu.unl.geoasist.identidades.aplicacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import tools.jackson.databind.JsonNode;

@Component
public class CasIdentityProvider implements IdentityProvider {
  private final RestClient client;
  private final String serviceKey;

  public CasIdentityProvider(RestClient.Builder builder, @Value("${geoasist.sso.internal-url}") String internalUrl, @Value("${geoasist.sso.service-key}") String serviceKey) {
    var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    var factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(2));
    client = builder.baseUrl(internalUrl).requestFactory(factory).build();
    this.serviceKey = serviceKey;
  }

  public VerifiedIdentity verifyTicket(String ticket, String service) {
    try {
      JsonNode response = client.get().uri(b -> b.path("/cas/p3/serviceValidate").queryParam("format", "JSON").queryParam("ticket", ticket).queryParam("service", service).build()).retrieve().body(JsonNode.class);
      JsonNode root = response == null ? null : response.path("serviceResponse");
      if (root == null || root.has("authenticationFailure")) throw invalidTicket();
      JsonNode success = root.path("authenticationSuccess"), attributes = success.path("attributes");
      if (success.isMissingNode() || attributes.isMissingNode()) throw invalidTicket();
      return new VerifiedIdentity(first(attributes, "correo", success.path("user").asText()), first(attributes, "cedula", ""), first(attributes, "nombres", ""), values(attributes.path("roles")), values(attributes.path("carreras")), Boolean.parseBoolean(first(attributes, "activo", "false")));
    } catch (ApiFailure e) { throw e; }
    catch (RestClientException e) { throw unavailable(); }
  }

  public boolean isActive(String correo) {
    try {
      JsonNode response = client.get().uri(b -> b.path("/api/v1/users/status").queryParam("correo", correo).build()).header("X-Service-Key", serviceKey).retrieve().body(JsonNode.class);
      return response != null && response.path("activo").asBoolean(false);
    } catch (RestClientException e) { throw unavailable(); }
  }

  private static String first(JsonNode a, String name, String fallback) {
    JsonNode v = a.path(name);
    if (v.isArray() && !v.isEmpty()) return v.get(0).asText();
    return v.isTextual() ? v.asText() : fallback;
  }
  private static List<String> values(JsonNode v) {
    if (v.isArray()) { List<String> result = new ArrayList<>(); v.forEach(n -> result.add(n.asText())); return List.copyOf(result); }
    return v.isTextual() ? List.of(v.asText()) : List.of();
  }
  private static ApiFailure invalidTicket() { return new ApiFailure(401, "TICKET_INVALIDO", "El ticket universitario no es válido."); }
  private static ApiFailure unavailable() { return new ApiFailure(502, "SSO_NO_DISPONIBLE", "El servicio universitario no está disponible."); }
}
