package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.Employee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

/** Read-only access to employees; search filters are composed as Specifications. */
public interface EmployeeRepository
    extends Repository<Employee, Integer>, JpaSpecificationExecutor<Employee> {

  /**
   * Finds an employee by number.
   *
   * @param empNo employee number
   * @return the employee, if it exists
   */
  Optional<Employee> findById(Integer empNo);
}
