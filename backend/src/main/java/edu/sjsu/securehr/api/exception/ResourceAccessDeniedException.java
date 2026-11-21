package edu.sjsu.securehr.api.exception;

import org.springframework.security.access.AccessDeniedException;

/**
 * Thrown by services when an authorization policy denies access to a specific employee's data.
 * Carries the target so the denial can be audited with {@code target_emp_no}.
 */
public class ResourceAccessDeniedException extends AccessDeniedException {

  private final Integer targetEmpNo;

  /**
   * Creates the exception.
   *
   * @param message safe explanation returned to the caller
   * @param targetEmpNo employee whose data was requested, if any
   */
  public ResourceAccessDeniedException(String message, Integer targetEmpNo) {
    super(message);
    this.targetEmpNo = targetEmpNo;
  }

  /** Employee whose data was requested, or {@code null}. */
  public Integer getTargetEmpNo() {
    return targetEmpNo;
  }
}
