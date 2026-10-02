#!/data/data/com.termux/files/usr/bin/bash
# RAMESH AI - back up the project source tree (never secrets) to a timestamped
# tarball. Refuses to overwrite an existing backup file, and never touches
# anything outside the repo.

set -e
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

timestamp=$(date +%Y%m%d-%H%M%S)
out="../rameshai-backup-$timestamp.tar.gz"

if [ -f "$out" ]; then
  echo "Backup file already exists: $out"
  read -r -p "Overwrite? [y/N]: " confirm
  if [ "$confirm" != "y" ]; then
    echo "Aborted."
    exit 0
  fi
fi

tar --exclude='.git' --exclude='build' --exclude='.gradle' \
    --exclude='config/assistant.json' --exclude='local.properties' --exclude='.env' \
    -czf "$out" .

echo "Backup written to $out"
