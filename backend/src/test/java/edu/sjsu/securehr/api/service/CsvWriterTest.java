package edu.sjsu.securehr.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CsvWriterTest {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '"',
      value = {"=SUM(A1:A9)|'=SUM(A1:A9)", "+1|'+1", "-1|'-1", "@cmd|'@cmd", "Georgi|Georgi"})
  void formulaLikeCellsAreNeutralized(String input, String expected) {
    assertThat(CsvWriter.cell(input)).isEqualTo(expected);
  }

  @Test
  void separatorsAndQuotesAreQuoted() {
    assertThat(CsvWriter.cell("a,b")).isEqualTo("\"a,b\"");
    assertThat(CsvWriter.cell("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    assertThat(CsvWriter.cell("line\nbreak")).isEqualTo("\"line\nbreak\"");
    assertThat(CsvWriter.cell("=1,2")).isEqualTo("\"'=1,2\"");
  }

  @Test
  void nullAndEmptyBecomeEmptyCells() {
    assertThat(CsvWriter.cell(null)).isEmpty();
    assertThat(CsvWriter.cell("")).isEmpty();
  }

  @Test
  void rowsUseCrlf() {
    String csv = new CsvWriter().row(List.of("a", 1)).row(Arrays.asList("b", null)).toString();
    assertThat(csv).isEqualTo("a,1\r\nb,\r\n");
  }
}
