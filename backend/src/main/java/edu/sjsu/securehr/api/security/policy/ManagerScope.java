package edu.sjsu.securehr.api.security.policy;

import edu.sjsu.securehr.api.domain.Dates;
import edu.sjsu.securehr.api.repository.DeptEmpRepository;
import edu.sjsu.securehr.api.repository.DeptManagerRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manager scope (Section 8.2): a manager may see only <em>current</em> members of a department they
 * <em>currently</em> manage. Former managers and former members are out of scope.
 */
@Component
public class ManagerScope {

  private final DeptManagerRepository deptManagers;
  private final DeptEmpRepository deptEmps;

  /**
   * Creates the scope check.
   *
   * @param deptManagers department manager history
   * @param deptEmps department membership history
   */
  public ManagerScope(DeptManagerRepository deptManagers, DeptEmpRepository deptEmps) {
    this.deptManagers = deptManagers;
    this.deptEmps = deptEmps;
  }

  /**
   * Whether the target is a current member of a department the manager currently manages.
   *
   * @param managerEmpNo the manager's employee number
   * @param targetEmpNo the employee being requested
   * @return {@code true} if in scope
   */
  public boolean includes(int managerEmpNo, int targetEmpNo) {
    List<String> managed = deptManagers.findCurrentDeptNosManagedBy(managerEmpNo, Dates.CURRENT);
    if (managed.isEmpty()) {
      return false;
    }
    return deptEmps
        .findCurrentFor(targetEmpNo, Dates.CURRENT)
        .map(membership -> managed.contains(membership.getId().deptNo()))
        .orElse(false);
  }
}
