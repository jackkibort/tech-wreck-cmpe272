package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.Employee;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Fetches one page of a search without the count query that Spring Data would add, so the service
 * can count in the cheapest way for the filters in use.
 */
public interface EmployeePageQuery {

  /**
   * One page of matching employees.
   *
   * @param spec search filters
   * @param pageable page, size and sort
   * @return the employees on that page, in sort order
   */
  List<Employee> findPage(Specification<Employee> spec, Pageable pageable);
}
