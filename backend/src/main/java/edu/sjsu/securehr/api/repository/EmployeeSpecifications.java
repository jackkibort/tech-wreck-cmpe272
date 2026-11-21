package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.Dates;
import edu.sjsu.securehr.api.domain.DeptEmp;
import edu.sjsu.securehr.api.domain.Employee;
import edu.sjsu.securehr.api.domain.Title;
import edu.sjsu.securehr.api.dto.EmployeeSearchCriteria;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the directory search query from optional filters (Specification pattern, Section 12.4).
 * Every filter is a parameterized predicate, never string-concatenated SQL.
 */
public final class EmployeeSpecifications {

  // Not backslash: MySQL also treats backslash as a string-literal escape, which makes
  // an ESCAPE clause using it dialect-sensitive.
  private static final char LIKE_ESCAPE = '!';

  private EmployeeSpecifications() {}

  /**
   * Combines every supplied filter; absent filters are ignored. Results are always limited to
   * current employees (those with a current department).
   *
   * @param criteria search filters
   * @return the combined specification
   */
  public static Specification<Employee> matching(EmployeeSearchCriteria criteria) {
    return Specification.allOf(
        currentlyInDepartment(criteria.deptNo()),
        nameStartsWith(criteria.q()),
        hasCurrentTitle(criteria.title()),
        hiredOnOrAfter(criteria),
        hiredOnOrBefore(criteria));
  }

  /**
   * Current employees, optionally restricted to one department. Uses the (dept_no, to_date, emp_no)
   * index.
   */
  static Specification<Employee> currentlyInDepartment(String deptNo) {
    return (root, query, cb) -> {
      Subquery<Integer> membership = query.subquery(Integer.class);
      var deptEmp = membership.from(DeptEmp.class);
      membership.select(deptEmp.get("id").get("empNo"));
      var current = cb.equal(deptEmp.get("toDate"), Dates.CURRENT);
      var sameEmployee = cb.equal(deptEmp.get("id").get("empNo"), root.get("empNo"));
      membership.where(
          deptNo == null
              ? cb.and(sameEmployee, current)
              : cb.and(sameEmployee, current, cb.equal(deptEmp.get("id").get("deptNo"), deptNo)));
      return cb.exists(membership);
    };
  }

  /**
   * Prefix match on first or last name. Prefix (not substring) matching lets MySQL use the name
   * indexes; the column collation is case-insensitive.
   */
  static Specification<Employee> nameStartsWith(String q) {
    if (q == null || q.isBlank()) {
      return null;
    }
    String pattern = escapeLike(q.strip()) + "%";
    return (root, query, cb) ->
        cb.or(
            cb.like(root.get("lastName"), pattern, LIKE_ESCAPE),
            cb.like(root.get("firstName"), pattern, LIKE_ESCAPE));
  }

  static Specification<Employee> hasCurrentTitle(String title) {
    if (title == null || title.isBlank()) {
      return null;
    }
    return (root, query, cb) -> {
      Subquery<Integer> titles = query.subquery(Integer.class);
      var t = titles.from(Title.class);
      titles
          .select(t.get("id").get("empNo"))
          .where(
              cb.equal(t.get("id").get("empNo"), root.get("empNo")),
              cb.equal(t.get("id").get("title"), title.strip()),
              cb.equal(t.get("toDate"), Dates.CURRENT));
      return cb.exists(titles);
    };
  }

  static Specification<Employee> hiredOnOrAfter(EmployeeSearchCriteria criteria) {
    return criteria.hiredFrom() == null
        ? null
        : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("hireDate"), criteria.hiredFrom());
  }

  static Specification<Employee> hiredOnOrBefore(EmployeeSearchCriteria criteria) {
    return criteria.hiredTo() == null
        ? null
        : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("hireDate"), criteria.hiredTo());
  }

  /** Treats user input as literal text: {@code %}, {@code _} and {@code !} are not wildcards. */
  static String escapeLike(String text) {
    StringBuilder escaped = new StringBuilder(text.length());
    for (char c : text.toCharArray()) {
      if (c == '%' || c == '_' || c == LIKE_ESCAPE) {
        escaped.append(LIKE_ESCAPE);
      }
      escaped.append(c);
    }
    return escaped.toString();
  }
}
