package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.Department;
import java.util.List;
import org.springframework.data.repository.Repository;

/** Read-only access to departments. */
public interface DepartmentRepository extends Repository<Department, String> {

  /**
   * Every department.
   *
   * @return departments ordered by number
   */
  List<Department> findAllByOrderByDeptNoAsc();
}
