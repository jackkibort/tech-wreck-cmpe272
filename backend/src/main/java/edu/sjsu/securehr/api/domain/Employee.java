package edu.sjsu.securehr.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

/** Row of the original {@code employees} table. Read-only: the dataset is never modified. */
@Entity
@Immutable
@Table(name = "employees")
public class Employee {

  /** Values of the dataset's {@code gender} ENUM column. */
  public enum Gender {
    M,
    F
  }

  @Id
  @Column(name = "emp_no")
  private Integer empNo;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender")
  private Gender gender;

  @Column(name = "hire_date")
  private LocalDate hireDate;

  /** Required by JPA. */
  protected Employee() {}

  public Integer getEmpNo() {
    return empNo;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public Gender getGender() {
    return gender;
  }

  public LocalDate getHireDate() {
    return hireDate;
  }
}
