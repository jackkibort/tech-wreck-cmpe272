package edu.sjsu.securehr.api.controller;

import edu.sjsu.securehr.api.dto.DepartmentDto;
import edu.sjsu.securehr.api.dto.OrgChainDto;
import edu.sjsu.securehr.api.service.OrganizationService;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/departments} and {@code GET /api/org/{empNo}}. */
@RestController
public class OrganizationController {

  private final OrganizationService organizationService;

  /**
   * Creates the controller.
   *
   * @param organizationService department and org-chart use cases
   */
  public OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @GetMapping("/api/departments")
  List<DepartmentDto> departments() {
    return organizationService.departments();
  }

  @GetMapping("/api/org/{empNo}")
  OrgChainDto chain(@PathVariable @Min(1) int empNo) {
    return organizationService.chain(empNo);
  }
}
