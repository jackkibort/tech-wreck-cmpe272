# Demo: vulnerable dependency (TC-PIPE-02, scenario S6)

This folder exists only on the `demo/vulnerable-dependency` branch, which is **never merged**.

It pins `lodash` 4.17.20, which has a known high-severity vulnerability (command injection,
CVE-2021-23337) fixed in 4.17.21. Jenkins stage 8 (Trivy dependency scan) must fail on this
branch every time, and a pull request from it can't merge into `main`.

**To demo it:** open a pull request from this branch. The Jenkins check goes red at
"8. Dependency scan", and the Trivy report under the build's artifacts names the library and
the fixed version.
