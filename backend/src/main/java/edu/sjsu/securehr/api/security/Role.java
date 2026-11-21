package edu.sjsu.securehr.api.security;

import java.util.Optional;

/** Keycloak realm roles (Section 0.5). SECUREHR_DEV grants nothing in this API. */
public enum Role {
  EMPLOYEE,
  MANAGER,
  HR_ADMIN,
  AUDITOR,
  SECUREHR_DEV;

  /** Spring Security authority name for this role, e.g. {@code ROLE_HR_ADMIN}. */
  public String authority() {
    return "ROLE_" + name();
  }

  /**
   * Parses a realm role name; unknown roles (for example Keycloak defaults) are ignored.
   *
   * @param name role name from the token
   * @return the role, or empty if it is not a SecureHR role
   */
  public static Optional<Role> fromClaim(String name) {
    for (Role role : values()) {
      if (role.name().equals(name)) {
        return Optional.of(role);
      }
    }
    return Optional.empty();
  }
}
