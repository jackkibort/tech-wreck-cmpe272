# Jenkins controller for SecureHR (Section 11.4): plugins and configuration as code baked in.

# The Docker CLI and its buildx and compose plugins, copied from the official image rather than installed
# from a package repository, so the version is pinned in one place.
FROM docker:29.10.1-cli AS docker-cli

FROM jenkins/jenkins:2.580.2-lts-jdk21

USER root
COPY --from=docker-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker-cli /usr/local/libexec/docker/cli-plugins/docker-buildx /usr/local/libexec/docker/cli-plugins/docker-buildx
# Compose, for the staging deploy and production promotion (stages 11 and 14).
COPY --from=docker-cli /usr/local/libexec/docker/cli-plugins/docker-compose /usr/local/libexec/docker/cli-plugins/docker-compose
COPY --chmod=0755 docker-socket-entrypoint.sh /usr/local/bin/docker-socket-entrypoint.sh

USER jenkins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

COPY casc/ /usr/share/jenkins/casc/
ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/casc/ \
    JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

# Starts as root only to grant the jenkins user access to the host Docker socket, then drops
# to jenkins before Jenkins itself starts (see docker-socket-entrypoint.sh). Jenkins never runs
# as root, so the Semgrep rule for a root final user is suppressed here on purpose.
# nosemgrep: dockerfile.security.last-user-is-root.last-user-is-root
USER root
ENTRYPOINT ["/usr/local/bin/docker-socket-entrypoint.sh"]
