package ec.edu.unl.sso.cas.presentacion;

import ec.edu.unl.sso.cas.aplicacion.SsoSessions;
import ec.edu.unl.sso.cas.aplicacion.Tickets;
import ec.edu.unl.sso.cas.aplicacion.Tickets.Consumido;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository;
import ec.edu.unl.sso.usuarios.persistencia.UserRepository.Usuario;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/** GET /cas/p3/serviceValidate: XML por defecto, JSON con {@code format=JSON}. */
@RestController
class ValidateController {
  private static final MediaType XML = MediaType.parseMediaType("application/xml;charset=UTF-8");

  private record Failure(String code, String description) {}

  private final Tickets tickets;
  private final SsoSessions sessions;
  private final UserRepository users;
  private final tools.jackson.databind.ObjectMapper mapper;

  ValidateController(
      Tickets tickets,
      SsoSessions sessions,
      UserRepository users,
      tools.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.tickets = tickets;
    this.sessions = sessions;
    this.users = users;
  }

  @GetMapping("/cas/p3/serviceValidate")
  ResponseEntity<String> validate(
      @RequestParam(required = false) String service,
      @RequestParam(required = false) String ticket,
      @RequestParam(required = false) String format) {
    boolean json = "JSON".equalsIgnoreCase(format);
    Usuario user = null;
    Failure failure = null;
    if (blank(service) || blank(ticket)) {
      failure = new Failure("INVALID_REQUEST", "Se requieren los parámetros service y ticket.");
    } else if (!ticket.startsWith("ST-") || ticket.length() > 256) {
      failure = new Failure("INVALID_TICKET", "El ticket no es válido.");
    } else {
      Optional<Consumido> used = tickets.consumir(ticket);
      if (used.isEmpty()) {
        failure = new Failure("INVALID_TICKET", "El ticket no es válido, ya se usó o venció.");
      } else if (!used.get().service().equals(service)) {
        // el ticket queda consumido: no se puede reintentar con otro servicio
        failure = new Failure("INVALID_SERVICE", "El ticket no corresponde a este servicio.");
      } else {
        user = sessions.usuarioActivoDe(used.get().sesionId()).flatMap(users::porId).orElse(null);
        if (user == null) failure = new Failure("INVALID_TICKET", "La sesión ya no es válida.");
      }
    }
    return json ? json(user, failure) : xml(user, failure);
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  private static Map<String, List<String>> attributes(Usuario u) {
    Map<String, List<String>> a = new LinkedHashMap<>();
    a.put("correo", List.of(u.correo()));
    a.put("cedula", List.of(u.cedula()));
    a.put("nombres", List.of(u.nombres()));
    a.put("roles", u.roles());
    a.put("carreras", u.carreras());
    a.put("activo", List.of(Boolean.toString(u.activo())));
    return a;
  }

  private ResponseEntity<String> json(Usuario user, Failure failure) {
    Map<String, Object> body = new LinkedHashMap<>();
    if (failure != null) {
      body.put(
          "authenticationFailure",
          Map.of("code", failure.code(), "description", failure.description()));
    } else {
      body.put(
          "authenticationSuccess", Map.of("user", user.correo(), "attributes", attributes(user)));
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(mapper.writeValueAsString(Map.of("serviceResponse", body)));
  }

  private ResponseEntity<String> xml(Usuario user, Failure failure) {
    StringBuilder x =
        new StringBuilder("<cas:serviceResponse xmlns:cas=\"http://www.yale.edu/tp/cas\">");
    if (failure != null) {
      x.append("<cas:authenticationFailure code=\"")
          .append(failure.code())
          .append("\">")
          .append(HtmlUtils.htmlEscape(failure.description()))
          .append("</cas:authenticationFailure>");
    } else {
      x.append("<cas:authenticationSuccess><cas:user>")
          .append(HtmlUtils.htmlEscape(user.correo()))
          .append("</cas:user><cas:attributes>");
      attributes(user)
          .forEach(
              (name, values) ->
                  values.forEach(
                      v ->
                          x.append("<cas:")
                              .append(name)
                              .append('>')
                              .append(HtmlUtils.htmlEscape(v))
                              .append("</cas:")
                              .append(name)
                              .append('>')));
      x.append("</cas:attributes></cas:authenticationSuccess>");
    }
    x.append("</cas:serviceResponse>");
    return ResponseEntity.ok().contentType(XML).body(x.toString());
  }
}
