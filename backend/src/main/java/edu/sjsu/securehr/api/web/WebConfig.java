package edu.sjsu.securehr.api.web;

import edu.sjsu.securehr.api.security.CurrentUserArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring MVC wiring. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final CurrentUserArgumentResolver currentUserArgumentResolver;

  /**
   * Creates the configuration.
   *
   * @param currentUserArgumentResolver resolves {@code CurrentUser} controller parameters
   */
  public WebConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
    this.currentUserArgumentResolver = currentUserArgumentResolver;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentUserArgumentResolver);
  }
}
