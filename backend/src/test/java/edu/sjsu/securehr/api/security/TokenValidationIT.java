package edu.sjsu.securehr.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.sjsu.securehr.api.audit.AuditEvent;
import edu.sjsu.securehr.api.audit.AuditEventType;
import edu.sjsu.securehr.api.audit.AuditOutcome;
import edu.sjsu.securehr.api.support.ApiIntegrationTest;
import edu.sjsu.securehr.api.support.CapturedAuditEvents;
import edu.sjsu.securehr.api.support.TestTokens;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** Authentication and coarse role rules: TC-API-01, TC-API-02, TC-API-08 (Section 13.2). */
@ApiIntegrationTest
class TokenValidationIT {

  private static final String ANY_PORTAL_ENDPOINT = "/api/departments";

  @Autowired private MockMvc mvc;
  @Autowired private CapturedAuditEvents audit;

  @BeforeEach
  void clearAudit() {
    audit.clear();
  }

  @Test
  void tcApi01_requestWithoutToken_isUnauthorized() throws Exception {
    mvc.perform(get(ANY_PORTAL_ENDPOINT).header("X-Request-ID", "req-tc-api-01"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.requestId").value("req-tc-api-01"));
  }

  @Test
  void tcApi02_expiredToken_isUnauthorized() throws Exception {
    expectUnauthorized(employee().expired().bearer());
  }

  @Test
  void tcApi02_forgedSignature_isUnauthorized() throws Exception {
    expectUnauthorized(employee().signedWithUntrustedKey().bearer());
  }

  @Test
  void tcApi02_wrongAudience_isUnauthorized() throws Exception {
    // A token minted only for the Document API must not be accepted here.
    expectUnauthorized(employee().audiences(TestTokens.DOCS_AUDIENCE).bearer());
  }

  @Test
  void tcApi02_wrongIssuer_isUnauthorized() throws Exception {
    expectUnauthorized(employee().issuer("https://evil.example/realms/securehr").bearer());
  }

  @Test
  void tcApi02_malformedToken_isUnauthorized() throws Exception {
    expectUnauthorized("Bearer not-a-jwt");
  }

  @Test
  void validEmployeeToken_passesAuthentication() throws Exception {
    int status =
        mvc.perform(get(ANY_PORTAL_ENDPOINT).header(HttpHeaders.AUTHORIZATION, employee().bearer()))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isNotIn(401, 403);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/me",
        "/api/employees",
        "/api/employees/10001",
        "/api/employees/10001/titles",
        "/api/employees/10001/salaries",
        "/api/departments",
        "/api/org/10001",
        "/api/export/employees"
      })
  void tcApi08_auditorCallsAnyEndpoint_isForbiddenAndAudited(String path) throws Exception {
    String auditor = TestTokens.user("demo.auditor", null, Role.AUDITOR).bearer();

    mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, auditor))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));

    List<AuditEvent> denied = audit.ofType(AuditEventType.ACCESS_DENIED);
    assertThat(denied).hasSize(1);
    assertThat(denied.get(0).actorUsername()).isEqualTo("demo.auditor");
    assertThat(denied.get(0).actorRoles()).containsExactly("AUDITOR");
    assertThat(denied.get(0).outcome()).isEqualTo(AuditOutcome.DENIED);
    assertThat(denied.get(0).resource()).isEqualTo(path);
  }

  @Test
  void devRoleAloneGrantsNothing() throws Exception {
    String dev = TestTokens.user("dev.user", null, Role.SECUREHR_DEV).bearer();
    mvc.perform(get(ANY_PORTAL_ENDPOINT).header(HttpHeaders.AUTHORIZATION, dev))
        .andExpect(status().isForbidden());
  }

  @Test
  void unknownPath_isDeniedByDefault() throws Exception {
    mvc.perform(
            get("/api/internal/anything").header(HttpHeaders.AUTHORIZATION, employee().bearer()))
        .andExpect(status().isForbidden());
  }

  @Test
  void unsafeRequestId_isReplaced() throws Exception {
    mvc.perform(get(ANY_PORTAL_ENDPOINT).header("X-Request-ID", "bad\nid"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("X-Request-ID", org.hamcrest.Matchers.not("bad\nid")));
  }

  private TestTokens employee() {
    return TestTokens.user("demo.employee", 10001, Role.EMPLOYEE);
  }

  private void expectUnauthorized(String authorization) throws Exception {
    mvc.perform(get(ANY_PORTAL_ENDPOINT).header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }
}
