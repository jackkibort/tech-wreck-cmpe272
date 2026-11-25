#!/bin/sh
# Gives the jenkins user access to the host Docker socket, then starts Jenkins as jenkins.
#
# The socket's group differs by host: "docker" (often gid 999) on Linux, root (gid 0) on Docker
# Desktop. Rather than hard-coding a gid, read it from the mounted socket and add jenkins to a
# group with that gid. Access to this socket is equivalent to root on the host; that trade-off
# is accepted for the single-machine proof of concept (Section 11.4, see /ci/README.md).
set -eu

SOCKET=/var/run/docker.sock

if [ -S "$SOCKET" ]; then
  gid=$(stat -c %g "$SOCKET")
  group=$(getent group "$gid" | cut -d: -f1 || true)
  if [ -z "$group" ]; then
    group=docker-host
    groupadd --gid "$gid" "$group"
  fi
  usermod -aG "$group" jenkins
fi

exec setpriv --reuid=jenkins --regid=jenkins --init-groups \
  env HOME=/var/jenkins_home /usr/bin/tini -- /usr/local/bin/jenkins.sh "$@"
