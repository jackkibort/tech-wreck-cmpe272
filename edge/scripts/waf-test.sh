#!/usr/bin/env bash
# TC-SEC-01: sends every payload in edge/waf-tests/payloads.tsv to the edge and checks that the
# firewall answers 403. Also checks that one normal search is NOT blocked (no false positive).
#
# Usage: edge/scripts/waf-test.sh [base-url]     (default https://securehr.local)
# Exit code 0 only when every payload is blocked and the normal request passes the firewall.
set -euo pipefail

BASE_URL="${1:-https://securehr.local}"
PAYLOADS="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/waf-tests/payloads.tsv"
failures=0

while IFS=$'\t' read -r id method path; do
  [[ -z "${id}" || "${id}" == \#* ]] && continue
  status="$(curl -s -o /dev/null -w '%{http_code}' -X "${method}" -H "X-Request-ID: tc-sec-01-${id}" "${BASE_URL}${path}")"
  if [[ "${status}" == "403" ]]; then
    printf 'PASS  %-8s blocked (403)\n' "${id}"
  else
    printf 'FAIL  %-8s got %s, expected 403\n' "${id}" "${status}"
    failures=$((failures + 1))
  fi
done < "${PAYLOADS}"

# Without a token the API answers 401, which proves the request reached it past the firewall.
status="$(curl -s -o /dev/null -w '%{http_code}' "${BASE_URL}/api/employees?q=Bridgland&deptNo=d005")"
if [[ "${status}" == "403" ]]; then
  echo "FAIL  NORMAL   a normal search was blocked (false positive)"
  failures=$((failures + 1))
else
  printf 'PASS  %-8s not blocked (%s)\n' "NORMAL" "${status}"
fi

echo "${failures} failure(s)"
[[ "${failures}" -eq 0 ]]
