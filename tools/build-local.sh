#!/data/data/com.termux/files/usr/bin/bash
# RAMESH AI - OPTIONAL local build attempt using Gradle.
# This requires a JDK and Android SDK command-line tools to be installed on
# device, which is heavy for Termux. The recommended workflow is
# tools/git-push.sh + GitHub Actions instead. This script exists for people
# who specifically want an on-device build and have already set up the SDK.

set -e
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

if [ -z "${ANDROID_SDK_ROOT:-}" ]; then
  echo "ANDROID_SDK_ROOT is not set. On-device builds need a working Android SDK."
  echo "This is advanced and not required -- GitHub Actions (tools/git-push.sh) builds the APK for you."
  exit 1
fi

if ! command -v gradle >/dev/null 2>&1; then
  echo "gradle not found. This repo ships without the gradle-wrapper.jar binary"
  echo "(generate it yourself with 'gradle wrapper' once you have Gradle installed,"
  echo "then commit gradlew/gradlew.bat + gradle/wrapper/gradle-wrapper.jar)."
  echo "For now, install Gradle directly (e.g. via sdkman) and re-run this script."
  exit 1
fi

gradle assembleDebug
echo "If successful, the APK is at app/build/outputs/apk/debug/"
