package edu.sjsu.securehr.api.exception;

/**
 * Thrown when a record does not exist. Services throw it only after authorization has passed, so a
 * 404 never reveals the existence of a record to a caller who may not see it (Section 8.2).
 */
public class ResourceNotFoundException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message safe explanation returned to the caller
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
