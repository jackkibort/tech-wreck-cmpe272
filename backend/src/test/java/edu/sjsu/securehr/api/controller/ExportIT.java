package edu.sjsu.securehr.api.controller;

import static edu.sjsu.securehr.api.support.TestUsers.HR_ADMIN_NO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.sjsu.securehr.api.audit.AuditEvent;
import edu.sjsu.securehr.api.audit.AuditEventType;
import edu.sjsu.securehr.api.audit.AuditOutcome;
import edu.sjsu.securehr.api.support.ApiIntegrationTest;
import edu.sjsu.securehr.api.support.CapturedAuditEvents;
import edu.sjsu.securehr.api.support.TestUsers;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** CSV export (TC-API-07). The 10,000-row cap itself is covered by ExportServiceTest. */
@ApiIntegrationTest
class ExportIT {

  private static final String PATH = "/api/export/employees";

  @Autowired private MockMvc mvc;
  @Autowired private CapturedAuditEvents audit;

  @BeforeEach
  void clearAudit() {
    audit.clear();
  }

  @Test
  void tcApi07_hrAdminExport_returnsCsvAndIsAudited() throws Exception {
    String csv =
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, TestUsers.hrAdmin()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"employees-export.csv\""))
            .andExpect(header().string("X-Export-Truncated", "false"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    List<String> lines = csv.lines().toList();
    assertThat(lines)
        .hasSize(9)
        .startsWith(
            "emp_no,first_name,last_name,gender,hire_date,dept_no,dept_name,title",
            "10003,Parto,Bamford,M,1986-08-28,d007,Sales,Senior Staff",
            "10001,Georgi,Facello,M,1986-06-26,d005,Development,Senior Engineer");
    // Current employees only.
    assertThat(csv).doesNotContain("Preusig");

    List<AuditEvent> exports = audit.ofType(AuditEventType.EXPORT);
    assertThat(exports).hasSize(1);
    AuditEvent event = exports.get(0);
    assertThat(event.outcome()).isEqualTo(AuditOutcome.ALLOWED);
    assertThat(event.actorUsername()).isEqualTo("demo.hradmin");
    assertThat(event.actorEmpNo()).isEqualTo(HR_ADMIN_NO);
    assertThat(event.resource()).isEqualTo(PATH);
    assertThat(event.requestId()).isNotBlank();
  }

  @Test
  void tcApi07_exportAppliesSearchFilters() throws Exception {
    String csv =
        mvc.perform(
                get(PATH)
                    .param("deptNo", "d005")
                    .param("title", "Engineer")
                    .header(HttpHeaders.AUTHORIZATION, TestUsers.hrAdmin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(csv.lines())
        .containsExactly(
            "emp_no,first_name,last_name,gender,hire_date,dept_no,dept_name,title",
            "10009,Sumant,Peac,F,1985-02-18,d005,Development,Engineer");
  }

  @Test
  void tcApi07_nonHrRoles_areForbiddenAndAudited() throws Exception {
    for (String caller : List.of(TestUsers.employee(), TestUsers.manager())) {
      mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, caller))
          .andExpect(status().isForbidden());
    }
    assertThat(audit.ofType(AuditEventType.ACCESS_DENIED)).hasSize(2);
    assertThat(audit.ofType(AuditEventType.EXPORT)).isEmpty();
  }

  @Test
  void invalidFilters_areBadRequest() throws Exception {
    mvc.perform(
            get(PATH)
                .param("hiredFrom", "2000-01-01")
                .param("hiredTo", "1990-01-01")
                .header(HttpHeaders.AUTHORIZATION, TestUsers.hrAdmin()))
        .andExpect(status().isBadRequest());
    assertThat(audit.ofType(AuditEventType.EXPORT)).isEmpty();
  }
}
