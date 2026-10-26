package ec.edu.unl.geoasist.identidades.presentacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.identidades.aplicacion.*;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ApiSecurityConfig {
  @Bean
  public org.springframework.security.core.userdetails.UserDetailsService noDefaultUser() {
    return name -> {
      throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
          "Sin proveedor de acceso por defecto.");
    };
  }

  @Bean
  public PasswordEncoder passwords() {
    return new BCryptPasswordEncoder(10);
  }

  @Bean
  SecurityFilterChain chain(
      HttpSecurity http,
      AuthenticationService auth,
      Clock clock,
      ObjectMapper mapper,
      @Value("${geoasist.allowed-origin}") String origin)
      throws Exception {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(List.of(origin));
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
    cors.setExposedHeaders(List.of("Retry-After", "X-Correlation-ID"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/v1/**", cors);
    http.csrf(c -> c.disable())
        .cors(c -> c.configurationSource(source))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(c -> c.disable())
        .httpBasic(c -> c.disable())
        .formLogin(c -> c.disable());
    http.authorizeHttpRequests(
        a ->
            a.requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/guest-registrations",
                    "/api/v1/guest-registrations/submit",
                    "/api/v1/events/open/*")
                .permitAll()
                .requestMatchers("/api/v1/admin/**")
                .hasAuthority("ADMINISTRADOR")
                .anyRequest()
                .authenticated());
    http.exceptionHandling(
        e ->
            e.authenticationEntryPoint(
                    (r, s, x) -> problem(s, ApiFailure.unauthenticated(), mapper))
                .accessDeniedHandler((r, s, x) -> problem(s, ApiFailure.forbidden(), mapper)));
    http.addFilterBefore(
        new OncePerRequestFilter() {
          protected void doFilterInternal(
              HttpServletRequest req, HttpServletResponse res, FilterChain chain)
              throws ServletException, IOException {
            req.setAttribute(
                "received_at", clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
            String correlation =
                ec.edu.unl.geoasist.compartido.aplicacion.RequestCorrelation.begin().toString();
            res.setHeader("X-Correlation-ID", correlation);
            org.slf4j.MDC.put("correlation_id", correlation);
            res.setHeader("Cache-Control", "no-store");
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("Referrer-Policy", "no-referrer");
            String header = req.getHeader("Authorization");
            try {
              if (header != null) {
                if (!header.startsWith("Bearer ")) throw ApiFailure.unauthenticated();
                UserSession user = auth.authenticate(header.substring(7));
                String path = req.getRequestURI();
                if (user.mustChangePassword()
                    && !(path.equals("/api/v1/me/password")
                        || path.equals("/api/v1/me")
                        || path.equals("/api/v1/auth/logout")))
                  throw new ApiFailure(
                      403,
                      "CAMBIO_PASSWORD_REQUERIDO",
                      "Debes cambiar tu contraseña para continuar.");
                SecurityContextHolder.getContext()
                    .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.roles().stream().map(SimpleGrantedAuthority::new).toList()));
              }
              chain.doFilter(req, res);
            } catch (ApiFailure e) {
              problem(res, e, mapper);
            } finally {
              SecurityContextHolder.clearContext();
              ec.edu.unl.geoasist.compartido.aplicacion.RequestCorrelation.clear();
              org.slf4j.MDC.remove("correlation_id");
            }
          }
        },
        UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  private static void problem(HttpServletResponse response, ApiFailure e, ObjectMapper mapper)
      throws IOException {
    response.setStatus(e.status());
    response.setContentType("application/problem+json");
    mapper.writeValue(response.getOutputStream(), ApiProblemHandler.body(e));
  }
}
