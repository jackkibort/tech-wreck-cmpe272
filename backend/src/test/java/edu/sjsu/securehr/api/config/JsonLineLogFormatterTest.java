package edu.sjsu.securehr.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;

class JsonLineLogFormatterTest {

  private final JsonLineLogFormatter formatter = new JsonLineLogFormatter();
  private final ObjectMapper mapper = new ObjectMapper();
  private final LoggerContext context = new LoggerContext();

  @Test
  void appEventIsSingleJsonLineWithAppLogTypeAndRequestId() throws Exception {
    LoggingEvent event =
        event(
            Map.of(JsonLineLogFormatter.REQUEST_ID_MDC_KEY, "req-1"),
            "edu.sjsu.securehr.api.Example",
            "hello {}",
            "world");

    String line = formatter.format(event);

    assertThat(line).endsWith(System.lineSeparator());
    assertThat(line.strip()).doesNotContain("\n");
    JsonNode json = mapper.readTree(line);
    assertThat(json.get("log_type").asText()).isEqualTo("app");
    assertThat(json.get("service").asText()).isEqualTo("api");
    assertThat(json.get("message").asText()).isEqualTo("hello world");
    assertThat(json.get("request_id").asText()).isEqualTo("req-1");
  }

  @Test
  void auditEventContainsOnlyItsKeyValueFields() throws Exception {
    LoggingEvent event = event(Map.of(), JsonLineLogFormatter.AUDIT_LOGGER, "audit");
    event.addKeyValuePair(new KeyValuePair("event_type", "SALARY_READ"));
    event.addKeyValuePair(new KeyValuePair("target_emp_no", 10001));

    JsonNode json = mapper.readTree(formatter.format(event));

    assertThat(json.get("log_type").asText()).isEqualTo("audit");
    assertThat(json.get("event_type").asText()).isEqualTo("SALARY_READ");
    assertThat(json.get("target_emp_no").asInt()).isEqualTo(10001);
    assertThat(json.has("message")).isFalse();
  }

  private LoggingEvent event(
      Map<String, String> mdc, String loggerName, String message, Object... args) {
    LoggingEvent event =
        new LoggingEvent(
            getClass().getName(), context.getLogger(loggerName), Level.INFO, message, null, args);
    event.setMDCPropertyMap(mdc);
    return event;
  }
}
