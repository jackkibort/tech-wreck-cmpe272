package edu.sjsu.securehr.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class HealthEndpointIT {

  @Autowired private TestRestTemplate rest;

  @Test
  void healthReportsOnlyUpOrDown() {
    ResponseEntity<String> response = rest.getForEntity("/api/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Section 0.6: status only, no component or detail leakage.
    assertThat(response.getBody()).isEqualTo("{\"status\":\"UP\"}");
  }
}
