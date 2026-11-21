# Two Spring Web MVC advisories accepted until Jan 31, 2027

- **Who:** Sabari
- **Date:** 2026-11-20
- **What:** Trivy reports CVE-2026-47884 and CVE-2026-47890 in `spring-webmvc` 6.2.19. Both are
  listed in `backend/.trivyignore.yaml` with a reason and an expiry date.
- **Why:**
  - Both are fixed only in Spring Framework 7, which needs Spring Boot 4. Section 0.1 pins
    Spring Boot 3.x, and 3.5.16 (Spring Framework 6.2.19) is the latest open-source 3.x release.
  - Neither applies to this API. CVE-2026-47884 needs XSLT view rendering, and CVE-2026-47890
    needs Server-Sent Events with view fragments. The API has only `@RestController` endpoints
    that return JSON or CSV, with no views and no SSE.
  - All other findings were fixed: Tomcat 10.1.62 and Jackson 2.21.8 overrides, Spring Cloud
    2025.0.3, and dropping the unused Apache HttpComponents and Bouncy Castle.
- **Revisit:** By the expiry date. The team should decide whether to move both Java APIs to
  Spring Boot 4.
