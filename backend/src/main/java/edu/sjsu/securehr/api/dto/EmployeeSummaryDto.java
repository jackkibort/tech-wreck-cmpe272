package edu.sjsu.securehr.api.dto;

/**
 * Directory fields only: what every portal role may see about anyone (Section 8.2).
 *
 * @param empNo employee number
 * @param firstName first name
 * @param lastName last name
 * @param deptNo current department number, or {@code null}
 * @param deptName current department name, or {@code null}
 * @param title current title, or {@code null}
 */
public record EmployeeSummaryDto(
    int empNo, String firstName, String lastName, String deptNo, String deptName, String title) {}
