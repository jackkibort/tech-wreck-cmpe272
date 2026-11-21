-- Small fixed dataset for integration tests (Section 13.3: tests never depend on random emp_no).
-- Shape mirrors the real data: one current department per employee, one current manager per
-- department, '9999-01-01' marks current rows.
--
--   10001 Georgi Facello     d005 Development  Senior Engineer   regular employee (like demo.employee)
--   10002 Bezalel Simmel     d005 Development  Manager           current manager of d005 (like demo.manager)
--   10003 Parto Bamford      d007 Sales        Senior Staff      other department (like demo.employee2)
--   10004 Chirstian Koblick  d003 HR           Senior Staff      HR admin's own record (like demo.hradmin)
--   10005 Kyoichi Maliniak   d007 Sales        Staff             moved from d005 to d007 in 1995
--   10006 Anneke Preusig     (left company)    Engineer          former d005 member, no current dept
--   10007 Tzvetan Zielinski  d001 Marketing    Senior Staff      former manager of d001 (until 1995)
--   10008 Saniya Kalloufi    d001 Marketing    Manager           current manager of d001
--   10009 Sumant Peac        d005 Development  Engineer          second Development member

INSERT INTO employees.departments (dept_no, dept_name) VALUES
  ('d001', 'Marketing'),
  ('d003', 'Human Resources'),
  ('d005', 'Development'),
  ('d007', 'Sales');

INSERT INTO employees.employees (emp_no, birth_date, first_name, last_name, gender, hire_date) VALUES
  (10001, '1953-09-02', 'Georgi',    'Facello',   'M', '1986-06-26'),
  (10002, '1964-06-02', 'Bezalel',   'Simmel',    'F', '1985-11-21'),
  (10003, '1959-12-03', 'Parto',     'Bamford',   'M', '1986-08-28'),
  (10004, '1954-05-01', 'Chirstian', 'Koblick',   'M', '1986-12-01'),
  (10005, '1955-01-21', 'Kyoichi',   'Maliniak',  'M', '1989-09-12'),
  (10006, '1953-04-20', 'Anneke',    'Preusig',   'F', '1989-06-02'),
  (10007, '1957-05-23', 'Tzvetan',   'Zielinski', 'F', '1989-02-10'),
  (10008, '1958-02-19', 'Saniya',    'Kalloufi',  'M', '1994-09-15'),
  (10009, '1952-04-19', 'Sumant',    'Peac',      'F', '1985-02-18');

INSERT INTO employees.dept_emp (emp_no, dept_no, from_date, to_date) VALUES
  (10001, 'd005', '1986-06-26', '9999-01-01'),
  (10002, 'd005', '1985-11-21', '9999-01-01'),
  (10003, 'd007', '1986-08-28', '9999-01-01'),
  (10004, 'd003', '1986-12-01', '9999-01-01'),
  (10005, 'd005', '1989-09-12', '1995-01-01'),
  (10005, 'd007', '1995-01-01', '9999-01-01'),
  (10006, 'd005', '1989-06-02', '2000-01-01'),
  (10007, 'd001', '1989-02-10', '9999-01-01'),
  (10008, 'd001', '1994-09-15', '9999-01-01'),
  (10009, 'd005', '1985-02-18', '9999-01-01');

INSERT INTO employees.dept_manager (emp_no, dept_no, from_date, to_date) VALUES
  (10002, 'd005', '1991-01-01', '9999-01-01'),
  (10007, 'd001', '1990-01-01', '1995-01-01'),
  (10008, 'd001', '1995-01-01', '9999-01-01');

INSERT INTO employees.titles (emp_no, title, from_date, to_date) VALUES
  (10001, 'Engineer',        '1986-06-26', '1995-06-26'),
  (10001, 'Senior Engineer', '1995-06-26', '9999-01-01'),
  (10002, 'Senior Engineer', '1985-11-21', '1991-01-01'),
  (10002, 'Manager',         '1991-01-01', '9999-01-01'),
  (10003, 'Senior Staff',    '1986-08-28', '9999-01-01'),
  (10004, 'Senior Staff',    '1986-12-01', '9999-01-01'),
  (10005, 'Staff',           '1989-09-12', '9999-01-01'),
  (10006, 'Engineer',        '1989-06-02', '2000-01-01'),
  (10007, 'Senior Staff',    '1989-02-10', '9999-01-01'),
  (10008, 'Manager',         '1995-01-01', '9999-01-01'),
  (10009, 'Engineer',        '1985-02-18', '9999-01-01');

INSERT INTO employees.salaries (emp_no, salary, from_date, to_date) VALUES
  (10001, 60117, '1986-06-26', '1987-06-26'),
  (10001, 62102, '1987-06-26', '1988-06-25'),
  (10001, 88958, '1988-06-25', '9999-01-01'),
  (10002, 65828, '1985-11-21', '1991-01-01'),
  (10002, 72527, '1991-01-01', '9999-01-01'),
  (10003, 40006, '1986-08-28', '9999-01-01'),
  (10004, 40054, '1986-12-01', '9999-01-01'),
  (10005, 78228, '1989-09-12', '9999-01-01'),
  (10006, 40000, '1989-06-02', '2000-01-01'),
  (10007, 56724, '1989-02-10', '9999-01-01'),
  (10008, 46671, '1994-09-15', '9999-01-01'),
  (10009, 60929, '1985-02-18', '9999-01-01');
