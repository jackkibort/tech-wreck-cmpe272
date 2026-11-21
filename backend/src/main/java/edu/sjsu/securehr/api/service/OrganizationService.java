package edu.sjsu.securehr.api.service;

import edu.sjsu.securehr.api.domain.Dates;
import edu.sjsu.securehr.api.domain.DeptEmp;
import edu.sjsu.securehr.api.domain.DeptManager;
import edu.sjsu.securehr.api.domain.Employee;
import edu.sjsu.securehr.api.dto.DepartmentDto;
import edu.sjsu.securehr.api.dto.EmployeeRefDto;
import edu.sjsu.securehr.api.dto.OrgChainDto;
import edu.sjsu.securehr.api.dto.OrgNodeDto;
import edu.sjsu.securehr.api.exception.ResourceNotFoundException;
import edu.sjsu.securehr.api.repository.DepartmentRepository;
import edu.sjsu.securehr.api.repository.DeptManagerRepository;
import edu.sjsu.securehr.api.repository.EmployeeRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Department list and management chain (UC-06). Both return directory fields only, which every
 * portal role may see, so no per-record policy applies and nothing is audited.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationService {

  /** If a department ever has two current managers, report the most recently appointed. */
  private static final BinaryOperator<DeptManager> LATEST_APPOINTED =
      BinaryOperator.maxBy(Comparator.comparing(DeptManager::getFromDate));

  private final DepartmentRepository departments;
  private final DeptManagerRepository deptManagers;
  private final EmployeeRepository employees;
  private final DirectoryRecords directory;

  /**
   * Creates the service.
   *
   * @param departments department repository
   * @param deptManagers department manager history
   * @param employees employee repository
   * @param directory current department and title lookups
   */
  public OrganizationService(
      DepartmentRepository departments,
      DeptManagerRepository deptManagers,
      EmployeeRepository employees,
      DirectoryRecords directory) {
    this.departments = departments;
    this.deptManagers = deptManagers;
    this.employees = employees;
    this.directory = directory;
  }

  /**
   * Every department with its current manager.
   *
   * @return departments ordered by number
   */
  public List<DepartmentDto> departments() {
    Map<String, DeptManager> managers = currentManagersByDepartment();
    return departments.findAllByOrderByDeptNoAsc().stream()
        .map(d ->
            new DepartmentDto(
                d.getDeptNo(),
                d.getDeptName(),
                Optional.ofNullable(managers.get(d.getDeptNo()))
                    .map(DeptManager::getEmployee)
                    .map(OrganizationService::ref)
                    .orElse(null)))
        .toList();
  }

  /**
   * The chain from an employee to the current manager of their current department.
   *
   * @param empNo employee number
   * @return the chain, employee first
   */
  public OrgChainDto chain(int empNo) {
    Employee employee =
        employees
            .findById(empNo)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));
    List<OrgNodeDto> chain = new ArrayList<>();
    chain.add(node(employee));
    Optional<DeptEmp> membership = directory.currentMembership(empNo);
    if (membership.isEmpty()) {
      return new OrgChainDto(null, null, chain);
    }
    String deptNo = membership.get().getDepartment().getDeptNo();
    DeptManager manager = currentManagersByDepartment().get(deptNo);
    if (manager != null && manager.getId().empNo() != empNo) {
      chain.add(node(manager.getEmployee()));
    }
    return new OrgChainDto(deptNo, membership.get().getDepartment().getDeptName(), chain);
  }

  private Map<String, DeptManager> currentManagersByDepartment() {
    return deptManagers.findAllCurrent(Dates.CURRENT).stream()
        .collect(Collectors.toMap(dm -> dm.getId().deptNo(), dm -> dm, LATEST_APPOINTED));
  }

  private OrgNodeDto node(Employee e) {
    return new OrgNodeDto(
        e.getEmpNo(),
        e.getFirstName(),
        e.getLastName(),
        directory.currentTitle(e.getEmpNo()).orElse(null));
  }

  private static EmployeeRefDto ref(Employee e) {
    return new EmployeeRefDto(e.getEmpNo(), e.getFirstName(), e.getLastName());
  }
}
