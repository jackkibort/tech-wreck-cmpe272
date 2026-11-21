package edu.sjsu.securehr.api.controller;

import edu.sjsu.securehr.api.dto.EmployeeSearchCriteria;
import edu.sjsu.securehr.api.security.CurrentUser;
import edu.sjsu.securehr.api.service.EmployeeExport;
import edu.sjsu.securehr.api.service.ExportService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/export/employees}: CSV of the current search filters, HR Admin only. */
@RestController
public class ExportController {

  /** Tells the caller that more rows matched than the export cap allows. */
  static final String TRUNCATED_HEADER = "X-Export-Truncated";

  private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

  private final ExportService exportService;

  /**
   * Creates the controller.
   *
   * @param exportService export use case
   */
  public ExportController(ExportService exportService) {
    this.exportService = exportService;
  }

  @GetMapping("/api/export/employees")
  ResponseEntity<String> export(CurrentUser user, @Valid EmployeeSearchCriteria criteria) {
    EmployeeExport export = exportService.export(user, criteria);
    return ResponseEntity.ok()
        .contentType(TEXT_CSV)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("employees-export.csv").build().toString())
        .header(TRUNCATED_HEADER, Boolean.toString(export.truncated()))
        .body(export.csv());
  }
}
