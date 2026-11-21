package edu.sjsu.securehr.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.sjsu.securehr.api.audit.AuditEventType;
import edu.sjsu.securehr.api.audit.AuditOutcome;
import edu.sjsu.securehr.api.audit.AuditTrail;
import edu.sjsu.securehr.api.domain.Employee;
import edu.sjsu.securehr.api.dto.EmployeeSearchCriteria;
import edu.sjsu.securehr.api.dto.EmployeeSummaryDto;
import edu.sjsu.securehr.api.repository.EmployeeRepository;
import edu.sjsu.securehr.api.security.CurrentUser;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Export cap behavior (TC-API-07), with a cap of 3 standing in for 10,000. */
class ExportServiceTest {

  private static final int CAP = 3;
  private static final EmployeeSearchCriteria NO_FILTERS =
      new EmployeeSearchCriteria(null, null, null, null, null);

  private final EmployeeRepository employees = mock(EmployeeRepository.class);
  private final DirectoryRecords directory = mock(DirectoryRecords.class);
  private final AuditTrail auditTrail = mock(AuditTrail.class);
  private final CurrentUser hrAdmin = mock(CurrentUser.class);
  private final ExportService service = new ExportService(employees, directory, auditTrail, CAP);

  @Test
  void productionCapIsTenThousand() {
    assertThat(ExportService.MAX_ROWS).isEqualTo(10_000);
  }

  @Test
  void tcApi07_moreMatchesThanCap_areTruncatedAndFlagged() {
    givenMatches(CAP + 1);

    EmployeeExport export = service.export(hrAdmin, NO_FILTERS);

    assertThat(export.rows()).isEqualTo(CAP);
    assertThat(export.truncated()).isTrue();
    assertThat(export.csv().lines()).hasSize(CAP + 1); // header + capped rows
    verify(auditTrail)
        .record(eq(AuditEventType.EXPORT), eq(AuditOutcome.ALLOWED), eq(hrAdmin), isNull());
  }

  @Test
  void exactlyCapMatches_areNotFlagged() {
    givenMatches(CAP);

    EmployeeExport export = service.export(hrAdmin, NO_FILTERS);

    assertThat(export.rows()).isEqualTo(CAP);
    assertThat(export.truncated()).isFalse();
  }

  @SuppressWarnings("unchecked")
  private void givenMatches(int count) {
    List<Employee> matches =
        IntStream.range(0, count).mapToObj(ExportServiceTest::employee).toList();
    when(employees.findPage(any(), any())).thenReturn(matches);
    when(directory.summarize(anyList()))
        .thenAnswer(invocation ->
            ((List<Employee>) invocation.getArgument(0))
                .stream()
                    .map(e ->
                        new EmployeeSummaryDto(
                            e.getEmpNo(), e.getFirstName(), e.getLastName(), "d005", "Dev", "Eng"))
                    .toList());
  }

  private static Employee employee(int i) {
    Employee e = mock(Employee.class);
    when(e.getEmpNo()).thenReturn(10_001 + i);
    when(e.getFirstName()).thenReturn("First" + i);
    when(e.getLastName()).thenReturn("Last" + i);
    when(e.getGender()).thenReturn(Employee.Gender.F);
    when(e.getHireDate()).thenReturn(LocalDate.of(1990, 1, 1));
    return e;
  }
}
