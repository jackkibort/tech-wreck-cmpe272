package edu.sjsu.securehr.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Directory search filters shared by search and CSV export (Section 0.6). All are optional.
 *
 * @param q name prefix matched against first or last name
 * @param deptNo current department number
 * @param title exact current title
 * @param hiredFrom inclusive lower bound on hire date
 * @param hiredTo inclusive upper bound on hire date
 */
public record EmployeeSearchCriteria(
    @Size(min = 1, max = 16) String q,
    @Pattern(regexp = "d\\d{3}") String deptNo,
    @Size(min = 1, max = 50) String title,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hiredFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hiredTo) {

  /**
   * Whether the only filter, if any, is the department: the directory's default view and its
   * department filter, whose totals come from department membership alone.
   *
   * @return {@code true} if no filter other than {@code deptNo} is set
   */
  public boolean isDepartmentOnly() {
    return (q == null || q.isBlank())
        && (title == null || title.isBlank())
        && hiredFrom == null
        && hiredTo == null;
  }

  /** Rejects an inverted hire-date range. */
  @AssertTrue
  public boolean isHireRangeOrdered() {
    return hiredFrom == null || hiredTo == null || !hiredFrom.isAfter(hiredTo);
  }
}
