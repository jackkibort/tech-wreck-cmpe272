package edu.sjsu.securehr.api.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Paged response with exactly the Section 0.6 fields.
 *
 * @param content items on this page
 * @param page zero-based page number
 * @param size page size actually applied (after the cap)
 * @param totalElements total matching items
 * @param totalPages total pages
 * @param <T> item type
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  /** Defensive copy of the content. */
  public PageResponse {
    content = List.copyOf(content);
  }

  /**
   * Converts a Spring Data page, mapping each item.
   *
   * @param page source page
   * @param mapper item mapping
   * @param <S> source item type
   * @param <T> response item type
   * @return the response page
   */
  public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
    return new PageResponse<>(
        page.getContent().stream().map(mapper).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
