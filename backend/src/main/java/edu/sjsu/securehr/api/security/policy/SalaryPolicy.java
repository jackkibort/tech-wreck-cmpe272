package edu.sjsu.securehr.api.security.policy;

import edu.sjsu.securehr.api.exception.ResourceAccessDeniedException;
import edu.sjsu.securehr.api.security.CurrentUser;
import edu.sjsu.securehr.api.security.Role;
import org.springframework.stereotype.Component;

/**
 * Who may read salary history (Section 8.2): the employee themself and HR Admin only. Managers are
 * deliberately excluded, even for their own team.
 */
@Component
public class SalaryPolicy {

  /**
   * Checks salary access.
   *
   * @param user the caller
   * @param targetEmpNo the employee whose salary is requested
   * @throws ResourceAccessDeniedException if the caller may not see the salary
   */
  public void authorize(CurrentUser user, int targetEmpNo) {
    if (!user.isEmployee(targetEmpNo) && !user.hasRole(Role.HR_ADMIN)) {
      throw new ResourceAccessDeniedException(
          "You are not allowed to view this employee's salary.", targetEmpNo);
    }
  }
}
