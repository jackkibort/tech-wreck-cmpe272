package edu.sjsu.securehr.api.support;

import edu.sjsu.securehr.api.security.Role;

/** Callers matching the fixture data in {@code V0_2__test_fixtures.sql}. */
public final class TestUsers {

  /** Regular Development employee. */
  public static final int EMPLOYEE_NO = 10001;

  /** Current manager of Development (d005). */
  public static final int MANAGER_NO = 10002;

  /** Sales employee: outside the manager's department. */
  public static final int OTHER_DEPT_NO = 10003;

  /** HR Admin's own record. */
  public static final int HR_ADMIN_NO = 10004;

  /** Moved from Development to Sales in 1995: no longer in the manager's scope. */
  public static final int TRANSFERRED_NO = 10005;

  /** Left the company: no current department. */
  public static final int FORMER_EMPLOYEE_NO = 10006;

  /** Former manager of Marketing (d001), now a regular member there. */
  public static final int FORMER_MANAGER_NO = 10007;

  /** Second Development member. */
  public static final int TEAMMATE_NO = 10009;

  /** An emp_no that does not exist. */
  public static final int MISSING_NO = 99999;

  private TestUsers() {}

  public static String employee() {
    return TestTokens.user("demo.employee", EMPLOYEE_NO, Role.EMPLOYEE).bearer();
  }

  public static String manager() {
    return TestTokens.user("demo.manager", MANAGER_NO, Role.EMPLOYEE, Role.MANAGER).bearer();
  }

  public static String formerManager() {
    return TestTokens.user("former.manager", FORMER_MANAGER_NO, Role.EMPLOYEE, Role.MANAGER)
        .bearer();
  }

  public static String hrAdmin() {
    return TestTokens.user("demo.hradmin", HR_ADMIN_NO, Role.EMPLOYEE, Role.HR_ADMIN).bearer();
  }

  public static String auditor() {
    return TestTokens.user("demo.auditor", null, Role.AUDITOR).bearer();
  }
}
