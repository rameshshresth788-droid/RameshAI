#!/data/data/com.termux/files/usr/bin/bash
# RAMESH AI - sanity checks before pushing, so obvious mistakes are caught
# locally instead of burning a GitHub Actions run.

set -e
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "== Checking for accidentally committed secrets =="
if git ls-files | grep -E '\.env$|local\.properties$|assistant\.json$|\.keystore$|\.jks$'; then
  echo "!! Found a file that should never be committed. Remove it and add to .gitignore."
  exit 1
else
  echo "OK: no obviously sensitive files are tracked."
fi

echo "== Checking .gitignore covers secrets =="
for pattern in ".env" "local.properties" "config/assistant.json" "*.keystore" "*.jks"; do
  if ! grep -qF "$pattern" .gitignore; then
    echo "!! .gitignore is missing pattern: $pattern"
    exit 1
  fi
done
echo "OK: .gitignore looks correct."

echo "== Checking required project files exist =="
required_files=(
  "app/src/main/AndroidManifest.xml"
  "app/build.gradle.kts"
  "settings.gradle.kts"
  ".github/workflows/build-apk.yml"
)
for f in "${required_files[@]}"; do
  if [ ! -f "$f" ]; then
    echo "!! Missing required file: $f"
    exit 1
  fi
done
echo "OK: required project files are present."

echo
echo "All checks passed. Safe to push."
