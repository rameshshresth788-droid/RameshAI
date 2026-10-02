#!/data/data/com.termux/files/usr/bin/bash
# RAMESH AI - one-time Termux environment setup.
# Installs git + curl + jq, and (optionally) OpenJDK if you ever want to try a
# local Gradle build on-device. The normal workflow relies on GitHub Actions
# for the actual APK build, so a full Android SDK on-device is NOT required.

set -e

echo "Updating Termux packages..."
pkg update -y

echo "Installing git, curl, jq..."
pkg install -y git curl jq

echo
echo "Setup complete."
echo "Next steps:"
echo "  1. git clone <your-repo-url>"
echo "  2. cd RameshAI"
echo "  3. bash tools/config.sh      # configure AI provider/API key (after installing the APK)"
echo "  4. bash tools/git-push.sh    # push code changes -> GitHub Actions builds the APK"
