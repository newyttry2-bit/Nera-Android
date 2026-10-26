#!/bin/sh
# Minimal Gradle wrapper entry point for this scaffold.
# If a real gradle-wrapper.jar is present it is used; otherwise this script
# downloads the distribution named in gradle/wrapper/gradle-wrapper.properties
# (gradle-8.7) once and runs it. On GitHub Actions, `gradle` may already be
# on PATH — in that case it is used directly.
# See: https://gradle.org/install/ and the project's README for the
# recommended step of replacing this file with the official wrapper generated
# by `gradle wrapper` in Android Studio.
set -e
cd "$(dirname "$0")"
if [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
fi
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
GRADLE_VERSION="8.7"
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$(dirname "$GRADLE_HOME")"
  TMP_ZIP="$(mktemp).zip"
  curl -L -o "$TMP_ZIP" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  unzip -q "$TMP_ZIP" -d "$(dirname "$GRADLE_HOME")"
  rm -f "$TMP_ZIP"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
