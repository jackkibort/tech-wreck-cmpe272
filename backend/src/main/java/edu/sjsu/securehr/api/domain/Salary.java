package edu.sjsu.securehr.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

/** Salary history (original {@code salaries} table). Restricted data: never logged. */
@Entity
@Immutable
@Table(name = "salaries")
public class Salary {

  /** Composite primary key (emp_no, from_date). */
  @Embeddable
  public record Key(
      @Column(name = "emp_no") Integer empNo, @Column(name = "from_date") LocalDate fromDate)
      implements Serializable {}

  @EmbeddedId private Key id;

  @Column(name = "salary")
  private Integer amount;

  @Column(name = "to_date")
  private LocalDate toDate;

  /** Required by JPA. */
  protected Salary() {}

  public Key getId() {
    return id;
  }

  public Integer getAmount() {
    return amount;
  }

  public LocalDate getToDate() {
    return toDate;
  }
}
