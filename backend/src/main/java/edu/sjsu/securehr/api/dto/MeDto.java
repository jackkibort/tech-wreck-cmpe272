package edu.sjsu.securehr.api.dto;

import java.util.List;

/**
 * The signed-in caller.
 *
 * @param username Keycloak username
 * @param empNo linked employee number, or {@code null}
 * @param roles SecureHR roles from the token
 * @param employee the caller's own directory record, or {@code null}
 */
public record MeDto(
    String username, Integer empNo, List<String> roles, EmployeeSummaryDto employee) {

  /** Defensive copy of the role list. */
  public MeDto {
    roles = List.copyOf(roles);
  }
}
