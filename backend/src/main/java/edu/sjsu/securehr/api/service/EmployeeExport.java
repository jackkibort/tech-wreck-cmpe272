package edu.sjsu.securehr.api.service;

/**
 * A finished CSV export.
 *
 * @param csv the file contents, header row first
 * @param rows number of data rows
 * @param truncated whether more rows matched than the export cap allows
 */
public record EmployeeExport(String csv, int rows, boolean truncated) {}
