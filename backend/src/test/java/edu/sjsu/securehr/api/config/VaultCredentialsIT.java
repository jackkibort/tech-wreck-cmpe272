package edu.sjsu.securehr.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.sjsu.securehr.api.SecureHrApiApplication;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.vault.VaultContainer;

/**
 * The running API gets its database credentials from Vault through AppRole, exactly as in Section
 * 0.7, and refuses to start when Vault does not hand them over. Starts the real application, not a
 * test slice, because the credentials are resolved while the configuration is being read.
 */
class VaultCredentialsIT {

  private static final String VAULT_IMAGE = "hashicorp/vault:1.20.4";
  private static final String ROOT_TOKEN = "test-only-root-token";
  private static final String APP_USER = "securehr_app";
  private static final String APP_PASSWORD = "vault-" + UUID.randomUUID();

  private static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.4.13")
          .withDatabaseName("employees")
          .withUsername(APP_USER)
          .withPassword(APP_PASSWORD);

  private static final VaultContainer<?> VAULT =
      new VaultContainer<>(VAULT_IMAGE).withVaultToken(ROOT_TOKEN);

  @BeforeAll
  static void startAndSeed() throws Exception {
    MYSQL.start();
    VAULT.start();
    // Mirrors the Section 0.7 contract that /secrets seeds in the real stack.
    vault("vault auth enable approle");
    vault("vault kv put secret/securehr/api/db username=" + APP_USER + " password=" + APP_PASSWORD);
    vault("vault kv put secret/securehr/docs/db username=docs_app password=not-for-the-api");
    vault(
        "echo 'path \"secret/data/securehr/api/*\" { capabilities = [\"read\"] }'"
            + " | vault policy write securehr-api -");
    vault("vault write auth/approle/role/securehr-api token_policies=securehr-api");
    vault("vault write auth/approle/role/no-secrets token_policies=default");
  }

  @AfterAll
  static void stop() {
    VAULT.stop();
    MYSQL.stop();
  }

  @Test
  void databaseCredentialsComeFromVault() throws Exception {
    try (ConfigurableApplicationContext app =
        start(roleId("securehr-api"), secretId("securehr-api"))) {
      assertThat(app.getEnvironment().getProperty("spring.datasource.password"))
          .isEqualTo(APP_PASSWORD);
      try (Connection connection = app.getBean(DataSource.class).getConnection()) {
        assertThat(connection.getMetaData().getUserName()).startsWith(APP_USER + "@");
      }
      // Nothing besides the AppRole IDs was given to the process.
      assertThat(System.getenv()).doesNotContainValue(APP_PASSWORD);
    }
  }

  @Test
  void wrongSecretId_preventsStartup() {
    assertThatThrownBy(() -> start(roleId("securehr-api"), "not-a-real-secret-id").close())
        .hasStackTraceContaining("Cannot login using AppRole");
  }

  @Test
  void roleWithoutAccessToTheApiPath_preventsStartup() {
    assertThatThrownBy(() -> start(roleId("no-secrets"), secretId("no-secrets")).close())
        .hasStackTraceContaining("403 Forbidden");
  }

  private static ConfigurableApplicationContext start(String roleId, String secretId) {
    Map<String, Object> env = new HashMap<>();
    env.put("VAULT_ADDR", VAULT.getHttpHostAddress());
    env.put("API_VAULT_ROLE_ID", roleId);
    env.put("API_VAULT_SECRET_ID", secretId);
    env.put("MYSQL_HOST", MYSQL.getHost());
    env.put("MYSQL_PORT", MYSQL.getFirstMappedPort());
    env.put("MYSQL_DATABASE", "employees");
    env.put("KEYCLOAK_ISSUER_URI", "https://keycloak.invalid/realms/securehr");
    env.put("server.port", 0);
    return new SpringApplicationBuilder(SecureHrApiApplication.class).properties(env).run();
  }

  private static String roleId(String role) throws Exception {
    return vault("vault read -field=role_id auth/approle/role/" + role + "/role-id");
  }

  private static String secretId(String role) throws Exception {
    return vault("vault write -f -field=secret_id auth/approle/role/" + role + "/secret-id");
  }

  /** Runs a shell command in the Vault container as root; {@code vault} is the Vault CLI. */
  private static String vault(String command) throws Exception {
    ExecResult result =
        VAULT.execInContainer("/bin/sh", "-c", "export VAULT_TOKEN=" + ROOT_TOKEN + "; " + command);
    assertThat(result.getExitCode()).as(result.getStderr()).isZero();
    return result.getStdout().strip();
  }
}
