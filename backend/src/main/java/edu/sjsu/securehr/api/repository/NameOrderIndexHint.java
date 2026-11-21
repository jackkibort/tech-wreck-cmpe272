package edu.sjsu.securehr.api.repository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Tells MySQL to read directory pages in name order from the name index.
 *
 * <p>Without the hint MySQL switches to sorting every employee once the page offset passes about
 * 1,000 rows (2 s per request on the full dataset); with it, deep pages take milliseconds and the
 * other filters are unaffected. Measurements are in the backend README.
 *
 * <p>JPA has no way to express a MySQL index hint, and Hibernate's own query-hint support only
 * produces {@code USE INDEX}, which would also stop MySQL from using the first-name and hire-date
 * indexes for filtering. This inspector therefore edits only statements with the exact shape of a
 * directory page: a select from {@code employees} ordered by last name, first name and number with
 * a limit. Everything else passes through unchanged. Registered in {@code application.yml}.
 */
public class NameOrderIndexHint implements StatementInspector {

  static final String HINT = "FORCE INDEX FOR ORDER BY (idx_employees_last_first)";

  private static final Pattern DIRECTORY_PAGE =
      Pattern.compile(
          "^(select .+? from employees (\\w+))"
              + "( where .+ order by \\2\\.last_name,\\2\\.first_name,\\2\\.emp_no limit .+)$",
          Pattern.DOTALL);

  @Override
  public String inspect(String sql) {
    Matcher page = DIRECTORY_PAGE.matcher(sql);
    return page.matches() ? page.group(1) + " " + HINT + page.group(3) : sql;
  }
}
