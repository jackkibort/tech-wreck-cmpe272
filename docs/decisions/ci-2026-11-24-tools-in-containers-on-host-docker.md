# Pipeline tools run in pinned containers on the host Docker engine

- **Who:** Samhitha
- **Date:** 2026-11-24
- **What:**
  - Every pipeline tool runs in its own container with an exact image tag: gitleaks, Semgrep,
    Trivy, the JDK and Node.
  - Jenkins starts these containers on the host Docker engine through the mounted socket.
  - Areas that aren't on `main` yet are skipped by the pipeline rather than failed.
- **Why:**
  - **One place for versions:** a tool upgrade is a one-line change in the `Jenkinsfile`.
  - **Same results everywhere:** builds behave the same on every teammate's machine.
  - **Integration tests need it:** Testcontainers and image builds need a Docker engine.
  - **Skipping lets the pipeline gate early:** it could gate the Employee API from day one, and
    each other area starts gating as soon as its folder is merged.
- **Trade-off:** access to the socket is equivalent to root on the host. That's acceptable for a
  single-machine proof of concept (Section 11.4). A production setup would use separate build
  agents.
