package edu.sjsu.securehr.api.service;

import java.util.List;

/**
 * Minimal RFC 4180 CSV writer with spreadsheet formula-injection protection: a cell starting with
 * {@code =}, {@code +}, {@code -} or {@code @} is prefixed with {@code '} so spreadsheet programs
 * treat it as text, never as a formula.
 */
final class CsvWriter {

  private static final String LINE_END = "\r\n";

  private final StringBuilder out = new StringBuilder();

  /**
   * Appends one row.
   *
   * @param cells cell values; {@code null} becomes an empty cell
   * @return this writer
   */
  CsvWriter row(List<?> cells) {
    for (int i = 0; i < cells.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      out.append(cell(cells.get(i)));
    }
    out.append(LINE_END);
    return this;
  }

  @Override
  public String toString() {
    return out.toString();
  }

  static String cell(Object value) {
    if (value == null) {
      return "";
    }
    String text = value.toString();
    if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
      text = "'" + text;
    }
    if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
      return '"' + text.replace("\"", "\"\"") + '"';
    }
    return text;
  }
}
