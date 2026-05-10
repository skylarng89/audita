#!/bin/bash
# Runs SonarQube analysis for both audita-api and audita-web.
# Requires: sonar-scanner CLI on PATH, Java + Gradle for the API.

set -euo pipefail

SONAR_HOST="http://localhost:7040"
SONAR_TOKEN="sqa_4abc72195d5e9ea5aa5b311329edae9d575d951a"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> [1/2] Scanning audita-api..."
(
  cd "$SCRIPT_DIR/audita-api"
  ./gradlew sonar \
    -Dsonar.projectKey=audita-api \
    -Dsonar.projectName='audita-api' \
    -Dsonar.host.url="$SONAR_HOST" \
    -Dsonar.token="$SONAR_TOKEN" \
    -Dsonar.scm.provider=git
)

echo ""
echo "==> [2/2] Scanning audita-web..."
(
  cd "$SCRIPT_DIR/audita-web"

  # The SonarQube project requires a dependency-check JSON report path.
  # We generate a minimal valid empty report so the plugin skips gracefully
  # when dependency-check CLI is not installed locally.
  DC_REPORT="$SCRIPT_DIR/audita-web/dependency-check-report.json"
  if [ ! -f "$DC_REPORT" ]; then
    echo '{"dependencies":[]}' > "$DC_REPORT"
  fi

  sonar \
    -Dsonar.projectKey=audita-web \
    -Dsonar.projectName='audita-web' \
    -Dsonar.host.url="$SONAR_HOST" \
    -Dsonar.token="$SONAR_TOKEN" \
    -Dsonar.scm.provider=git \
    -Dsonar.dependencyCheck.jsonReportPath="$DC_REPORT"
)

echo ""
echo "==> All scans complete. Results at $SONAR_HOST"
