#!/data/data/com.termux/files/usr/bin/bash
# RAMESH AI - guided git add/commit/push from Termux.
# Runs run-check.sh first so secrets never get pushed by accident.

set -e
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

bash tools/run-check.sh

git status
echo
read -r -p "Commit message: " msg
if [ -z "$msg" ]; then
  echo "Commit message cannot be empty. Aborting."
  exit 1
fi

git add .
git commit -m "$msg"

current_branch=$(git rev-parse --abbrev-ref HEAD)
echo "Pushing branch '$current_branch'..."
git push origin "$current_branch"

echo
echo "Pushed. GitHub Actions will now build the APK automatically."
echo "Check progress under your repo's Actions tab, or download the artifact once it finishes."
