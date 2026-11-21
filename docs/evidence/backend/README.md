# Evidence: /backend (Employee API, Sabari)

Section 13.5 evidence. Regenerate with `backend/scripts/collect-evidence.sh`.

| Item | Where | Result (Nov 20, 2026) |
|---|---|---|
| Unit test report (HTML) | [`unit-tests/index.html`](unit-tests/index.html) | 26 tests, 100% passed |
| Integration test report (HTML), including TC-API-01 to TC-API-10 | [`integration-tests/index.html`](integration-tests/index.html) | 62 tests, 100% passed |
| Coverage report | [`coverage/index.html`](coverage/index.html) | 95% overall; `service` 98%, `security` 95%, `security.policy` 100% (gate: 70%) |
| Screenshot of the passing Maven run | `maven-verify.png` | To add: screenshot of `./mvnw verify` ending in `BUILD SUCCESS` |

Each test's name starts with its test case ID where one applies (for example
`tcApi04_employeeReadsAnotherSalary_isForbiddenAndAudited`), so results can be traced to
Section 13.2.
