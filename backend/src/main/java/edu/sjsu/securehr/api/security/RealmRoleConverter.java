package edu.sjsu.securehr.api.security;

import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps Keycloak realm roles ({@code realm_access.roles}, Section 0.5) to Spring Security
 * authorities. Only SecureHR roles are mapped; Keycloak's default roles grant nothing.
 */
public class RealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    List<GrantedAuthority> authorities =
        CurrentUser.roles(jwt).stream()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.authority()))
            .toList();
    return authorities;
  }
}
