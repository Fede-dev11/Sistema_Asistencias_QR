package ec.edu.unl.sso.servicios.aplicacion;

import ec.edu.unl.sso.servicios.persistencia.ServiceRegistry;
import ec.edu.unl.sso.servicios.persistencia.ServiceRegistry.Servicio;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Decide a qué servicio registrado pertenece un {@code service}. Esquema, host y puerto deben ser
 * idénticos (sin comodines, sensibles a mayúsculas); la ruta, por prefijo de segmentos. Sin
 * redirecciones abiertas: se rechazan credenciales en la URL, fragmentos, rutas con {@code ..} y
 * {@code http} fuera de localhost.
 */
@Service
public class ServiceMatcher {
  private final ServiceRegistry registry;

  ServiceMatcher(ServiceRegistry registry) {
    this.registry = registry;
  }

  public Optional<Servicio> match(String service) {
    URI uri = parse(service);
    if (uri == null) return Optional.empty();
    return registry.activos().stream()
        .filter(s -> s.retornos().stream().anyMatch(prefix -> covers(parse(prefix), uri)))
        .findFirst();
  }

  /** Un prefijo de retorno es válido si cumple las mismas reglas que una URL de servicio. */
  public static boolean validPrefix(String raw) {
    return parse(raw) != null;
  }

  private static URI parse(String raw) {
    if (raw == null || raw.length() > 2048 || !raw.matches("[\\x21-\\x7E]+")) return null;
    try {
      URI u = new URI(raw);
      if (u.getScheme() == null || u.getRawAuthority() == null || u.getRawUserInfo() != null) {
        return null;
      }
      if (u.getRawFragment() != null || raw.contains("\\")) return null;
      if (u.getHost() == null || u.getPath() == null) return null;
      String lowerPath = u.getRawPath().toLowerCase();
      if (lowerPath.contains("%2e") || lowerPath.contains("%2f") || lowerPath.contains("%5c")) {
        return null;
      }
      for (String segment : u.getPath().split("/")) if (segment.equals("..")) return null;
      if (u.getScheme().equals("http") && !u.getHost().equals("localhost")) return null;
      return u;
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private static boolean covers(URI prefix, URI uri) {
    if (prefix == null) return false;
    if (!prefix.getScheme().equals(uri.getScheme())
        || !prefix.getHost().equals(uri.getHost())
        || prefix.getPort() != uri.getPort()) {
      return false;
    }
    String p = prefix.getPath();
    String path = uri.getPath();
    return path.equals(p) || (p.endsWith("/") && path.startsWith(p)) || path.startsWith(p + "/");
  }
}
