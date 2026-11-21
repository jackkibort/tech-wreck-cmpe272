package edu.sjsu.securehr.api.audit;

/**
 * Fixed audit event types shared by every producer (Section 0.10). This API emits DATA_READ,
 * SALARY_READ, EXPORT and ACCESS_DENIED; the others belong to Keycloak, docs-api and the edge.
 */
public enum AuditEventType {
  LOGIN_SUCCESS,
  LOGIN_FAILURE,
  DATA_READ,
  SALARY_READ,
  EXPORT,
  ACCESS_DENIED,
  DOC_UPLOAD,
  DOC_DOWNLOAD,
  DOC_DELETE,
  WAF_BLOCK,
  RATE_LIMITED
}
