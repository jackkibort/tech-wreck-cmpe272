package edu.sjsu.securehr.api.security;

import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

/**
 * The single validation chain applied to every access token (Section 5.3): expiry and not-before
 * (with default clock skew), issuer, and audience. Signature checks happen in the decoder.
 * Production and tests share this chain so the tests prove the real rules.
 */
public final class TokenValidators {

  private TokenValidators() {}

  /**
   * Builds the validator chain.
   *
   * @param properties expected issuer and audience
   * @return validator that fails unless every check passes
   */
  public static OAuth2TokenValidator<Jwt> forApi(SecurityProperties properties) {
    return new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(properties.issuerUri()),
        audience(properties.audience()));
  }

  // Tokens minted for another API (e.g. only securehr-docs) must not be replayed here.
  private static OAuth2TokenValidator<Jwt> audience(String expected) {
    OAuth2Error error =
        new OAuth2Error("invalid_token", "The token is not intended for this API", null);
    return jwt -> {
      List<String> audiences = jwt.getAudience();
      return audiences != null && audiences.contains(expected)
          ? OAuth2TokenValidatorResult.success()
          : OAuth2TokenValidatorResult.failure(error);
    };
  }
}
