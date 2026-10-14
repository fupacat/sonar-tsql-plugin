#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 4 ]]; then
  echo "Usage: $0 <tag> <pom_version> <artifact_path> <repo_url>"
  exit 1
fi

TAG="$1"
POM_VERSION="$2"
ARTIFACT="$3"
REPO_URL="$4"
PLUGIN_CLASS='io/github/fupacat/sonar/tsql/TSqlPlugin.class'

if [[ ! "$TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Requested tag '$TAG' is not a valid release tag. Expected vX.Y.Z."
  exit 1
fi

if ! git ls-remote --exit-code --refs "$REPO_URL" "refs/tags/$TAG" > /dev/null; then
  echo "Requested release tag '$TAG' does not exist on origin."
  exit 1
fi

VERSION="${TAG#v}"
if [[ "$POM_VERSION" != "$VERSION" ]]; then
  echo "POM version '$POM_VERSION' does not match requested release tag '$TAG' (expected '$VERSION')."
  exit 1
fi

if [[ ! -f "$ARTIFACT" ]]; then
  echo "Expected packaged artifact '$ARTIFACT' was not produced."
  exit 1
fi

JAR_CONTENTS="$(jar tf "$ARTIFACT")"
if ! grep -Fxq "$PLUGIN_CLASS" <<< "$JAR_CONTENTS"; then
  echo "Packaged JAR does not contain the plugin main class."
  exit 1
fi
