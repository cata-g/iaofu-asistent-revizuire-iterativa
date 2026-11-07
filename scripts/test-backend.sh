#!/usr/bin/env sh
# Runs the backend tests in Docker, so no local JDK is needed.
# Usage: sh scripts/test-backend.sh [extra maven args, e.g. -Dtest=VersionClassifierTest]
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$ROOT/backend:/src" \
  -v "$ROOT/problems:/problems:ro" \
  -v "$ROOT/fixtures:/fixtures:ro" \
  -v iaofu-maven-cache:/root/.m2 \
  -w /src \
  maven:3.9-eclipse-temurin-21 \
  mvn -q -B test -DtrimStackTrace=true "$@"
