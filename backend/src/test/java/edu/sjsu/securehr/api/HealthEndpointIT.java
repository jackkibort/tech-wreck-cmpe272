package edu.sjsu.securehr.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.sjsu.securehr.api.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@ApiIntegrationTest
class HealthEndpointIT {

  @Autowired private MockMvc mvc;

  @Test
  void healthReportsOnlyUpOrDownWithoutToken() throws Exception {
    // Section 0.6: status only, no component or detail leakage; internal callers need no token.
    mvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"status\":\"UP\"}", true));
  }
}
