package edu.sjsu.securehr.api.controller;

import static edu.sjsu.securehr.api.support.TestUsers.EMPLOYEE_NO;
import static edu.sjsu.securehr.api.support.TestUsers.FORMER_EMPLOYEE_NO;
import static edu.sjsu.securehr.api.support.TestUsers.HR_ADMIN_NO;
import static edu.sjsu.securehr.api.support.TestUsers.MANAGER_NO;
import static edu.sjsu.securehr.api.support.TestUsers.MISSING_NO;
import static edu.sjsu.securehr.api.support.TestUsers.OTHER_DEPT_NO;
import static edu.sjsu.securehr.api.support.TestUsers.TEAMMATE_NO;
import static edu.sjsu.securehr.api.support.TestUsers.TRANSFERRED_NO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.sjsu.securehr.api.audit.AuditEvent;
import edu.sjsu.securehr.api.audit.AuditEventType;
import edu.sjsu.securehr.api.audit.AuditOutcome;
import edu.sjsu.securehr.api.support.ApiIntegrationTest;
import edu.sjsu.securehr.api.support.CapturedAuditEvents;
import edu.sjsu.securehr.api.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The Section 8.2 matrix for employee records, titles and salaries, including IDOR attempts and
 * field masking (TC-API-03 to TC-API-06).
 */
@ApiIntegrationTest
class EmployeeAccessMatrixIT {

  @Autowired private MockMvc mvc;
  @Autowired private CapturedAuditEvents audit;

  @BeforeEach
  void clearAudit() {
    audit.clear();
  }

  // ---- Salary history -------------------------------------------------------------------

  @Test
  void tcApi03_employeeReadsOwnSalary_isAllowedAndAudited() throws Exception {
    call(TestUsers.employee(), "/api/employees/" + EMPLOYEE_NO + "/salaries")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].salary").value(60117))
        .andExpect(jsonPath("$[2].toDate").isEmpty());

    AuditEvent event = single(AuditEventType.SALARY_READ);
    assertThat(event.outcome()).isEqualTo(AuditOutcome.ALLOWED);
    assertThat(event.actorEmpNo()).isEqualTo(EMPLOYEE_NO);
    assertThat(event.targetEmpNo()).isEqualTo(EMPLOYEE_NO);
  }

  @Test
  void tcApi04_employeeReadsAnotherSalary_isForbiddenAndAudited() throws Exception {
    call(TestUsers.employee(), "/api/employees/" + TEAMMATE_NO + "/salaries")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.requestId").isNotEmpty());

    AuditEvent event = single(AuditEventType.ACCESS_DENIED);
    assertThat(event.outcome()).isEqualTo(AuditOutcome.DENIED);
    assertThat(event.actorUsername()).isEqualTo("demo.employee");
    assertThat(event.targetEmpNo()).isEqualTo(TEAMMATE_NO);
    assertThat(event.resource()).isEqualTo("/api/employees/" + TEAMMATE_NO + "/salaries");
    assertThat(audit.ofType(AuditEventType.SALARY_READ)).isEmpty();
  }

  @Test
  void managerReadsTeamMemberSalary_isForbidden() throws Exception {
    call(TestUsers.manager(), "/api/employees/" + EMPLOYEE_NO + "/salaries")
        .andExpect(status().isForbidden());
  }

  @Test
  void hrAdminReadsAnySalary_isAllowedAndAudited() throws Exception {
    call(TestUsers.hrAdmin(), "/api/employees/" + OTHER_DEPT_NO + "/salaries")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].salary").value(40006));
    assertThat(single(AuditEventType.SALARY_READ).targetEmpNo()).isEqualTo(OTHER_DEPT_NO);
  }

  @Test
  void hrAdminReadsMissingEmployeeSalary_isNotFound() throws Exception {
    call(TestUsers.hrAdmin(), "/api/employees/" + MISSING_NO + "/salaries")
        .andExpect(status().isNotFound());
  }

  @Test
  void employeeReadsMissingEmployeeSalary_isForbiddenNotNotFound() throws Exception {
    // Existence must not leak to a caller who may not see the record (Section 8.2).
    call(TestUsers.employee(), "/api/employees/" + MISSING_NO + "/salaries")
        .andExpect(status().isForbidden());
  }

  // ---- Employee detail ------------------------------------------------------------------

  @Test
  void employeeReadsOwnDetail_includesRestrictedFields() throws Exception {
    call(TestUsers.employee(), "/api/employees/" + EMPLOYEE_NO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.empNo").value(EMPLOYEE_NO))
        .andExpect(jsonPath("$.deptNo").value("d005"))
        .andExpect(jsonPath("$.title").value("Senior Engineer"))
        .andExpect(jsonPath("$.birthDate").value("1953-09-02"))
        .andExpect(jsonPath("$.currentSalary").value(88958));
    // Reading your own record is not a sensitive access.
    assertThat(audit.all()).isEmpty();
  }

  @Test
  void tcApi05_managerReadsInDepartmentEmployee_withoutSalaryOrBirthDate() throws Exception {
    call(TestUsers.manager(), "/api/employees/" + EMPLOYEE_NO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.empNo").value(EMPLOYEE_NO))
        .andExpect(jsonPath("$.lastName").value("Facello"))
        .andExpect(jsonPath("$.birthDate").doesNotExist())
        .andExpect(jsonPath("$.currentSalary").doesNotExist());

    AuditEvent event = single(AuditEventType.DATA_READ);
    assertThat(event.actorEmpNo()).isEqualTo(MANAGER_NO);
    assertThat(event.targetEmpNo()).isEqualTo(EMPLOYEE_NO);
  }

  @Test
  void tcApi05_managerReadsInDepartmentTitles_isAllowed() throws Exception {
    call(TestUsers.manager(), "/api/employees/" + EMPLOYEE_NO + "/titles")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].title").value("Engineer"))
        .andExpect(jsonPath("$[0].toDate").value("1995-06-26"))
        .andExpect(jsonPath("$[1].toDate").isEmpty());
  }

  @ParameterizedTest
  @ValueSource(ints = {OTHER_DEPT_NO, TRANSFERRED_NO, FORMER_EMPLOYEE_NO, MISSING_NO})
  void tcApi06_managerReadsOutOfScopeEmployee_isForbiddenAndAudited(int empNo) throws Exception {
    call(TestUsers.manager(), "/api/employees/" + empNo).andExpect(status().isForbidden());
    call(TestUsers.manager(), "/api/employees/" + empNo + "/titles")
        .andExpect(status().isForbidden());

    assertThat(audit.ofType(AuditEventType.ACCESS_DENIED))
        .hasSize(2)
        .allSatisfy(e -> assertThat(e.targetEmpNo()).isEqualTo(empNo));
  }

  @Test
  void formerManager_hasNoManagerScope() throws Exception {
    // 10007 managed d001 until 1995; 10008 is a current d001 member.
    call(TestUsers.formerManager(), "/api/employees/10008").andExpect(status().isForbidden());
  }

  @Test
  void employeeReadsAnotherEmployeeDetail_isForbidden() throws Exception {
    call(TestUsers.employee(), "/api/employees/" + TEAMMATE_NO).andExpect(status().isForbidden());
    call(TestUsers.employee(), "/api/employees/" + TEAMMATE_NO + "/titles")
        .andExpect(status().isForbidden());
  }

  @Test
  void hrAdminReadsAnyDetail_includesRestrictedFields() throws Exception {
    call(TestUsers.hrAdmin(), "/api/employees/" + FORMER_EMPLOYEE_NO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deptNo").isEmpty())
        .andExpect(jsonPath("$.birthDate").value("1953-04-20"))
        // Former employees have no current salary row.
        .andExpect(jsonPath("$.currentSalary").doesNotExist());
  }

  @Test
  void hrAdminReadsMissingEmployee_isNotFound() throws Exception {
    call(TestUsers.hrAdmin(), "/api/employees/" + MISSING_NO)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
    call(TestUsers.hrAdmin(), "/api/employees/" + MISSING_NO + "/titles")
        .andExpect(status().isNotFound());
  }

  @Test
  void nonNumericEmpNo_isBadRequest() throws Exception {
    call(TestUsers.hrAdmin(), "/api/employees/abc").andExpect(status().isBadRequest());
  }

  // ---- /api/me --------------------------------------------------------------------------

  @Test
  void meReturnsCallerFromTokenOnly() throws Exception {
    call(TestUsers.hrAdmin(), "/api/me?emp_no=10001")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("demo.hradmin"))
        .andExpect(jsonPath("$.empNo").value(HR_ADMIN_NO))
        .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"))
        .andExpect(jsonPath("$.roles[1]").value("HR_ADMIN"))
        .andExpect(jsonPath("$.employee.deptName").value("Human Resources"))
        .andExpect(jsonPath("$.employee.birthDate").doesNotExist());
  }

  private ResultActions call(String bearer, String path) throws Exception {
    return mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer));
  }

  private AuditEvent single(AuditEventType type) {
    assertThat(audit.ofType(type)).hasSize(1);
    return audit.ofType(type).get(0);
  }
}
