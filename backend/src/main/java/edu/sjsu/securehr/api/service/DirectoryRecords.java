package edu.sjsu.securehr.api.service;

import edu.sjsu.securehr.api.domain.Dates;
import edu.sjsu.securehr.api.domain.DeptEmp;
import edu.sjsu.securehr.api.domain.Employee;
import edu.sjsu.securehr.api.domain.Title;
import edu.sjsu.securehr.api.dto.EmployeeSummaryDto;
import edu.sjsu.securehr.api.repository.DeptEmpRepository;
import edu.sjsu.securehr.api.repository.TitleRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Resolves the current department and title that accompany directory fields. Lookups for a page are
 * batched: two queries per page, not two per employee.
 */
@Component
public class DirectoryRecords {

  private final DeptEmpRepository deptEmps;
  private final TitleRepository titles;

  /**
   * Creates the helper.
   *
   * @param deptEmps department membership history
   * @param titles title history
   */
  public DirectoryRecords(DeptEmpRepository deptEmps, TitleRepository titles) {
    this.deptEmps = deptEmps;
    this.titles = titles;
  }

  /**
   * Builds directory summaries, preserving the input order.
   *
   * @param employees employees to summarize
   * @return one summary per employee
   */
  public List<EmployeeSummaryDto> summarize(List<Employee> employees) {
    if (employees.isEmpty()) {
      return List.of();
    }
    List<Integer> empNos = employees.stream().map(Employee::getEmpNo).toList();
    Map<Integer, DeptEmp> departments =
        deptEmps.findCurrentFor(empNos, Dates.CURRENT).stream()
            .collect(Collectors.toMap(de -> de.getId().empNo(), Function.identity()));
    Map<Integer, String> currentTitles =
        titles.findCurrentFor(empNos, Dates.CURRENT).stream()
            .collect(Collectors.toMap(t -> t.getId().empNo(), t -> t.getId().title(), (a, b) -> a));
    return employees.stream()
        .map(e -> summary(e, departments.get(e.getEmpNo()), currentTitles.get(e.getEmpNo())))
        .toList();
  }

  /**
   * Builds one directory summary.
   *
   * @param employee the employee
   * @return the summary
   */
  public EmployeeSummaryDto summarize(Employee employee) {
    return summarize(List.of(employee)).get(0);
  }

  /**
   * The employee's current department membership.
   *
   * @param empNo employee number
   * @return current membership, empty for former employees
   */
  public Optional<DeptEmp> currentMembership(int empNo) {
    return deptEmps.findCurrentFor(empNo, Dates.CURRENT);
  }

  /**
   * The employee's current title.
   *
   * @param empNo employee number
   * @return current title, if any
   */
  public Optional<String> currentTitle(int empNo) {
    return titles.findCurrentFor(List.of(empNo), Dates.CURRENT).stream()
        .map(Title::getId)
        .map(Title.Key::title)
        .findFirst();
  }

  private static EmployeeSummaryDto summary(Employee e, DeptEmp membership, String title) {
    return new EmployeeSummaryDto(
        e.getEmpNo(),
        e.getFirstName(),
        e.getLastName(),
        membership == null ? null : membership.getDepartment().getDeptNo(),
        membership == null ? null : membership.getDepartment().getDeptName(),
        title);
  }
}
