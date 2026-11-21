package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.DeptManager;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only access to department managers. */
public interface DeptManagerRepository extends Repository<DeptManager, DeptManager.Key> {

  /**
   * Departments the employee manages today (manager scope, Section 8.2).
   *
   * @param empNo manager's employee number
   * @param current the dataset's "current" end date
   * @return department numbers, empty if not a current manager
   */
  @Query(
      """
      select dm.id.deptNo from DeptManager dm
      where dm.id.empNo = :empNo and dm.toDate = :current
      """)
  List<String> findCurrentDeptNosManagedBy(
      @Param("empNo") int empNo, @Param("current") LocalDate current);

  /**
   * All current managers with their employee record loaded.
   *
   * @param current the dataset's "current" end date
   * @return current manager rows
   */
  @Query(
      """
      select dm from DeptManager dm join fetch dm.employee
      where dm.toDate = :current
      """)
  List<DeptManager> findAllCurrent(@Param("current") LocalDate current);
}
