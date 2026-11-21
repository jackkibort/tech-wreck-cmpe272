package edu.sjsu.securehr.api.security.policy;

/** How much of an employee record the caller may see. */
public enum RecordAccess {
  /** Every field, including birth date and salary: the employee themself or HR Admin. */
  FULL,
  /** Everything except restricted fields: a manager viewing a team member. */
  WITHOUT_RESTRICTED_FIELDS
}
