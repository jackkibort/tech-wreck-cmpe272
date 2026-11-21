package edu.sjsu.securehr.api.controller;

import edu.sjsu.securehr.api.dto.MeDto;
import edu.sjsu.securehr.api.security.CurrentUser;
import edu.sjsu.securehr.api.service.EmployeeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/me}: the signed-in caller. */
@RestController
@RequestMapping("/api/me")
public class MeController {

  private final EmployeeService employeeService;

  /**
   * Creates the controller.
   *
   * @param employeeService employee use cases
   */
  public MeController(EmployeeService employeeService) {
    this.employeeService = employeeService;
  }

  @GetMapping
  MeDto me(CurrentUser user) {
    return employeeService.me(user);
  }
}
