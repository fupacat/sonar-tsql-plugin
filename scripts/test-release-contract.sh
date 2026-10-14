#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VALIDATOR="$ROOT_DIR/scripts/validate-release-contract.sh"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

REMOTE_REPO="$TMP_DIR/remote.git"
WORK_REPO="$TMP_DIR/work"
ARTIFACT_DIR="$TMP_DIR/artifact"
ARTIFACT="$ARTIFACT_DIR/sonar-tsql-plugin-1.2.3.jar"
INVALID_ARTIFACT="$TMP_DIR/missing-plugin-class.jar"

git init --bare "$REMOTE_REPO" >/dev/null
git init "$WORK_REPO" >/dev/null
(
  cd "$WORK_REPO"
  git config user.name "ci"
  git config user.email "ci@example.com"
  echo "release validation" > README.txt
  git add README.txt
  git commit -m "seed" >/dev/null
  git tag v1.2.3
  git remote add origin "$REMOTE_REPO"
  git push origin HEAD --tags >/dev/null
)

mkdir -p "$ARTIFACT_DIR/io/github/fupacat/sonar/tsql"
printf 'placeholder' > "$ARTIFACT_DIR/io/github/fupacat/sonar/tsql/TSqlPlugin.class"
(
  cd "$ARTIFACT_DIR"
  jar cf "$ARTIFACT" io >/dev/null
  jar cf "$INVALID_ARTIFACT" -C "$WORK_REPO" README.txt >/dev/null
)

run_failure_case() {
  local description="$1"
  shift
  if "$VALIDATOR" "$@" >/dev/null 2>&1; then
    echo "Expected failure: $description"
    exit 1
  fi
}

"$VALIDATOR" "v1.2.3" "1.2.3" "$ARTIFACT" "$REMOTE_REPO"
run_failure_case "mismatched pom version" "v1.2.3" "1.2.4" "$ARTIFACT" "$REMOTE_REPO"
run_failure_case "invalid tag format" "release-1.2.3" "1.2.3" "$ARTIFACT" "$REMOTE_REPO"
run_failure_case "missing tag ref" "v9.9.9" "9.9.9" "$ARTIFACT" "$REMOTE_REPO"
run_failure_case "missing artifact" "v1.2.3" "1.2.3" "$TMP_DIR/missing.jar" "$REMOTE_REPO"
run_failure_case "missing plugin class" "v1.2.3" "1.2.3" "$INVALID_ARTIFACT" "$REMOTE_REPO"

echo "Release contract validation scenarios passed."
