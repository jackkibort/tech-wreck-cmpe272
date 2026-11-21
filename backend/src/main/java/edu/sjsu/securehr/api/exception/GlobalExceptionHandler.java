package edu.sjsu.securehr.api.exception;

import edu.sjsu.securehr.api.audit.AuditEventType;
import edu.sjsu.securehr.api.audit.AuditOutcome;
import edu.sjsu.securehr.api.audit.AuditTrail;
import edu.sjsu.securehr.api.security.CurrentUser;
import edu.sjsu.securehr.api.web.RequestIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The single place where exceptions become HTTP responses, always in the Section 0.6 format.
 * Messages are fixed, safe strings: internal details and stack traces are logged, never returned.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final AuditTrail auditTrail;

  /**
   * Creates the handler.
   *
   * @param auditTrail records denials raised by service-layer policies
   */
  public GlobalExceptionHandler(AuditTrail auditTrail) {
    this.auditTrail = auditTrail;
  }

  /** Policy denial for a specific record: audited with the target employee. */
  @ExceptionHandler(ResourceAccessDeniedException.class)
  ResponseEntity<ApiError> accessDenied(ResourceAccessDeniedException e) {
    auditTrail.record(
        AuditEventType.ACCESS_DENIED, AuditOutcome.DENIED, currentUser(), e.getTargetEmpNo());
    return error(HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ApiError> notFound(ResourceNotFoundException e) {
    return error(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler({
    BindException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    HandlerMethodValidationException.class,
    ConstraintViolationException.class,
    IllegalArgumentException.class
  })
  ResponseEntity<ApiError> badRequest(Exception e) {
    return error(HttpStatus.BAD_REQUEST, "One or more request parameters are invalid.");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiError> noRoute(NoResourceFoundException e) {
    return error(HttpStatus.NOT_FOUND, "Not found.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception e) {
    LOG.error("Unhandled exception", e);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
  }

  private static ResponseEntity<ApiError> error(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(ApiError.of(status, message, RequestIdFilter.currentRequestId()));
  }

  private static CurrentUser currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getPrincipal() instanceof Jwt jwt
        ? CurrentUser.fromToken(jwt)
        : null;
  }
}
