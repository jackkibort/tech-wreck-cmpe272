package edu.sjsu.securehr.api.service;

import edu.sjsu.securehr.api.audit.AuditEventType;
import edu.sjsu.securehr.api.audit.AuditOutcome;
import edu.sjsu.securehr.api.audit.AuditTrail;
import edu.sjsu.securehr.api.domain.Employee;
import edu.sjsu.securehr.api.dto.EmployeeSearchCriteria;
import edu.sjsu.securehr.api.dto.EmployeeSummaryDto;
import edu.sjsu.securehr.api.repository.EmployeeRepository;
import edu.sjsu.securehr.api.repository.EmployeeSpecifications;
import edu.sjsu.securehr.api.security.CurrentUser;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CSV export of the directory search (UC-07). HR Admin only, enforced by the URL rules in {@code
 * SecurityConfig}. Every export is audited as EXPORT.
 */
@Service
@Transactional(readOnly = true)
public class ExportService {

  /** Largest export ever produced (Section 9). */
  public static final int MAX_ROWS = 10_000;

  /** Column order fixed by the employee-api spec. */
  static final List<String> HEADER =
      List.of(
          "emp_no",
          "first_name",
          "last_name",
          "gender",
          "hire_date",
          "dept_no",
          "dept_name",
          "title");

  /** Keeps the IN lists for department and title lookups to a reasonable size. */
  private static final int LOOKUP_BATCH = 1_000;

  private final EmployeeRepository employees;
  private final DirectoryRecords directory;
  private final AuditTrail auditTrail;
  private final int maxRows;

  /**
   * Creates the service.
   *
   * @param employees employee repository
   * @param directory current department and title lookups
   * @param auditTrail audit event publisher
   */
  @Autowired
  public ExportService(
      EmployeeRepository employees, DirectoryRecords directory, AuditTrail auditTrail) {
    this(employees, directory, auditTrail, MAX_ROWS);
  }

  /** Lets tests exercise truncation with a small cap. */
  ExportService(
      EmployeeRepository employees,
      DirectoryRecords directory,
      AuditTrail auditTrail,
      int maxRows) {
    this.employees = employees;
    this.directory = directory;
    this.auditTrail = auditTrail;
    this.maxRows = maxRows;
  }

  /**
   * Exports every current employee matching the filters, in directory order, up to {@link
   * #MAX_ROWS}.
   *
   * @param user the caller (already authorized as HR Admin)
   * @param criteria search filters
   * @return the CSV and whether it was truncated
   */
  public EmployeeExport export(CurrentUser user, EmployeeSearchCriteria criteria) {
    // One extra row tells us whether the cap cut anything off, without a count query.
    List<Employee> matches =
        employees.findPage(
            EmployeeSpecifications.forPage(criteria),
            PageRequest.of(0, maxRows + 1, EmployeeService.DIRECTORY_ORDER));
    boolean truncated = matches.size() > maxRows;
    List<Employee> rows = truncated ? matches.subList(0, maxRows) : matches;

    CsvWriter csv = new CsvWriter().row(HEADER);
    for (int from = 0; from < rows.size(); from += LOOKUP_BATCH) {
      List<Employee> batch = rows.subList(from, Math.min(from + LOOKUP_BATCH, rows.size()));
      List<EmployeeSummaryDto> summaries = directory.summarize(batch);
      for (int i = 0; i < batch.size(); i++) {
        csv.row(line(batch.get(i), summaries.get(i)));
      }
    }
    auditTrail.record(AuditEventType.EXPORT, AuditOutcome.ALLOWED, user, null);
    return new EmployeeExport(csv.toString(), rows.size(), truncated);
  }

  private static List<Object> line(Employee e, EmployeeSummaryDto summary) {
    // Arrays.asList, unlike List.of, allows the null cells of former employees.
    return Arrays.asList(
        e.getEmpNo(),
        e.getFirstName(),
        e.getLastName(),
        e.getGender().name(),
        e.getHireDate(),
        summary.deptNo(),
        summary.deptName(),
        summary.title());
  }
}
