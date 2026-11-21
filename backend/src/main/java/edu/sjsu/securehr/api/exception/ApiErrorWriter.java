package edu.sjsu.securehr.api.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sjsu.securehr.api.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes {@link ApiError} bodies from servlet-level handlers (the security filter chain), where
 * Spring MVC's exception handling does not apply.
 */
@Component
public class ApiErrorWriter {

  private final ObjectMapper objectMapper;

  /**
   * Creates the writer.
   *
   * @param objectMapper application JSON mapper
   */
  public ApiErrorWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Writes an error response.
   *
   * @param response servlet response
   * @param status HTTP status
   * @param message safe explanation
   * @throws IOException if the response cannot be written
   */
  public void write(HttpServletResponse response, HttpStatus status, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        ApiError.of(status, message, RequestIdFilter.currentRequestId()));
  }
}
