package edu.sjsu.securehr.api.service;

import edu.sjsu.securehr.api.domain.Dates;
import edu.sjsu.securehr.api.dto.EmployeeSearchCriteria;
import edu.sjsu.securehr.api.repository.DeptEmpRepository;
import edu.sjsu.securehr.api.repository.EmployeeRepository;
import edu.sjsu.securehr.api.repository.EmployeeSpecifications;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Total matches for a directory search (the {@code totalElements} of a page), computed in the
 * cheapest way for the filters in use and remembered.
 *
 * <p>The default view and the department filter are counted from department membership alone, which
 * an index answers without touching the employees table. Other filters use a count query.
 *
 * <p>Totals are cached because the API never changes the dataset (Section 7.3): paging through
 * results, or repeating a search, would otherwise recount every time. The cache holds at most
 * {@value #MAX_ENTRIES} searches and evicts the least recently used; a restart picks up a
 * re-imported dataset.
 */
@Component
public class SearchTotals {

  static final int MAX_ENTRIES = 1_000;

  private final EmployeeRepository employees;
  private final DeptEmpRepository deptEmps;
  private final Map<EmployeeSearchCriteria, Long> cache =
      new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<EmployeeSearchCriteria, Long> eldest) {
          return size() > MAX_ENTRIES;
        }
      };

  /**
   * Creates the counter.
   *
   * @param employees employee repository
   * @param deptEmps department membership history
   */
  public SearchTotals(EmployeeRepository employees, DeptEmpRepository deptEmps) {
    this.employees = employees;
    this.deptEmps = deptEmps;
  }

  /**
   * Number of current employees matching the filters.
   *
   * @param criteria validated search filters
   * @return the total
   */
  public long count(EmployeeSearchCriteria criteria) {
    EmployeeSearchCriteria key = normalize(criteria);
    synchronized (cache) {
      Long cached = cache.get(key);
      if (cached != null) {
        return cached;
      }
    }
    // Counted outside the lock so one slow count never blocks other searches.
    long total = compute(key);
    synchronized (cache) {
      cache.put(key, total);
    }
    return total;
  }

  private long compute(EmployeeSearchCriteria criteria) {
    if (!criteria.isDepartmentOnly()) {
      return employees.count(EmployeeSpecifications.forCount(criteria));
    }
    return criteria.deptNo() == null
        ? deptEmps.countCurrentEmployees(Dates.CURRENT)
        : deptEmps.countCurrentEmployees(criteria.deptNo(), Dates.CURRENT);
  }

  // Name matching ignores case and surrounding spaces, so "Fac", "fac " and "FAC" share a total.
  private static EmployeeSearchCriteria normalize(EmployeeSearchCriteria c) {
    return new EmployeeSearchCriteria(
        blankToNull(c.q() == null ? null : c.q().strip().toLowerCase(Locale.ROOT)),
        c.deptNo(),
        blankToNull(c.title() == null ? null : c.title().strip()),
        c.hiredFrom(),
        c.hiredTo());
  }

  private static String blankToNull(String s) {
    return s == null || s.isEmpty() ? null : s;
  }
}
