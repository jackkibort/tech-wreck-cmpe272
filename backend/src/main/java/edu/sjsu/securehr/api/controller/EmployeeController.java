package edu.sjsu.securehr.api.controller;

import edu.sjsu.securehr.api.dto.EmployeeDetailDto;
import edu.sjsu.securehr.api.dto.EmployeeSearchCriteria;
import edu.sjsu.securehr.api.dto.EmployeeSummaryDto;
import edu.sjsu.securehr.api.dto.PageResponse;
import edu.sjsu.securehr.api.dto.SalaryRecordDto;
import edu.sjsu.securehr.api.dto.TitleRecordDto;
import edu.sjsu.securehr.api.security.CurrentUser;
import edu.sjsu.securehr.api.service.EmployeeService;
import edu.sjsu.securehr.api.service.Paging;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Employee directory and records. Role rules live in the security configuration and the service
 * policies; this class only maps HTTP to use cases.
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

  private final EmployeeService employeeService;

  /**
   * Creates the controller.
   *
   * @param employeeService employee use cases
   */
  public EmployeeController(EmployeeService employeeService) {
    this.employeeService = employeeService;
  }

  @GetMapping
  PageResponse<EmployeeSummaryDto> search(
      @Valid EmployeeSearchCriteria criteria,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "" + Paging.DEFAULT_SIZE) @Min(1) int size) {
    return employeeService.search(criteria, page, size);
  }

  @GetMapping("/{empNo}")
  EmployeeDetailDto detail(CurrentUser user, @PathVariable @Min(1) int empNo) {
    return employeeService.detail(user, empNo);
  }

  @GetMapping("/{empNo}/titles")
  List<TitleRecordDto> titles(CurrentUser user, @PathVariable @Min(1) int empNo) {
    return employeeService.titles(user, empNo);
  }

  @GetMapping("/{empNo}/salaries")
  List<SalaryRecordDto> salaries(CurrentUser user, @PathVariable @Min(1) int empNo) {
    return employeeService.salaries(user, empNo);
  }
}
