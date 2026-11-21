package edu.sjsu.securehr.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Error body shared by both SecureHR APIs (Section 0.6).
 *
 * @param status HTTP status code
 * @param error HTTP reason phrase
 * @param message human-readable explanation that never reveals internal details
 * @param requestId edge request ID for correlating with logs
 */
public record ApiError(int status, String error, String message, String requestId) {

  /**
   * Creates an error body for a status.
   *
   * @param status HTTP status
   * @param message safe explanation
   * @param requestId current request ID
   * @return the error body
   */
  public static ApiError of(HttpStatus status, String message, String requestId) {
    return new ApiError(status.value(), status.getReasonPhrase(), message, requestId);
  }
}
