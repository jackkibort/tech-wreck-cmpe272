package edu.sjsu.securehr.api.repository;

import edu.sjsu.securehr.api.domain.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

/**
 * Spring Data fragment implementing {@link EmployeePageQuery} with the Criteria API.
 *
 * <p>Every page is sorted by name, and MySQL is told to read that order from the name index.
 * Without the hint MySQL switches to sorting every employee once the page offset passes about 1,000
 * rows (2 s per request on the full dataset); with it, deep pages take milliseconds and the other
 * filters are unaffected. Measurements are in the backend README.
 */
class EmployeePageQueryImpl implements EmployeePageQuery {

  /** Applied by Hibernate's MySQL dialect right after the employees table in the FROM clause. */
  static final String NAME_ORDER_HINT = "FORCE INDEX FOR ORDER BY (idx_employees_last_first)";

  @PersistenceContext private EntityManager entityManager;

  @Override
  public List<Employee> findPage(Specification<Employee> spec, Pageable pageable) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Employee> query = cb.createQuery(Employee.class);
    Root<Employee> root = query.from(Employee.class);
    Predicate where = spec.toPredicate(root, query, cb);
    if (where != null) {
      query.where(where);
    }
    query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));
    return entityManager
        .createQuery(query)
        .setFirstResult(Math.toIntExact(pageable.getOffset()))
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }
}
