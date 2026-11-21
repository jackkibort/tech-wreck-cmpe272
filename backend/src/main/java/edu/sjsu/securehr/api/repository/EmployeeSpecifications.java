package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.Dates;
import edu.sjsu.securehr.api.domain.DeptEmp;
import edu.sjsu.securehr.api.domain.Employee;
import edu.sjsu.securehr.api.domain.Title;
import edu.sjsu.securehr.api.dto.EmployeeSearchCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
   * Filters for fetching a sorted page (search and export). Combines every supplied filter; absent
   * filters are ignored. Results are always limited to current employees.
   *
   * @param criteria search filters
   * @return the combined specification
   */
  public static Specification<Employee> forPage(EmployeeSearchCriteria criteria) {
    return withFilters(currentInDepartmentPerRow(criteria.deptNo()), criteria);
  }

  /**
   * The same filters, shaped for counting. Counting needs no sort order, so MySQL's semijoin
   * rewrite of {@code exists}, which hurts a sorted page, is the faster plan here.
   *
   * @param criteria search filters
   * @return the combined specification
   */
  public static Specification<Employee> forCount(EmployeeSearchCriteria criteria) {
    return withFilters(currentInDepartment(criteria.deptNo()), criteria);
  }

  private static Specification<Employee> withFilters(
      Specification<Employee> current, EmployeeSearchCriteria criteria) {
    return Specification.allOf(
        current,
        nameStartsWith(criteria.q()),
        hasCurrentTitle(criteria.title()),
        hiredOnOrAfter(criteria),
        hiredOnOrBefore(criteria));
  }

  /**
   * Current employees, optionally restricted to one department, as an {@code exists} subquery. Uses
   * the (dept_no, to_date, emp_no) index.
   */
  static Specification<Employee> currentInDepartment(String deptNo) {
    return (root, query, cb) -> {
      Subquery<Integer> membership = query.subquery(Integer.class);
      var deptEmp = membership.from(DeptEmp.class);
      membership
          .select(deptEmp.get("id").get("empNo"))
          .where(currentMembership(cb, deptEmp, root, deptNo));
      return cb.exists(membership);
    };
  }

  /**
   * The same condition written as {@code (select count(*) ...) > 0}, on purpose. MySQL turns an
   * {@code exists} into a semijoin, which for broad filters joins and sorts every current employee
   * (2.7 s for the unfiltered first page on the full dataset). A scalar subquery is checked per row
   * instead, so MySQL walks the name index in sort order and stops after one page (under 1 ms),
   * while selective filters such as a title still drive the plan. Measurements are in the backend
   * README.
   */
  static Specification<Employee> currentInDepartmentPerRow(String deptNo) {
    return (root, query, cb) -> {
      Subquery<Long> memberships = query.subquery(Long.class);
      var deptEmp = memberships.from(DeptEmp.class);
      memberships
          .select(cb.count(deptEmp.get("id").get("empNo")))
          .where(currentMembership(cb, deptEmp, root, deptNo));
      return cb.greaterThan(memberships, 0L);
    };
  }

  private static Predicate currentMembership(
      CriteriaBuilder cb, Root<DeptEmp> deptEmp, Root<Employee> employee, String deptNo) {
    Predicate sameEmployee = cb.equal(deptEmp.get("id").get("empNo"), employee.get("empNo"));
    Predicate current = cb.equal(deptEmp.get("toDate"), Dates.CURRENT);
    return deptNo == null
        ? cb.and(sameEmployee, current)
        : cb.and(sameEmployee, current, cb.equal(deptEmp.get("id").get("deptNo"), deptNo));
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
