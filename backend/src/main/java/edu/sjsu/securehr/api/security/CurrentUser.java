package edu.sjsu.securehr.api.security;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The authenticated caller, built only from the validated access token (Section 8.2: the caller's
 * emp_no never comes from the request).
 *
 * @param username Keycloak {@code preferred_username}
 * @param empNo linked employee number from the {@code emp_no} claim, or {@code null} if absent
 * @param roles SecureHR realm roles held by the caller
 */
public record CurrentUser(String username, Integer empNo, Set<Role> roles) {

  static final String USERNAME_CLAIM = "preferred_username";
  static final String EMP_NO_CLAIM = "emp_no";
  static final String REALM_ACCESS_CLAIM = "realm_access";
  static final String ROLES_KEY = "roles";

  /** Defensive copy so the role set cannot change after construction. */
  public CurrentUser {
    roles = roles.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(roles));
  }

  /**
   * Builds the caller from a token that has already passed signature, issuer, audience and expiry
   * validation.
   *
   * @param jwt validated access token
   * @return the caller
   */
  public static CurrentUser fromToken(Jwt jwt) {
    return new CurrentUser(
        jwt.getClaimAsString(USERNAME_CLAIM), parseEmpNo(jwt.getClaim(EMP_NO_CLAIM)), roles(jwt));
  }

  /** Whether the caller holds the given role. */
  public boolean hasRole(Role role) {
    return roles.contains(role);
  }

  /** Whether the caller is the employee with the given number. */
  public boolean isEmployee(int otherEmpNo) {
    return empNo != null && empNo == otherEmpNo;
  }

  static Set<Role> roles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
    if (realmAccess == null || !(realmAccess.get(ROLES_KEY) instanceof Collection<?> names)) {
      return Set.of();
    }
    Set<Role> roles = EnumSet.noneOf(Role.class);
    for (Object name : names) {
      Role.fromClaim(String.valueOf(name)).ifPresent(roles::add);
    }
    return roles;
  }

  // LDAP attribute mappers may emit emp_no as a number or a string; anything else is ignored.
  private static Integer parseEmpNo(Object claim) {
    if (claim instanceof Number number) {
      return number.intValue();
    }
    if (claim instanceof String text) {
      try {
        return Integer.valueOf(text.trim());
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }
}
