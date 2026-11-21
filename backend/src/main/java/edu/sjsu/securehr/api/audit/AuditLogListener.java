package edu.sjsu.securehr.api.audit;

import edu.sjsu.securehr.api.config.JsonLineLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The only writer of audit events: one JSON line per event with {@code log_type=audit} on stdout,
 * collected by Alloy into Loki (Section 11). Every Section 11.1 field is always present, with
 * {@code null} when not applicable, so dashboards can rely on the shape.
 */
@Component
public class AuditLogListener {

  private static final Logger AUDIT = LoggerFactory.getLogger(JsonLineLogFormatter.AUDIT_LOGGER);

  /**
   * Writes an audit event.
   *
   * @param event the event to write
   */
  @EventListener
  public void onAuditEvent(AuditEvent event) {
    AUDIT
        .atInfo()
        .addKeyValue("timestamp", event.timestamp().toString())
        .addKeyValue("event_type", event.eventType().name())
        .addKeyValue("actor_username", event.actorUsername())
        .addKeyValue("actor_emp_no", event.actorEmpNo())
        .addKeyValue("actor_roles", event.actorRoles())
        .addKeyValue("target_emp_no", event.targetEmpNo())
        .addKeyValue("resource", event.resource())
        .addKeyValue("outcome", event.outcome().name())
        .addKeyValue("source_ip", event.sourceIp())
        .addKeyValue("request_id", event.requestId())
        .log(event.eventType().name());
  }
}
