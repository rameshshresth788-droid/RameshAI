#!/data/data/com.termux/files/usr/bin/bash
# RAMESH AI - runtime configuration tool
#
# Talks to the app's loopback-only config server (127.0.0.1:8765), which is
# only reachable from processes on THIS phone. No PC, no ADB required.
# The app must be running (foreground or background service) for this to work.
#
# Changing anything here takes effect immediately -- NO APK rebuild needed.

set -euo pipefail
BASE_URL="http://127.0.0.1:8765"

require_curl() {
  if ! command -v curl >/dev/null 2>&1; then
    echo "curl not found. Install it with: pkg install curl"
    exit 1
  fi
}

require_jq() {
  if ! command -v jq >/dev/null 2>&1; then
    echo "jq not found (used to pretty-print JSON). Install it with: pkg install jq"
    return 1
  fi
  return 0
}

check_reachable() {
  if ! curl -s -m 3 "$BASE_URL/health" >/dev/null; then
    echo "Could not reach RAMESH AI on this phone."
    echo "Make sure the app is open (or its background service is running) and try again."
    exit 1
  fi
}

show_config() {
  check_reachable
  if require_jq >/dev/null 2>&1; then
    curl -s "$BASE_URL/config" | jq .
  else
    curl -s "$BASE_URL/config"
    echo
  fi
}

set_field() {
  local field="$1"
  local value="$2"
  check_reachable
  # Minimal JSON string-escaping for embedded quotes/backslashes.
  local escaped
  escaped=$(printf '%s' "$value" | sed 's/\\/\\\\/g; s/"/\\"/g')
  curl -s -X POST "$BASE_URL/config" \
    -H "Content-Type: application/json" \
    -d "{\"$field\": \"$escaped\"}" >/dev/null
  echo "Updated $field."
}

read_secret() {
  local prompt="$1"
  local value
  read -r -s -p "$prompt: " value
  echo
  echo "$value"
}

menu() {
  cat <<'EOF'

RAMESH AI - Configuration
==========================
 1) AI Provider (openai | gemini | openrouter | local)
 2) API Base URL
 3) API Key
 4) Model
 5) Assistant Name
 6) Wake Phrase
 7) Language (e.g. hi-IN, en-US)
 8) System Prompt
 9) News Provider (none | newsapi)
10) Show Configuration
11) Test AI (health check only)
12) Exit
EOF
  read -r -p "Choose an option: " choice
  case "$choice" in
    1) read -r -p "Provider [openai/gemini/openrouter/local]: " v; set_field "aiProvider" "$v" ;;
    2) read -r -p "Base URL: " v; set_field "aiBaseUrl" "$v" ;;
    3) v=$(read_secret "API Key (input hidden)"); set_field "aiApiKey" "$v" ;;
    4) read -r -p "Model: " v; set_field "aiModel" "$v" ;;
    5) read -r -p "Assistant name: " v; set_field "assistantName" "$v" ;;
    6) read -r -p "Wake phrase: " v; set_field "wakePhrase" "$v" ;;
    7) read -r -p "Language tag: " v; set_field "language" "$v" ;;
    8) echo "Enter system prompt (single line):"; read -r v; set_field "systemPrompt" "$v" ;;
    9) read -r -p "News provider [none/newsapi]: " v; set_field "newsProvider" "$v" ;;
    10) show_config ;;
    11) check_reachable; echo "RAMESH AI is reachable and responding." ;;
    12) exit 0 ;;
    *) echo "Unknown option." ;;
  esac
}

require_curl

if [ "${1:-}" = "show" ]; then
  show_config
  exit 0
fi

while true; do
  menu
done
