package edu.sjsu.securehr.api.dto;

/**
 * One person in a management chain, with directory fields only.
 *
 * @param empNo employee number
 * @param firstName first name
 * @param lastName last name
 * @param title current title, or {@code null}
 */
public record OrgNodeDto(int empNo, String firstName, String lastName, String title) {}
