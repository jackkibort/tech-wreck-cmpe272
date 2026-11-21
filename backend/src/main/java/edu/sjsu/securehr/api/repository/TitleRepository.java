package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.Title;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only access to title history. */
public interface TitleRepository extends Repository<Title, Title.Key> {

  /**
   * Full title history of one employee, oldest first.
   *
   * @param empNo employee number
   * @return title history
   */
  List<Title> findByIdEmpNoOrderByIdFromDateAsc(int empNo);

  /**
   * Current titles for a batch of employees.
   *
   * @param empNos employees on the current page
   * @param current the dataset's "current" end date
   * @return current title rows
   */
  @Query("select t from Title t where t.id.empNo in :empNos and t.toDate = :current")
  List<Title> findCurrentFor(
      @Param("empNos") Collection<Integer> empNos, @Param("current") LocalDate current);
}
