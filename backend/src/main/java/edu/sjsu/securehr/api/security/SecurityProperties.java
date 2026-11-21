package edu.sjsu.securehr.api.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Token validation settings, bound from {@code KEYCLOAK_ISSUER_URI} and {@code API_AUDIENCE}.
 *
 * @param issuerUri expected {@code iss} claim and OIDC discovery location
 * @param audience value that must appear in the token's {@code aud} claim
 */
@Validated
@ConfigurationProperties("securehr.security")
public record SecurityProperties(@NotBlank String issuerUri, @NotBlank String audience) {}
