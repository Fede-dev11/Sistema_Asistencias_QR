package ec.edu.unl.sso.compartido;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Spring Security solo aporta CSRF en formularios, cabeceras (Cache-Control: no-store,
 * X-Frame-Options: DENY) y BCrypt; la sesión SSO es propia (CASTGC).
 */
@Configuration
class SecurityConfig {
  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    CookieCsrfTokenRepository csrf = new CookieCsrfTokenRepository();
    csrf.setCookiePath("/");
    csrf.setCookieCustomizer(c -> c.sameSite("Lax"));
    http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .csrf(c -> c.csrfTokenRepository(csrf))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            h ->
                h.frameOptions(f -> f.deny())
                    .contentSecurityPolicy(
                        p -> p.policyDirectives("default-src 'self'; frame-ancestors 'none'")))
        .formLogin(f -> f.disable())
        .httpBasic(b -> b.disable())
        .logout(l -> l.disable());
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
