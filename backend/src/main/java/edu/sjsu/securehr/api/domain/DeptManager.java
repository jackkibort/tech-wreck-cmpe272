package edu.sjsu.securehr.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

/**
 * Department managers over time (original {@code dept_manager} table). The current manager of a
 * department has {@code to_date = 9999-01-01}.
 */
@Entity
@Immutable
@Table(name = "dept_manager")
public class DeptManager {

  /** Composite primary key (emp_no, dept_no). */
  @Embeddable
  public record Key(@Column(name = "emp_no") Integer empNo, @Column(name = "dept_no") String deptNo)
      implements Serializable {}

  @EmbeddedId private Key id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "emp_no", insertable = false, updatable = false)
  private Employee employee;

  @Column(name = "from_date")
  private LocalDate fromDate;

  @Column(name = "to_date")
  private LocalDate toDate;

  /** Required by JPA. */
  protected DeptManager() {}

  public Key getId() {
    return id;
  }

  public Employee getEmployee() {
    return employee;
  }

  public LocalDate getFromDate() {
    return fromDate;
  }

  public LocalDate getToDate() {
    return toDate;
  }
}
