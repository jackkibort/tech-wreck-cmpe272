package edu.sjsu.securehr.api.dto;

/**
 * A reference to an employee by name, such as a department's manager.
 *
 * @param empNo employee number
 * @param firstName first name
 * @param lastName last name
 */
public record EmployeeRefDto(int empNo, String firstName, String lastName) {}
