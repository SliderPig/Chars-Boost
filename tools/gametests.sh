#!/usr/bin/env bash
# The headless GameTests, then a clean build without them. Run through heavy.sh.
cd "$(dirname "$0")/.."
LOG="${1:-gametest.log}"
timeout 400 ./gradlew --no-daemon --console=plain --max-workers=3 -Dorg.gradle.jvmargs=-Xmx2200M runGameTestServer -Psmoke > "$LOG" 2>&1
echo "gametests exit $?"
./gradlew --no-daemon --console=plain --max-workers=3 -Dorg.gradle.jvmargs=-Xmx2200M test build > "$LOG.build" 2>&1
echo "build exit $?"
