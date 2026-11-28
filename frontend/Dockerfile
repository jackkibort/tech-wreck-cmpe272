# securehr/ui (Section 0.11): the React build served by an unprivileged NGINX on port 8080.

# ---- Build stage: install exact dependencies, run the production build ----
FROM node:22.23.5-alpine3.24 AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# ---- Runtime stage: static files only, no Node.js ----
FROM nginxinc/nginx-unprivileged:1.31.7-alpine3.24

COPY docker/log-format.conf /etc/nginx/conf.d/00-log-format.conf
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY docker/40-write-config.sh /docker-entrypoint.d/40-write-config.sh
COPY --from=build /app/dist /usr/share/nginx/html

# config.json is written at start-up into a writable folder and linked into the web root, so the
# web root itself stays read-only (Compose mounts /tmp as tmpfs).
USER root
RUN rm -f /usr/share/nginx/html/config.json \
    && ln -s /tmp/securehr-config/config.json /usr/share/nginx/html/config.json
USER nginx

EXPOSE 8080
