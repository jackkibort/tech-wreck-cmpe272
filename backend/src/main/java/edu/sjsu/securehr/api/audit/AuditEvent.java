package edu.sjsu.securehr.api.audit;

import java.time.Instant;
import java.util.List;

/**
 * One audit event with exactly the Section 11.1 fields. Never carries salaries, birth dates or
 * tokens: people are identified only by username and emp_no (Section 0.10).
 *
 * @param timestamp UTC time of the action
 * @param eventType fixed event type
 * @param actorUsername Keycloak username, or {@code null} if unauthenticated
 * @param actorEmpNo linked emp_no of the caller, if any
 * @param actorRoles roles in the caller's token
 * @param targetEmpNo employee whose data was touched, if any
 * @param resource API path
 * @param outcome ALLOWED or DENIED
 * @param sourceIp client IP as resolved from X-Forwarded-For by the trusted edge
 * @param requestId X-Request-ID from the edge
 */
public record AuditEvent(
    Instant timestamp,
    AuditEventType eventType,
    String actorUsername,
    Integer actorEmpNo,
    List<String> actorRoles,
    Integer targetEmpNo,
    String resource,
    AuditOutcome outcome,
    String sourceIp,
    String requestId) {

  /** Defensive copy of the role list. */
  public AuditEvent {
    actorRoles = List.copyOf(actorRoles);
  }
}
