package edu.sjsu.securehr.api.controller;

import static edu.sjsu.securehr.api.support.TestUsers.EMPLOYEE_NO;
import static edu.sjsu.securehr.api.support.TestUsers.FORMER_EMPLOYEE_NO;
import static edu.sjsu.securehr.api.support.TestUsers.MANAGER_NO;
import static edu.sjsu.securehr.api.support.TestUsers.MISSING_NO;
import static edu.sjsu.securehr.api.support.TestUsers.OTHER_DEPT_NO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.sjsu.securehr.api.support.ApiIntegrationTest;
import edu.sjsu.securehr.api.support.CapturedAuditEvents;
import edu.sjsu.securehr.api.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Department list and management chain: directory data every portal role may see. */
@ApiIntegrationTest
class OrganizationIT {

  @Autowired private MockMvc mvc;
  @Autowired private CapturedAuditEvents audit;

  @BeforeEach
  void clearAudit() {
    audit.clear();
  }

  @Test
  void departmentsListCurrentManagersOnly() throws Exception {
    call(TestUsers.employee(), "/api/departments")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[0].deptNo").value("d001"))
        .andExpect(jsonPath("$[0].deptName").value("Marketing"))
        // 10007 managed d001 until 1995; 10008 is the current manager.
        .andExpect(jsonPath("$[0].manager.empNo").value(10008))
        .andExpect(jsonPath("$[0].manager.lastName").value("Kalloufi"))
        .andExpect(jsonPath("$[1].manager").isEmpty())
        .andExpect(jsonPath("$[2].manager.empNo").value(MANAGER_NO))
        // Directory fields only.
        .andExpect(jsonPath("$[2].manager.birthDate").doesNotExist());
    assertThat(audit.all()).isEmpty();
  }

  @Test
  void chainRunsFromEmployeeToDepartmentManager() throws Exception {
    call(TestUsers.employee(), "/api/org/" + EMPLOYEE_NO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deptNo").value("d005"))
        .andExpect(jsonPath("$.deptName").value("Development"))
        .andExpect(jsonPath("$.chain.length()").value(2))
        .andExpect(jsonPath("$.chain[0].empNo").value(EMPLOYEE_NO))
        .andExpect(jsonPath("$.chain[0].title").value("Senior Engineer"))
        .andExpect(jsonPath("$.chain[1].empNo").value(MANAGER_NO))
        .andExpect(jsonPath("$.chain[1].title").value("Manager"))
        .andExpect(jsonPath("$.chain[0].birthDate").doesNotExist());
  }

  @Test
  void managerChainIsJustThemself() throws Exception {
    call(TestUsers.employee(), "/api/org/" + MANAGER_NO)
        .andExpect(jsonPath("$.chain.length()").value(1))
        .andExpect(jsonPath("$.chain[0].empNo").value(MANAGER_NO));
  }

  @Test
  void departmentWithoutCurrentManager_endsAtEmployee() throws Exception {
    call(TestUsers.employee(), "/api/org/" + OTHER_DEPT_NO)
        .andExpect(jsonPath("$.deptNo").value("d007"))
        .andExpect(jsonPath("$.chain.length()").value(1));
  }

  @Test
  void formerEmployeeHasNoDepartment() throws Exception {
    call(TestUsers.manager(), "/api/org/" + FORMER_EMPLOYEE_NO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deptNo").isEmpty())
        .andExpect(jsonPath("$.chain.length()").value(1))
        // Their title ended when they left, so there is no current title.
        .andExpect(jsonPath("$.chain[0].title").isEmpty());
  }

  @Test
  void unknownEmployee_isNotFound() throws Exception {
    // Every portal role may see directory data, so existence is not a secret here.
    call(TestUsers.employee(), "/api/org/" + MISSING_NO)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  private ResultActions call(String bearer, String path) throws Exception {
    return mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer));
  }
}
