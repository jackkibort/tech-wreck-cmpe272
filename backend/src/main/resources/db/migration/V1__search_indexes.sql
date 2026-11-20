-- Search and authorization-scope indexes for the Employee API (Section 7.3, goal G3).
-- Adds indexes only: no table, column or row of the original dataset is changed.
-- Tables are schema-qualified because Flyway's own history lives in securehr_flyway.

-- Name prefix search (q) and the default sort order: last name, first name, emp_no.
-- InnoDB appends the primary key (emp_no) to every secondary index, which completes the sort.
CREATE INDEX idx_employees_last_first ON employees.employees (last_name, first_name);
CREATE INDEX idx_employees_first_name ON employees.employees (first_name);

-- Hire-date range filters (hiredFrom, hiredTo).
CREATE INDEX idx_employees_hire_date ON employees.employees (hire_date);

-- Current members of a department: department filter and manager scope (Section 8.2).
CREATE INDEX idx_dept_emp_dept_current ON employees.dept_emp (dept_no, to_date, emp_no);

-- Employees holding a given current title: title filter.
CREATE INDEX idx_titles_title_current ON employees.titles (title, to_date, emp_no);
