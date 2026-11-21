package edu.sjsu.securehr.api.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import edu.sjsu.securehr.api.security.Role;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Mints RS256 access tokens shaped like Keycloak's (Section 0.5) for tests. Tokens are signed with
 * a per-JVM test key; {@link #signedWithUntrustedKey()} simulates a forged token.
 */
public final class TestTokens {

  public static final String ISSUER = "https://auth.securehr.test/realms/securehr";
  public static final String API_AUDIENCE = "securehr-api";
  public static final String DOCS_AUDIENCE = "securehr-docs";

  private static final KeyPair TRUSTED_KEY = newKeyPair();
  private static final KeyPair UNTRUSTED_KEY = newKeyPair();

  private String username;
  private Object empNo;
  private List<String> roles = List.of();
  private List<String> audiences = List.of(API_AUDIENCE, DOCS_AUDIENCE);
  private String issuer = ISSUER;
  private Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
  private KeyPair signingKey = TRUSTED_KEY;

  private TestTokens() {}

  /** Starts a token for a directory user with the given roles. */
  public static TestTokens user(String username, Integer empNo, Role... roles) {
    TestTokens token = new TestTokens();
    token.username = username;
    token.empNo = empNo;
    token.roles = Arrays.stream(roles).map(Role::name).toList();
    return token;
  }

  /** Public key matching the trusted signing key, for the test decoder. */
  public static RSAPublicKey trustedPublicKey() {
    return (RSAPublicKey) TRUSTED_KEY.getPublic();
  }

  public TestTokens audiences(String... values) {
    this.audiences = List.of(values);
    return this;
  }

  public TestTokens issuer(String value) {
    this.issuer = value;
    return this;
  }

  public TestTokens expired() {
    this.expiresAt = Instant.now().minus(Duration.ofHours(1));
    return this;
  }

  public TestTokens empNoClaim(Object value) {
    this.empNo = value;
    return this;
  }

  public TestTokens signedWithUntrustedKey() {
    this.signingKey = UNTRUSTED_KEY;
    return this;
  }

  /** Signs the token and returns the compact serialization. */
  public String build() {
    JWTClaimsSet.Builder claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("sub-" + username)
            .audience(audiences)
            .issueTime(Date.from(expiresAt.minus(Duration.ofMinutes(5))))
            .expirationTime(Date.from(expiresAt))
            .claim("preferred_username", username)
            .claim("realm_access", Map.of("roles", roles));
    if (empNo != null) {
      claims.claim("emp_no", empNo);
    }
    try {
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
      jwt.sign(new RSASSASigner(signingKey.getPrivate()));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  /** {@code Authorization} header value for this token. */
  public String bearer() {
    return "Bearer " + build();
  }

  private static KeyPair newKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
