package edu.sjsu.securehr.api.support;

import edu.sjsu.securehr.api.TestcontainersConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full application against MySQL in Testcontainers with fixture data and test-signed tokens. Every
 * class using it shares one cached Spring context and one container.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  TestSecurityConfiguration.class,
  CapturedAuditEvents.class
})
public @interface ApiIntegrationTest {}
