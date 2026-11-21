package edu.sjsu.securehr.api.security;

import edu.sjsu.securehr.api.audit.AuditEventType;
import edu.sjsu.securehr.api.audit.AuditOutcome;
import edu.sjsu.securehr.api.audit.AuditTrail;
import edu.sjsu.securehr.api.exception.ApiErrorWriter;
import edu.sjsu.securehr.api.exception.ResourceAccessDeniedException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Zero-trust request security (Sections 5.3 and 8.2). Every request must carry a valid Keycloak
 * access token; URL rules give each endpoint an explicit role list and deny everything else.
 * Per-record rules (self, manager scope, salary) are enforced by the service-layer policies.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

  /** Business roles allowed to use the portal endpoints. AUDITOR is deliberately absent. */
  private static final String[] PORTAL_ROLES = {
    Role.EMPLOYEE.name(), Role.MANAGER.name(), Role.HR_ADMIN.name()
  };

  @Bean
  SecurityFilterChain apiSecurity(
      HttpSecurity http,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable) // Stateless bearer tokens; no cookies to forge.
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth ->
            auth
                // Internal only: the edge does not route it (Section 0.3).
                .requestMatchers(HttpMethod.GET, "/api/health")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/export/employees")
                .hasRole(Role.HR_ADMIN.name())
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/me",
                    "/api/employees",
                    "/api/employees/*",
                    "/api/employees/*/titles",
                    "/api/employees/*/salaries",
                    "/api/departments",
                    "/api/org/*")
                .hasAnyRole(PORTAL_ROLES)
                // Deny by default (Section 8.2).
                .anyRequest()
                .denyAll())
        .oauth2ResourceServer(oauth2 ->
            oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(authenticationEntryPoint))
        .exceptionHandling(e ->
            e.authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));
    return http.build();
  }

  /**
   * Decoder that verifies signatures against the realm's published keys. Discovery is deferred to
   * the first request, so the API starts even if Keycloak is not up yet.
   */
  @Bean
  JwtDecoder jwtDecoder(SecurityProperties properties) {
    return new SupplierJwtDecoder(() -> {
      NimbusJwtDecoder decoder =
          (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(properties.issuerUri());
      decoder.setJwtValidator(TokenValidators.forApi(properties));
      return decoder;
    });
  }

  /** 401 for a missing or invalid token, in the Section 0.6 error format. */
  @Bean
  AuthenticationEntryPoint authenticationEntryPoint(ApiErrorWriter errorWriter) {
    return (request, response, exception) ->
        errorWriter.write(response, HttpStatus.UNAUTHORIZED, "A valid access token is required.");
  }

  /** 403 for an authenticated caller without the required role; every denial is audited. */
  @Bean
  AccessDeniedHandler accessDeniedHandler(ApiErrorWriter errorWriter, AuditTrail auditTrail) {
    return (request, response, exception) -> {
      Integer target =
          exception instanceof ResourceAccessDeniedException denied
              ? denied.getTargetEmpNo()
              : null;
      auditTrail.record(
          AuditEventType.ACCESS_DENIED, AuditOutcome.DENIED, currentUser(request), target);
      errorWriter.write(
          response, HttpStatus.FORBIDDEN, "You are not allowed to perform this action.");
    };
  }

  private static JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new RealmRoleConverter());
    converter.setPrincipalClaimName(CurrentUser.USERNAME_CLAIM);
    return converter;
  }

  private static CurrentUser currentUser(jakarta.servlet.http.HttpServletRequest request) {
    return request.getUserPrincipal() instanceof Authentication auth
            && auth.getPrincipal() instanceof Jwt jwt
        ? CurrentUser.fromToken(jwt)
        : null;
  }
}
