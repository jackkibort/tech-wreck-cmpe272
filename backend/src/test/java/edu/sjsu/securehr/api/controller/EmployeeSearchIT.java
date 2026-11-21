package edu.sjsu.securehr.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.sjsu.securehr.api.support.ApiIntegrationTest;
import edu.sjsu.securehr.api.support.TestUsers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Directory search against the real schema and migrations (TC-API-09, TC-API-10). */
@ApiIntegrationTest
class EmployeeSearchIT {

  @Autowired private MockMvc mvc;

  @Test
  void tcApi10_noFiltersReturnsCurrentEmployeesInNameOrder() throws Exception {
    // 8 current employees; 10006 has left the company.
    search()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(8))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(25))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.content[0].lastName").value("Bamford"))
        .andExpect(jsonPath("$.content[*].empNo", Matchers.not(Matchers.hasItem(10006))));
  }

  @Test
  void tcApi10_searchByNamePrefix_matchesFirstOrLastNameCaseInsensitively() throws Exception {
    search("q", "fac")
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].empNo").value(10001));
    search("q", "Sa")
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].firstName").value("Saniya"));
  }

  @Test
  void tcApi10_likeWildcardsInNameAreLiteral() throws Exception {
    search("q", "%").andExpect(jsonPath("$.totalElements").value(0));
    search("q", "_").andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void tcApi10_searchByDepartment_usesCurrentMembershipOnly() throws Exception {
    // 10005 moved out of d005 and 10006 left, so neither is listed.
    search("deptNo", "d005")
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.content[*].empNo", Matchers.containsInAnyOrder(10001, 10002, 10009)))
        .andExpect(jsonPath("$.content[0].deptName").value("Development"));
  }

  @Test
  void tcApi10_searchByCurrentTitle() throws Exception {
    // 10002 was a Senior Engineer before 1991 and must not match.
    search("title", "Senior Engineer")
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].empNo").value(10001))
        .andExpect(jsonPath("$.content[0].title").value("Senior Engineer"));
  }

  @Test
  void tcApi10_searchByHireDateRange_isInclusive() throws Exception {
    search("hiredFrom", "1986-06-26", "hiredTo", "1986-12-01")
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(
            jsonPath("$.content[*].empNo", Matchers.containsInAnyOrder(10001, 10003, 10004)));
  }

  @Test
  void tcApi10_combinedFilters() throws Exception {
    search("deptNo", "d005", "title", "Engineer", "hiredTo", "1990-01-01")
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].empNo").value(10009));
  }

  @Test
  void tcApi09_pageSizeAbove100_isCappedAt100() throws Exception {
    search("size", "500").andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100));
  }

  @Test
  void pagingIsStable() throws Exception {
    search("size", "3", "page", "2")
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  void searchResultsContainDirectoryFieldsOnly() throws Exception {
    search("q", "Facello")
        .andExpect(jsonPath("$.content[0].birthDate").doesNotExist())
        .andExpect(jsonPath("$.content[0].currentSalary").doesNotExist())
        .andExpect(jsonPath("$.content[0].hireDate").doesNotExist());
  }

  @Test
  void invalidParameters_areBadRequest() throws Exception {
    search("size", "0").andExpect(status().isBadRequest());
    search("page", "-1").andExpect(status().isBadRequest());
    search("deptNo", "x' OR 1=1").andExpect(status().isBadRequest());
    search("hiredFrom", "not-a-date").andExpect(status().isBadRequest());
    search("hiredFrom", "2000-01-01", "hiredTo", "1990-01-01")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  /** Searches as an employee; arguments are name/value pairs, sent unencoded as parameters. */
  private ResultActions search(String... nameValuePairs) throws Exception {
    MockHttpServletRequestBuilder request =
        get("/api/employees").header(HttpHeaders.AUTHORIZATION, TestUsers.employee());
    for (int i = 0; i < nameValuePairs.length; i += 2) {
      request.param(nameValuePairs[i], nameValuePairs[i + 1]);
    }
    return mvc.perform(request);
  }
}
