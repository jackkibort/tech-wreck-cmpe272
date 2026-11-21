package edu.sjsu.securehr.api.service;

import edu.sjsu.securehr.api.audit.AuditEventType;
import edu.sjsu.securehr.api.audit.AuditOutcome;
import edu.sjsu.securehr.api.audit.AuditTrail;
import edu.sjsu.securehr.api.domain.Dates;
import edu.sjsu.securehr.api.domain.DeptEmp;
import edu.sjsu.securehr.api.domain.Employee;
import edu.sjsu.securehr.api.domain.Salary;
import edu.sjsu.securehr.api.dto.EmployeeDetailDto;
import edu.sjsu.securehr.api.dto.EmployeeSearchCriteria;
import edu.sjsu.securehr.api.dto.EmployeeSummaryDto;
import edu.sjsu.securehr.api.dto.MeDto;
import edu.sjsu.securehr.api.dto.PageResponse;
import edu.sjsu.securehr.api.dto.SalaryRecordDto;
import edu.sjsu.securehr.api.dto.TitleRecordDto;
import edu.sjsu.securehr.api.exception.ResourceNotFoundException;
import edu.sjsu.securehr.api.repository.EmployeeRepository;
import edu.sjsu.securehr.api.repository.EmployeeSpecifications;
import edu.sjsu.securehr.api.repository.SalaryRepository;
import edu.sjsu.securehr.api.repository.TitleRepository;
import edu.sjsu.securehr.api.security.CurrentUser;
import edu.sjsu.securehr.api.security.Role;
import edu.sjsu.securehr.api.security.policy.EmployeeRecordPolicy;
import edu.sjsu.securehr.api.security.policy.RecordAccess;
import edu.sjsu.securehr.api.security.policy.SalaryPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Directory, record, title and salary use cases (UC-02 to UC-05). Every per-record method
 * authorizes first, then loads data, then audits sensitive reads.
 */
@Service
@Transactional(readOnly = true)
public class EmployeeService {

  /** Directory order; emp_no keeps paging stable for people with the same name. */
  static final Sort DIRECTORY_ORDER = Sort.by("lastName", "firstName", "empNo");

  private static final String NOT_FOUND = "Employee not found.";

  private final EmployeeRepository employees;
  private final TitleRepository titles;
  private final SalaryRepository salaries;
  private final DirectoryRecords directory;
  private final SearchTotals searchTotals;
  private final EmployeeRecordPolicy recordPolicy;
  private final SalaryPolicy salaryPolicy;
  private final AuditTrail auditTrail;

  /**
   * Creates the service.
   *
   * @param employees employee repository
   * @param titles title history repository
   * @param salaries salary history repository
   * @param directory current department and title lookups
   * @param searchTotals search totals
   * @param recordPolicy record access policy
   * @param salaryPolicy salary access policy
   * @param auditTrail audit event publisher
   */
  public EmployeeService(
      EmployeeRepository employees,
      TitleRepository titles,
      SalaryRepository salaries,
      DirectoryRecords directory,
      SearchTotals searchTotals,
      EmployeeRecordPolicy recordPolicy,
      SalaryPolicy salaryPolicy,
      AuditTrail auditTrail) {
    this.employees = employees;
    this.titles = titles;
    this.salaries = salaries;
    this.directory = directory;
    this.searchTotals = searchTotals;
    this.recordPolicy = recordPolicy;
    this.salaryPolicy = salaryPolicy;
    this.auditTrail = auditTrail;
  }

  /**
   * Searches current employees. Returns directory fields only, which every portal role may see.
   *
   * @param criteria search filters
   * @param page zero-based page
   * @param size requested page size (capped at {@link Paging#MAX_SIZE})
   * @return one page of summaries
   */
  public PageResponse<EmployeeSummaryDto> search(
      EmployeeSearchCriteria criteria, int page, int size) {
    Pageable pageable = Paging.of(page, size, DIRECTORY_ORDER);
    Page<Employee> result =
        PageableExecutionUtils.getPage(
            employees.findPage(EmployeeSpecifications.forPage(criteria), pageable),
            pageable,
            () -> searchTotals.count(criteria));
    return new PageResponse<>(
        directory.summarize(result.getContent()),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  /**
   * The caller and their own directory record.
   *
   * @param user the caller
   * @return the caller's profile
   */
  public MeDto me(CurrentUser user) {
    EmployeeSummaryDto own =
        Optional.ofNullable(user.empNo())
            .flatMap(employees::findById)
            .map(directory::summarize)
            .orElse(null);
    List<String> roles = user.roles().stream().map(Role::name).sorted().toList();
    return new MeDto(user.username(), user.empNo(), roles, own);
  }

  /**
   * Employee detail with restricted fields masked by role.
   *
   * @param user the caller
   * @param empNo requested employee
   * @return the employee
   */
  public EmployeeDetailDto detail(CurrentUser user, int empNo) {
    RecordAccess access = recordPolicy.authorize(user, empNo);
    Employee employee = employees.findById(empNo).orElseThrow(EmployeeService::notFound);
    Optional<DeptEmp> membership = directory.currentMembership(empNo);
    boolean full = access == RecordAccess.FULL;
    Integer currentSalary =
        full
            ? salaries.findCurrentFor(empNo, Dates.CURRENT).map(Salary::getAmount).orElse(null)
            : null;
    auditOtherPersonRead(user, empNo);
    return new EmployeeDetailDto(
        employee.getEmpNo(),
        employee.getFirstName(),
        employee.getLastName(),
        employee.getGender().name(),
        employee.getHireDate(),
        membership.map(m -> m.getDepartment().getDeptNo()).orElse(null),
        membership.map(m -> m.getDepartment().getDeptName()).orElse(null),
        directory.currentTitle(empNo).orElse(null),
        full ? employee.getBirthDate() : null,
        currentSalary);
  }

  /**
   * Title history, under the same rule as the detail view.
   *
   * @param user the caller
   * @param empNo requested employee
   * @return titles, oldest first
   */
  public List<TitleRecordDto> titles(CurrentUser user, int empNo) {
    recordPolicy.authorize(user, empNo);
    requireExists(empNo);
    auditOtherPersonRead(user, empNo);
    return titles.findByIdEmpNoOrderByIdFromDateAsc(empNo).stream()
        .map(t ->
            new TitleRecordDto(
                t.getId().title(), t.getId().fromDate(), Dates.apiToDate(t.getToDate())))
        .toList();
  }

  /**
   * Salary history: self and HR Admin only. Every successful read is audited as SALARY_READ.
   *
   * @param user the caller
   * @param empNo requested employee
   * @return salaries, oldest first
   */
  public List<SalaryRecordDto> salaries(CurrentUser user, int empNo) {
    salaryPolicy.authorize(user, empNo);
    requireExists(empNo);
    List<SalaryRecordDto> history =
        salaries.findByIdEmpNoOrderByIdFromDateAsc(empNo).stream()
            .map(s ->
                new SalaryRecordDto(
                    s.getAmount(), s.getId().fromDate(), Dates.apiToDate(s.getToDate())))
            .toList();
    auditTrail.record(AuditEventType.SALARY_READ, AuditOutcome.ALLOWED, user, empNo);
    return history;
  }

  private void requireExists(int empNo) {
    if (employees.findById(empNo).isEmpty()) {
      throw notFound();
    }
  }

  // Reading your own record is routine; reading anyone else's is a sensitive access (G4).
  private void auditOtherPersonRead(CurrentUser user, int empNo) {
    if (!user.isEmployee(empNo)) {
      auditTrail.record(AuditEventType.DATA_READ, AuditOutcome.ALLOWED, user, empNo);
    }
  }

  private static ResourceNotFoundException notFound() {
    return new ResourceNotFoundException(NOT_FOUND);
  }
}
