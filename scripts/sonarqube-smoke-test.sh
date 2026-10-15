#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLUGIN_PATH="${PLUGIN_PATH:-$(find "$ROOT_DIR/target" -maxdepth 1 -type f -name 'sonar-tsql-plugin-*.jar' | head -n 1)}"
SQ_VERSION="${SQ_VERSION:-9.9.1-community}"
CONTAINER_NAME="${CONTAINER_NAME:-sonarqube-tsql-smoke}"
SCANNER_IMAGE="${SCANNER_IMAGE:-sonarsource/sonar-scanner-cli:latest}"
FIXTURE_DIR="$ROOT_DIR/integration/smoke/fixture"
SONAR_LOGIN="${SONAR_LOGIN:-admin}"
SONAR_PASSWORD="${SONAR_PASSWORD:-admin}"

export SONAR_LOGIN SONAR_PASSWORD

if [[ -z "$PLUGIN_PATH" || ! -f "$PLUGIN_PATH" ]]; then
  echo "Plugin artifact not found in $ROOT_DIR/target. Run 'mvn -DskipTests package' first." >&2
  exit 1
fi

cleanup() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

for required in docker curl python3; do
  if ! command -v "$required" >/dev/null 2>&1; then
    echo "Required tool '$required' is not installed." >&2
    exit 1
  fi
done

wait_for_sonarqube() {
  local status_url="http://localhost:9000/api/system/status"
  for _ in $(seq 1 120); do
    if curl -fsS "$status_url" >/tmp/sonarqube-status.json 2>/dev/null; then
      local status
      status="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("status", ""))' </tmp/sonarqube-status.json)"
      if [[ "$status" == "UP" ]]; then
        return 0
      fi
    fi
    sleep 10
  done
  return 1
}

echo "==> Starting SonarQube ${SQ_VERSION}"
docker run -d --name "$CONTAINER_NAME" \
  -p 9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  "sonarqube:${SQ_VERSION}"

if ! wait_for_sonarqube; then
  echo "SonarQube did not become ready in time." >&2
  docker logs "$CONTAINER_NAME" >&2 || true
  exit 1
fi

echo "==> Installing plugin artifact"
docker cp "$PLUGIN_PATH" "$CONTAINER_NAME:/opt/sonarqube/extensions/plugins/sonar-tsql-plugin.jar"
docker restart "$CONTAINER_NAME" >/dev/null

if ! wait_for_sonarqube; then
  echo "SonarQube did not recover after plugin installation." >&2
  docker logs "$CONTAINER_NAME" >&2 || true
  exit 1
fi

echo "==> Running scanner against smoke fixture"
SCAN_LOG="$(mktemp)"
docker run --rm --network host \
  -v "$FIXTURE_DIR:/usr/src" \
  -w /usr/src \
  "$SCANNER_IMAGE" \
  -Dsonar.login="$SONAR_LOGIN" \
  -Dsonar.password="$SONAR_PASSWORD" >"$SCAN_LOG" 2>&1 || { cat "$SCAN_LOG"; exit 1; }

TASK_ID="$(grep -o 'api/ce/task?id=[A-Za-z0-9_-]*' "$SCAN_LOG" | sed 's#.*id=##' | tail -n 1 || true)"
if [[ -z "$TASK_ID" ]]; then
  echo "Could not determine SonarQube task id from scanner output." >&2
  cat "$SCAN_LOG" >&2
  exit 1
fi
export TASK_ID

python3 - <<'PY'
import base64
import json
import os
import time
import urllib.request

base = 'http://localhost:9000'
login = os.environ['SONAR_LOGIN']
password = os.environ['SONAR_PASSWORD']
task_id = os.environ['TASK_ID']


def fetch(path):
    token = base64.b64encode(f'{login}:{password}'.encode()).decode()
    request = urllib.request.Request(base + path, headers={'Authorization': f'Basic {token}'})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)

for _ in range(90):
    task = fetch(f'/api/ce/task?id={task_id}')
    status = task.get('task', {}).get('status')
    if status in {'SUCCESS', 'FAILED', 'CANCELED'}:
        if status != 'SUCCESS':
            raise SystemExit(f"SonarQube task ended in {status}: {task}")
        break
    time.sleep(2)
else:
    raise SystemExit(f"SonarQube task {task_id} did not finish in time.")

for _ in range(30):
    try:
        languages = fetch('/api/languages/list')['languages']
        if any(language.get('key') == 'tsql' for language in languages):
            break
    except Exception:
        time.sleep(2)
else:
    raise SystemExit('T-SQL language never registered in SonarQube')

languages = fetch('/api/languages/list')['languages']
assert any(language.get('key') == 'tsql' for language in languages), languages

rules = fetch('/api/rules/search?languages=tsql&ps=100')['rules']
assert rules, 'No T-SQL rules were registered'

profiles = fetch('/api/qualityprofiles/search?language=tsql')['profiles']
assert any(profile.get('name') == 'Sonar way' for profile in profiles), profiles

issues = []
for _ in range(60):
    try:
        issues = fetch('/api/issues/search?componentKeys=tsql-smoke&ps=100&resolved=false')['issues']
        if issues:
            break
    except Exception:
        pass
    time.sleep(2)

assert issues, 'No issues were reported for the smoke fixture; scanner did not exercise the plugin.'
assert any(issue.get('rule') == 'tsql:AvoidSelectAll' for issue in issues), issues
assert any(issue.get('rule') == 'tsql:SyntaxError' for issue in issues), issues
assert any(issue.get('component', '').endswith('ddl/001_schema.sql') for issue in issues), issues
assert any(issue.get('component', '').endswith('routines/010_get_accounts.sql') for issue in issues), issues
assert all(issue.get('line') is not None for issue in issues), issues

print(f"Validated language registration, rule registration, quality profile, and {len(issues)} issues for tsql-smoke.")
PY

rm -f "$SCAN_LOG"

echo "==> Smoke test passed"
