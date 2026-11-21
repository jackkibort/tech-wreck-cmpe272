package edu.sjsu.securehr.api.dto;

import java.time.LocalDate;

/**
 * One salary period.
 *
 * @param salary annual salary
 * @param fromDate start date
 * @param toDate end date, or {@code null} if current
 */
public record SalaryRecordDto(int salary, LocalDate fromDate, LocalDate toDate) {}
