package edu.sjsu.securehr.api.audit;

import edu.sjsu.securehr.api.security.CurrentUser;
import edu.sjsu.securehr.api.security.Role;
import edu.sjsu.securehr.api.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Entry point for recording audit events. Fills in the request context (path, client IP, request
 * ID) and publishes an {@link AuditEvent}; {@link AuditLogListener} writes it (Observer pattern,
 * Section 12.4). Producers never write audit logs directly.
 */
@Component
public class AuditTrail {

  private final ApplicationEventPublisher publisher;
  private final Clock clock;

  /**
   * Creates the audit trail.
   *
   * @param publisher Spring application event publisher
   * @param clock time source (UTC)
   */
  public AuditTrail(ApplicationEventPublisher publisher, Clock clock) {
    this.publisher = publisher;
    this.clock = clock;
  }

  /**
   * Records an action by the authenticated caller.
   *
   * @param type event type
   * @param outcome whether the action was allowed
   * @param actor caller, or {@code null} if unknown
   * @param targetEmpNo employee whose data was touched, if any
   */
  public void record(
      AuditEventType type, AuditOutcome outcome, CurrentUser actor, Integer targetEmpNo) {
    HttpServletRequest request = currentRequest();
    List<String> roles =
        actor == null ? List.of() : actor.roles().stream().map(Role::name).sorted().toList();
    publisher.publishEvent(
        new AuditEvent(
            Instant.now(clock),
            type,
            actor == null ? null : actor.username(),
            actor == null ? null : actor.empNo(),
            roles,
            targetEmpNo,
            request == null ? null : request.getRequestURI(),
            outcome,
            // With forward-headers-strategy=native, Tomcat's RemoteIpValve has already resolved
            // the client from X-Forwarded-For, trusting only internal proxies (the edge).
            request == null ? null : request.getRemoteAddr(),
            RequestIdFilter.currentRequestId()));
  }

  private static HttpServletRequest currentRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    return attributes instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
  }
}
