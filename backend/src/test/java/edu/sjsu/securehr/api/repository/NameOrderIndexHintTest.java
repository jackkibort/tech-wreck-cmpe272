package edu.sjsu.securehr.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameOrderIndexHintTest {

  private final NameOrderIndexHint inspector = new NameOrderIndexHint();

  @Test
  void addsHintToDirectoryPage() {
    String page =
        "select e1_0.emp_no,e1_0.last_name from employees e1_0 where (select count(de1_0.emp_no)"
            + " from dept_emp de1_0 where de1_0.emp_no=e1_0.emp_no)>? order by"
            + " e1_0.last_name,e1_0.first_name,e1_0.emp_no limit ?,?";

    assertThat(inspector.inspect(page))
        .isEqualTo(
            "select e1_0.emp_no,e1_0.last_name from employees e1_0 "
                + NameOrderIndexHint.HINT
                + " where (select count(de1_0.emp_no) from dept_emp de1_0 where"
                + " de1_0.emp_no=e1_0.emp_no)>? order by e1_0.last_name,e1_0.first_name,e1_0.emp_no"
                + " limit ?,?");
  }

  @Test
  void leavesOtherStatementsAlone() {
    String count = "select count(e1_0.emp_no) from employees e1_0 where e1_0.hire_date>=?";
    String byId = "select e1_0.emp_no from employees e1_0 where e1_0.emp_no=?";
    String otherOrder =
        "select e1_0.emp_no from employees e1_0 where e1_0.emp_no>?"
            + " order by e1_0.hire_date limit ?";
    String otherTable =
        "select d1_0.dept_no from departments d1_0 where d1_0.dept_no=? order by"
            + " d1_0.last_name,d1_0.first_name,d1_0.emp_no limit ?";

    assertThat(inspector.inspect(count)).isEqualTo(count);
    assertThat(inspector.inspect(byId)).isEqualTo(byId);
    assertThat(inspector.inspect(otherOrder)).isEqualTo(otherOrder);
    assertThat(inspector.inspect(otherTable)).isEqualTo(otherTable);
  }
}
