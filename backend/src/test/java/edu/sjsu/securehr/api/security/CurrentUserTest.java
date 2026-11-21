package edu.sjsu.securehr.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CurrentUserTest {

  @Test
  void readsUsernameEmpNoAndOnlySecureHrRoles() {
    Jwt jwt =
        jwt(
            Map.of(
                "preferred_username",
                "demo.manager",
                "emp_no",
                110567,
                "realm_access",
                Map.of(
                    "roles",
                    List.of("EMPLOYEE", "MANAGER", "offline_access", "uma_authorization"))));

    CurrentUser user = CurrentUser.fromToken(jwt);

    assertThat(user.username()).isEqualTo("demo.manager");
    assertThat(user.empNo()).isEqualTo(110567);
    assertThat(user.roles()).containsExactlyInAnyOrder(Role.EMPLOYEE, Role.MANAGER);
    assertThat(user.isEmployee(110567)).isTrue();
    assertThat(user.hasRole(Role.HR_ADMIN)).isFalse();
  }

  @Test
  void acceptsEmpNoAsString() {
    Jwt jwt = jwt(Map.of("preferred_username", "u", "emp_no", "10012"));
    assertThat(CurrentUser.fromToken(jwt).empNo()).isEqualTo(10012);
  }

  @Test
  void ignoresInvalidEmpNoAndMissingRoles() {
    Jwt jwt = jwt(Map.of("preferred_username", "u", "emp_no", "not-a-number"));

    CurrentUser user = CurrentUser.fromToken(jwt);

    assertThat(user.empNo()).isNull();
    assertThat(user.roles()).isEmpty();
    assertThat(user.isEmployee(10012)).isFalse();
  }

  private static Jwt jwt(Map<String, Object> claims) {
    return new Jwt(
        "token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "RS256"), claims);
  }
}
