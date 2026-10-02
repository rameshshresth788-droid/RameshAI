#!/data/data/com.termux/files/usr/bin/bash
# RAMESH AI - safe log viewing helper.
# Uses adb logcat if adb is available and a device/emulator is attached
# (e.g. wireless debugging to the same phone). Never prints API keys --
# the app itself never logs them (see ConfigRepository/PhoneActionTools).

set -e

if command -v adb >/dev/null 2>&1; then
  echo "Streaming logs for package com.rameshai (Ctrl+C to stop)..."
  adb logcat --pid="$(adb shell pidof -s com.rameshai || true)" 2>/dev/null || \
    adb logcat | grep -i "rameshai"
else
  echo "adb not found in Termux."
  echo "Install Android platform-tools, or enable Wireless debugging and connect,"
  echo "or view logs via Android's built-in 'Developer options > System logs' if your ROM supports it."
fi
