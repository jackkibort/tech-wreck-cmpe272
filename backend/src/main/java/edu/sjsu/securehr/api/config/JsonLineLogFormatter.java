package edu.sjsu.securehr.api.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.logging.structured.StructuredLogFormatter;

/**
 * Writes every log event as one JSON object per line on standard output (Section 0.10).
 *
 * <p>Events from the {@value #AUDIT_LOGGER} logger are audit events: they carry {@code
 * log_type=audit} and their fields come only from the event's key/value pairs, so the Section 11.1
 * field set is exact. All other events carry {@code log_type=app}.
 */
public class JsonLineLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

  /** Name of the logger used exclusively for audit events. */
  public static final String AUDIT_LOGGER = "securehr.audit";

  /** MDC key holding the edge-provided request ID. */
  public static final String REQUEST_ID_MDC_KEY = "request_id";

  private static final String SERVICE_NAME = "api";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String format(ILoggingEvent event) {
    Map<String, Object> line =
        AUDIT_LOGGER.equals(event.getLoggerName()) ? auditLine(event) : appLine(event);
    try {
      return objectMapper.writeValueAsString(line) + System.lineSeparator();
    } catch (JsonProcessingException e) {
      // Logging must never fail the request; fall back to a minimal, always-valid line.
      return "{\"log_type\":\"app\",\"level\":\"ERROR\",\"message\":\"unserializable log event\"}"
          + System.lineSeparator();
    }
  }

  private Map<String, Object> appLine(ILoggingEvent event) {
    Map<String, Object> line = new LinkedHashMap<>();
    line.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
    line.put("log_type", "app");
    line.put("service", SERVICE_NAME);
    line.put("level", event.getLevel().toString());
    line.put("logger", event.getLoggerName());
    line.put("thread", event.getThreadName());
    line.put("message", event.getFormattedMessage());
    String requestId = event.getMDCPropertyMap().get(REQUEST_ID_MDC_KEY);
    if (requestId != null) {
      line.put(REQUEST_ID_MDC_KEY, requestId);
    }
    IThrowableProxy throwable = event.getThrowableProxy();
    if (throwable != null) {
      line.put("exception", ThrowableProxyUtil.asString(throwable));
    }
    return line;
  }

  private Map<String, Object> auditLine(ILoggingEvent event) {
    Map<String, Object> line = new LinkedHashMap<>();
    line.put("log_type", "audit");
    line.put("service", SERVICE_NAME);
    if (event.getKeyValuePairs() != null) {
      for (KeyValuePair pair : event.getKeyValuePairs()) {
        line.put(pair.key, pair.value);
      }
    }
    return line;
  }
}
