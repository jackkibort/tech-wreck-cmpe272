package edu.sjsu.securehr.api.dto;

import java.util.List;

/**
 * Management chain from an employee up to the current manager of their current department.
 *
 * @param deptNo the employee's current department, or {@code null} for former employees
 * @param deptName that department's name, or {@code null}
 * @param chain the employee first, their department manager last; one element when the employee is
 *     that manager or has no current department
 */
public record OrgChainDto(String deptNo, String deptName, List<OrgNodeDto> chain) {

  /** Defensive copy of the chain. */
  public OrgChainDto {
    chain = List.copyOf(chain);
  }
}
