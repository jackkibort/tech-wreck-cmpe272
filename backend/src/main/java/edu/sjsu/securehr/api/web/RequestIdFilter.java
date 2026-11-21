package edu.sjsu.securehr.api.web;

import edu.sjsu.securehr.api.config.JsonLineLogFormatter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Carries the edge's {@code X-Request-ID} (Section 0.3) through logs, audit events and error
 * responses. Runs before Spring Security so even 401 responses carry the ID.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  /** Header set by the edge on every request. */
  public static final String HEADER = "X-Request-ID";

  // Only accept IDs that are safe to write into logs; anything else is replaced, which also
  // blocks log-injection through this header.
  private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String incoming = request.getHeader(HEADER);
    // Direct calls that bypass the edge (tests, health checks) still get a traceable ID.
    String requestId =
        incoming != null && SAFE_ID.matcher(incoming).matches()
            ? incoming
            : UUID.randomUUID().toString();
    MDC.put(JsonLineLogFormatter.REQUEST_ID_MDC_KEY, requestId);
    response.setHeader(HEADER, requestId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(JsonLineLogFormatter.REQUEST_ID_MDC_KEY);
    }
  }

  /** The current request's ID, or {@code null} outside a request. */
  public static String currentRequestId() {
    return MDC.get(JsonLineLogFormatter.REQUEST_ID_MDC_KEY);
  }
}
