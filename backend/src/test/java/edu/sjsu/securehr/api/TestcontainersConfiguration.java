package edu.sjsu.securehr.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts the same MySQL version as production for integration tests. Spring's test-context cache
 * shares one container across every test class that imports this configuration.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  /** Must match the image pinned in /database/compose.yaml. */
  static final String MYSQL_IMAGE = "mysql:8.4.13";

  @Bean
  @ServiceConnection
  MySQLContainer<?> mysqlContainer() {
    // Root is used only inside the throwaway container so Flyway can create the fixture schema.
    return new MySQLContainer<>(DockerImageName.parse(MYSQL_IMAGE))
        .withDatabaseName("employees")
        .withUsername("root")
        .withPassword("test-only-root");
  }
}
