package edu.sjsu.securehr.api.support;

import edu.sjsu.securehr.api.audit.AuditEvent;
import edu.sjsu.securehr.api.audit.AuditEventType;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.event.EventListener;

/** Records every published audit event so tests can assert exactly what was audited. */
@TestComponent
public class CapturedAuditEvents {

  private final List<AuditEvent> events = new CopyOnWriteArrayList<>();

  @EventListener
  void capture(AuditEvent event) {
    events.add(event);
  }

  public void clear() {
    events.clear();
  }

  public List<AuditEvent> all() {
    return List.copyOf(events);
  }

  public List<AuditEvent> ofType(AuditEventType type) {
    return events.stream().filter(e -> e.eventType() == type).toList();
  }
}
