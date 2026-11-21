package edu.sjsu.securehr.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class TokenValidatorsTest {

  private static final String ISSUER = "https://auth.securehr.local/realms/securehr";

  private final OAuth2TokenValidator<Jwt> validator =
      TokenValidators.forApi(new SecurityProperties(ISSUER, "securehr-api"));

  @Test
  void acceptsTokenForThisApiAmongOtherAudiences() {
    assertThat(
            validator.validate(jwt(ISSUER, List.of("securehr-docs", "securehr-api"))).hasErrors())
        .isFalse();
  }

  @Test
  void rejectsTokenForAnotherAudience() {
    assertThat(validator.validate(jwt(ISSUER, List.of("securehr-docs"))).hasErrors()).isTrue();
  }

  @Test
  void rejectsTokenFromAnotherIssuer() {
    assertThat(
            validator.validate(jwt("https://other/realms/x", List.of("securehr-api"))).hasErrors())
        .isTrue();
  }

  private static Jwt jwt(String issuer, List<String> audience) {
    Instant now = Instant.now();
    return new Jwt(
        "token",
        now,
        now.plusSeconds(300),
        Map.of("alg", "RS256"),
        Map.of("iss", issuer, "aud", audience, "sub", "s"));
  }
}
