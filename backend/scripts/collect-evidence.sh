#!/usr/bin/env sh
# Section 13.5 evidence for /backend: runs the full build and copies the HTML test reports and the
# coverage report into /docs/evidence/backend. Needs Docker (integration tests use Testcontainers).
# The terminal screenshot of the passing run is taken by hand and saved next to these reports.
set -eu
backend=$(cd "$(dirname "$0")/.." && pwd)
out="$backend/../docs/evidence/backend"
report_plugin=org.apache.maven.plugins:maven-surefire-report-plugin:3.5.6

cd "$backend"
./mvnw -B clean verify
./mvnw -B -q "$report_plugin:report-only" "$report_plugin:failsafe-report-only" -DlinkXRef=false

rm -rf "$out/unit-tests" "$out/integration-tests" "$out/coverage"
mkdir -p "$out/unit-tests" "$out/integration-tests"
cp target/reports/surefire.html "$out/unit-tests/index.html"
cp target/reports/failsafe.html "$out/integration-tests/index.html"
# The reports share one stylesheet and image folder; each copy gets its own.
for d in unit-tests integration-tests; do
  cp -r target/reports/css target/reports/images "$out/$d/" 2>/dev/null || true
done
cp -r target/site/jacoco "$out/coverage"
echo "Evidence written to docs/evidence/backend"
