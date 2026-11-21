package edu.sjsu.securehr.api.support;

import edu.sjsu.securehr.api.security.SecurityProperties;
import edu.sjsu.securehr.api.security.TokenValidators;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Replaces only the key source: tokens are verified against the test signing key instead of
 * Keycloak's published keys, while issuer, audience and expiry go through the production {@link
 * TokenValidators} chain unchanged.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfiguration {

  @Bean
  @Primary
  JwtDecoder testJwtDecoder(SecurityProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withPublicKey(TestTokens.trustedPublicKey()).build();
    decoder.setJwtValidator(TokenValidators.forApi(properties));
    return decoder;
  }
}
