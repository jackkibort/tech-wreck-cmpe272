package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.Salary;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only access to salary history. Callers must check the salary policy first. */
public interface SalaryRepository extends Repository<Salary, Salary.Key> {

  /**
   * Full salary history of one employee, oldest first.
   *
   * @param empNo employee number
   * @return salary history
   */
  List<Salary> findByIdEmpNoOrderByIdFromDateAsc(int empNo);

  /**
   * The employee's current salary row.
   *
   * @param empNo employee number
   * @param current the dataset's "current" end date
   * @return current salary, if the employee is still paid
   */
  @Query("select s from Salary s where s.id.empNo = :empNo and s.toDate = :current")
  Optional<Salary> findCurrentFor(@Param("empNo") int empNo, @Param("current") LocalDate current);
}
