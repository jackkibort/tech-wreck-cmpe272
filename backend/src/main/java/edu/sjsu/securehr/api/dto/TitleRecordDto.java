package edu.sjsu.securehr.api.dto;

import java.time.LocalDate;

/**
 * One title held by an employee.
 *
 * @param title title
 * @param fromDate start date
 * @param toDate end date, or {@code null} if current
 */
public record TitleRecordDto(String title, LocalDate fromDate, LocalDate toDate) {}
