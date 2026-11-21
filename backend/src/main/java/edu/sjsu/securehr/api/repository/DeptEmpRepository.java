package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.DeptEmp;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only access to department membership. */
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
      select de from DeptEmp de join fetch de.department
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
      select de from DeptEmp de join fetch de.department
      where de.id.empNo = :empNo and de.toDate = :current
      """)
  Optional<DeptEmp> findCurrentFor(@Param("empNo") int empNo, @Param("current") LocalDate current);
}
