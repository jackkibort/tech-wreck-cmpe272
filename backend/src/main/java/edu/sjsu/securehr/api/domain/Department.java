package edu.sjsu.securehr.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Row of the original {@code departments} table. */
@Entity
@Immutable
@Table(name = "departments")
public class Department {

  @Id
  @Column(name = "dept_no")
  private String deptNo;

  @Column(name = "dept_name")
  private String deptName;

  /** Required by JPA. */
  protected Department() {}

  public String getDeptNo() {
    return deptNo;
  }

  public String getDeptName() {
    return deptName;
  }
}
