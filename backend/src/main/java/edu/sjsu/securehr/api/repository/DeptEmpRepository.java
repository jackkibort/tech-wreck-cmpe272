package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.DeptEmp;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only access to department membership.
 *
 * <p>Lookups use {@code left join fetch} although every membership has a department: a left join
 * makes MySQL start from the requested employees (primary key) instead of scanning each
 * department's members, which its statistics underestimate (200 ms down to 0.3 ms per page).
 */
public interface DeptEmpRepository extends Repository<DeptEmp, DeptEmp.Key> {

  /**
   * Current department memberships (with department loaded) for a batch of employees.
   *
   * @param empNos employees on the current page
   * @param current the dataset's "current" end date
   * @return current memberships
   */
  @Query(
      """
      select de from DeptEmp de left join fetch de.department
      where de.id.empNo in :empNos and de.toDate = :current
      """)
  List<DeptEmp> findCurrentFor(
      @Param("empNos") Collection<Integer> empNos, @Param("current") LocalDate current);

  /**
   * The employee's current membership, if they are a current employee.
   *
   * @param empNo employee number
   * @param current the dataset's "current" end date
   * @return current membership
   */
  @Query(
      """
      select de from DeptEmp de left join fetch de.department
      where de.id.empNo = :empNo and de.toDate = :current
      """)
  Optional<DeptEmp> findCurrentFor(@Param("empNo") int empNo, @Param("current") LocalDate current);

  /**
   * Number of current employees. Answered from the department-membership index alone.
   *
   * @param current the dataset's "current" end date
   * @return the headcount
   */
  @Query("select count(distinct de.id.empNo) from DeptEmp de where de.toDate = :current")
  long countCurrentEmployees(@Param("current") LocalDate current);

  /**
   * Number of current employees in one department.
   *
   * @param deptNo department number
   * @param current the dataset's "current" end date
   * @return the headcount
   */
  @Query(
      """
      select count(distinct de.id.empNo) from DeptEmp de
      where de.id.deptNo = :deptNo and de.toDate = :current
      """)
  long countCurrentEmployees(@Param("deptNo") String deptNo, @Param("current") LocalDate current);
}
