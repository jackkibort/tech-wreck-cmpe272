package edu.sjsu.securehr.api.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Paging rules from Section 0.6: page starts at 0, size defaults to 25 and is capped at 100. */
public final class Paging {

  /** Page size when none is requested. */
  public static final int DEFAULT_SIZE = 25;

  /** Largest page ever returned (Section 8.2), regardless of what was requested. */
  public static final int MAX_SIZE = 100;

  private Paging() {}

  /**
   * Builds a page request, capping the size.
   *
   * @param page zero-based page number (validated as non-negative by the controller)
   * @param size requested size (validated as positive by the controller)
   * @param sort sort order
   * @return the page request
   */
  public static PageRequest of(int page, int size, Sort sort) {
    return PageRequest.of(page, Math.min(size, MAX_SIZE), sort);
  }
}
