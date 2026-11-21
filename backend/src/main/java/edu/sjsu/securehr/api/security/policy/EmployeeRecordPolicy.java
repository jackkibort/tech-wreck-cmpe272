package edu.sjsu.securehr.api.security.policy;

import edu.sjsu.securehr.api.exception.ResourceAccessDeniedException;
import edu.sjsu.securehr.api.security.CurrentUser;
import edu.sjsu.securehr.api.security.Role;
import org.springframework.stereotype.Component;

/**
 * Who may read another employee's full record and title history (Section 8.2 matrix): themself and
 * HR Admin fully, a manager for in-scope team members without restricted fields, nobody else.
 */
@Component
public class EmployeeRecordPolicy {

  private final ManagerScope managerScope;

  /**
   * Creates the policy.
   *
   * @param managerScope manager-scope check
   */
  public EmployeeRecordPolicy(ManagerScope managerScope) {
    this.managerScope = managerScope;
  }

  /**
   * Decides access to an employee record. Runs before any existence check so a denial never reveals
   * whether the record exists.
   *
   * @param user the caller
   * @param targetEmpNo the employee being requested
   * @return the level of access granted
   * @throws ResourceAccessDeniedException if the caller may not see the record
   */
  public RecordAccess authorize(CurrentUser user, int targetEmpNo) {
    if (user.isEmployee(targetEmpNo) || user.hasRole(Role.HR_ADMIN)) {
      return RecordAccess.FULL;
    }
    if (user.hasRole(Role.MANAGER)
        && user.empNo() != null
        && managerScope.includes(user.empNo(), targetEmpNo)) {
      return RecordAccess.WITHOUT_RESTRICTED_FIELDS;
    }
    throw new ResourceAccessDeniedException(
        "You are not allowed to view this employee's record.", targetEmpNo);
  }
}
