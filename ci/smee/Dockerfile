# Webhook relay (Section 11.4): forwards GitHub webhooks from the public smee.io channel to the
# local Jenkins, which GitHub cannot reach directly.
FROM node:22.23.5-alpine3.24

# Pinned client version, installed at build time so the container never downloads code at start.
RUN npm install --global --omit=dev smee-client@5.0.0 && npm cache clean --force

USER node
ENTRYPOINT ["sh", "-c", "exec smee --url \"$GITHUB_WEBHOOK_PROXY_URL\" --target http://jenkins:8080/github-webhook/"]
