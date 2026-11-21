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
 * Department membership over time (original {@code dept_emp} table). The current membership has
 * {@code to_date = 9999-01-01}; every employee has at most one.
 */
@Entity
@Immutable
@Table(name = "dept_emp")
public class DeptEmp {

  /** Composite primary key (emp_no, dept_no). */
  @Embeddable
  public record Key(@Column(name = "emp_no") Integer empNo, @Column(name = "dept_no") String deptNo)
      implements Serializable {}

  @EmbeddedId private Key id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dept_no", insertable = false, updatable = false)
  private Department department;

  @Column(name = "from_date")
  private LocalDate fromDate;

  @Column(name = "to_date")
  private LocalDate toDate;

  /** Required by JPA. */
  protected DeptEmp() {}

  public Key getId() {
    return id;
  }

  public Department getDepartment() {
    return department;
  }

  public LocalDate getFromDate() {
    return fromDate;
  }

  public LocalDate getToDate() {
    return toDate;
  }
}
