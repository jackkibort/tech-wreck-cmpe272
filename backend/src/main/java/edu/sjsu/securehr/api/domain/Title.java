package edu.sjsu.securehr.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

/** Title history (original {@code titles} table). */
@Entity
@Immutable
@Table(name = "titles")
public class Title {

  /** Composite primary key (emp_no, title, from_date). */
  @Embeddable
  public record Key(
      @Column(name = "emp_no") Integer empNo,
      @Column(name = "title") String title,
      @Column(name = "from_date") LocalDate fromDate)
      implements Serializable {}

  @EmbeddedId private Key id;

  @Column(name = "to_date")
  private LocalDate toDate;

  /** Required by JPA. */
  protected Title() {}

  public Key getId() {
    return id;
  }

  public LocalDate getToDate() {
    return toDate;
  }
}
