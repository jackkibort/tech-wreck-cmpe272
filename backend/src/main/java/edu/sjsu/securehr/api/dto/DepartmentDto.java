package edu.sjsu.securehr.api.dto;

/**
 * A department and its current manager.
 *
 * @param deptNo department number
 * @param deptName department name
 * @param manager current manager, or {@code null} if the department has none
 */
public record DepartmentDto(String deptNo, String deptName, EmployeeRefDto manager) {}
