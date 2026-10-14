#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLUGIN_PATH="${PLUGIN_PATH:-$(find "$ROOT_DIR/target" -maxdepth 1 -type f -name 'sonar-tsql-plugin-*.jar' | head -n 1)}"
SQ_VERSION="${SQ_VERSION:-9.9.1}"
CONTAINER_NAME="${CONTAINER_NAME:-sonarqube-tsql-smoke}"
SCANNER_IMAGE="${SCANNER_IMAGE:-sonarsource/sonar-scanner-cli:latest}"
FIXTURE_DIR="$ROOT_DIR/integration/smoke/fixture"
SONAR_LOGIN="${SONAR_LOGIN:-admin}"
SONAR_PASSWORD="${SONAR_PASSWORD:-admin}"

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

echo "==> Starting SonarQube ${SQ_VERSION}"
docker run -d --name "$CONTAINER_NAME" \
  -p 9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  "sonarqube:${SQ_VERSION}-community"

for _ in $(seq 1 120); do
  if curl -fsS "http://localhost:9000/api/system/status" >/tmp/sonarqube-status.json 2>/dev/null; then
    STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("status", ""))' </tmp/sonarqube-status.json)"
    if [[ "$STATUS" == "UP" ]]; then
      break
    fi
  fi
  sleep 10
done

if ! curl -fsS "http://localhost:9000/api/system/status" >/tmp/sonarqube-status.json 2>/dev/null; then
  echo "SonarQube did not become ready in time." >&2
  docker logs "$CONTAINER_NAME" >&2 || true
  exit 1
fi

STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("status", ""))' </tmp/sonarqube-status.json)"
if [[ "$STATUS" != "UP" ]]; then
  echo "SonarQube status is '$STATUS', expected 'UP'." >&2
  docker logs "$CONTAINER_NAME" >&2 || true
  exit 1
fi

echo "==> Installing plugin artifact"
docker cp "$PLUGIN_PATH" "$CONTAINER_NAME:/opt/sonarqube/extensions/plugins/sonar-tsql-plugin.jar"
docker restart "$CONTAINER_NAME" >/dev/null

for _ in $(seq 1 120); do
  if curl -fsS "http://localhost:9000/api/system/status" >/tmp/sonarqube-status.json 2>/dev/null; then
    STATUS="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("status", ""))' </tmp/sonarqube-status.json)"
    if [[ "$STATUS" == "UP" ]]; then
      break
    fi
  fi
  sleep 10
done

if [[ "$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("status", ""))' </tmp/sonarqube-status.json)" != "UP" ]]; then
  echo "SonarQube did not recover after plugin installation." >&2
  docker logs "$CONTAINER_NAME" >&2 || true
  exit 1
fi

cat >"$FIXTURE_DIR/sonar-project.properties" <<EOF
sonar.projectKey=tsql-smoke
sonar.projectName=T-SQL smoke
sonar.projectVersion=1.0
sonar.host.url=http://localhost:9000
sonar.sources=ddl,routines
sonar.sourceEncoding=UTF-8
sonar.qualitygate.wait=false
