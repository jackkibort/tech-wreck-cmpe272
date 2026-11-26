# SecureHR edge (Section 8.4): NGINX + ModSecurity v3 + OWASP Core Rule Set, from the official
# OWASP CRS image. Only configuration is added; no packages are installed.
FROM owasp/modsecurity-crs:4.30.0-nginx-alpine-202611190211

# The edge listens on 443/80 inside the container as well as on the host, because the APIs reach
# Keycloak at https://auth.securehr.local through the edge on the internal network (Section 0.3).
# The image refuses ports below 1024 for its unprivileged user, so that check is removed; the
# Compose file lowers the unprivileged port floor for this container only (see compose.yaml).
USER root
RUN rm /docker-entrypoint.d/01-check-low-port.sh
USER nginx

ENV PORT=80 \
    SSL_PORT=443 \
    # Blocking mode, paranoia level 1 (Section 8.4).
    MODSEC_RULE_ENGINE=on \
    BLOCKING_PARANOIA=1 \
    DETECTION_PARANOIA=1 \
    # The request-body limit sits just above the 10 MB upload cap so NGINX, not ModSecurity,
    # rejects oversized uploads with a clear 413.
    MODSEC_REQ_BODY_LIMIT=10600000 \
    # Firewall audit log as JSON on stdout, collected by Alloy (Section 0.10).
    MODSEC_AUDIT_LOG=/dev/stdout \
    MODSEC_AUDIT_LOG_FORMAT=JSON \
    ERRORLOG=/dev/stderr \
    LOGLEVEL=warn

# Our server blocks and JSON log format replace the image's single-backend defaults. The image's
# modsecurity.conf template (modsecurity on; rules from /etc/modsecurity.d/setup.conf) is kept.
COPY --chown=nginx:nginx templates/conf.d/default.conf.template /etc/nginx/templates/conf.d/default.conf.template
COPY --chown=nginx:nginx templates/conf.d/logging.conf.template /etc/nginx/templates/conf.d/logging.conf.template
COPY --chown=nginx:nginx templates/includes/ /etc/nginx/templates/includes/

# Documented rule exceptions (Section 8.4), loaded by the CRS rules/*.conf include.
COPY --chown=nginx:nginx modsecurity/REQUEST-900-EXCLUSION-RULES-BEFORE-CRS.conf /opt/owasp-crs/rules/REQUEST-900-EXCLUSION-RULES-BEFORE-CRS.conf
