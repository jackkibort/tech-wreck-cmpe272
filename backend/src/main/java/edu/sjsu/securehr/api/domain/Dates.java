package edu.sjsu.securehr.api.domain;

import java.time.LocalDate;

/** Dataset date conventions (Section 7.2). */
public final class Dates {

  /** The dataset marks the current row of every history table with this {@code to_date}. */
  public static final LocalDate CURRENT = LocalDate.of(9999, 1, 1);

  private Dates() {}

  /**
   * Converts a dataset {@code to_date} to its API form, where {@code null} means "current".
   *
   * @param toDate stored end date (may be {@code null} in {@code titles})
   * @return the end date, or {@code null} for the current row
   */
  public static LocalDate apiToDate(LocalDate toDate) {
    return toDate == null || CURRENT.equals(toDate) ? null : toDate;
  }
}
