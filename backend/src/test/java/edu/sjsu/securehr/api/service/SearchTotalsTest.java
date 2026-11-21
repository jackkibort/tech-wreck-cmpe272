package edu.sjsu.securehr.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.sjsu.securehr.api.domain.Dates;
import edu.sjsu.securehr.api.dto.EmployeeSearchCriteria;
import edu.sjsu.securehr.api.repository.DeptEmpRepository;
import edu.sjsu.securehr.api.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class SearchTotalsTest {

  private final EmployeeRepository employees = mock(EmployeeRepository.class);
  private final DeptEmpRepository deptEmps = mock(DeptEmpRepository.class);
  private final SearchTotals totals = new SearchTotals(employees, deptEmps);

  @Test
  void defaultViewIsCountedFromDepartmentMembership() {
    when(deptEmps.countCurrentEmployees(Dates.CURRENT)).thenReturn(240_124L);

    assertThat(totals.count(criteria(null, null))).isEqualTo(240_124L);
    verify(employees, never()).count(any(Specification.class));
  }

  @Test
  void departmentFilterIsCountedFromDepartmentMembership() {
    when(deptEmps.countCurrentEmployees("d005", Dates.CURRENT)).thenReturn(61_386L);

    assertThat(totals.count(criteria(null, "d005"))).isEqualTo(61_386L);
    verify(employees, never()).count(any(Specification.class));
  }

  @Test
  void otherFiltersUseACountQuery() {
    when(employees.count(any(Specification.class))).thenReturn(141L);

    assertThat(totals.count(criteria("Fac", null))).isEqualTo(141L);
  }

  @Test
  void repeatedSearchesAreCountedOnce_ignoringCaseAndSpaces() {
    when(employees.count(any(Specification.class))).thenReturn(141L);

    totals.count(criteria("Fac", null));
    totals.count(criteria("fac ", null));
    totals.count(criteria("FAC", null));

    verify(employees, times(1)).count(any(Specification.class));
  }

  @Test
  void cacheIsBounded_leastRecentlyUsedIsRecounted() {
    when(employees.count(any(Specification.class))).thenReturn(1L);
    EmployeeSearchCriteria first = criteria("a0", null);
    totals.count(first);
    for (int i = 1; i <= SearchTotals.MAX_ENTRIES; i++) {
      totals.count(criteria("a" + i, null));
    }

    totals.count(first);

    verify(employees, times(SearchTotals.MAX_ENTRIES + 2)).count(any(Specification.class));
    verify(deptEmps, never()).countCurrentEmployees(eq(Dates.CURRENT));
  }

  private static EmployeeSearchCriteria criteria(String q, String deptNo) {
    return new EmployeeSearchCriteria(q, deptNo, null, null, null);
  }
}
