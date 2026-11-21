package edu.sjsu.securehr.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * Employee detail. {@code birthDate} and {@code currentSalary} are restricted fields: they are
 * {@code null} unless the policy allows them and then omitted from the JSON entirely, so masked
 * data never reaches the client (Section 8.2).
 *
 * @param empNo employee number
 * @param firstName first name
 * @param lastName last name
 * @param gender M or F
 * @param hireDate hire date
 * @param deptNo current department number, or {@code null}
 * @param deptName current department name, or {@code null}
 * @param title current title, or {@code null}
 * @param birthDate restricted: birth date
 * @param currentSalary restricted: current salary
 */
public record EmployeeDetailDto(
    int empNo,
    String firstName,
    String lastName,
    String gender,
    LocalDate hireDate,
    String deptNo,
    String deptName,
    String title,
    @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate birthDate,
    @JsonInclude(JsonInclude.Include.NON_NULL) Integer currentSalary) {}
