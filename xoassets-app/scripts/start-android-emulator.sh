#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
EMULATOR="$SDK_ROOT/emulator/emulator"
ADB="$SDK_ROOT/platform-tools/adb"
AVD_NAME="${1:-xoassets_api36}"

if "$ADB" devices | grep -q "emulator-.*device"; then
  DEVICE_ID="$($ADB devices | awk '/emulator-.*device/{print $1; exit}')"
else
  "$EMULATOR" -avd "$AVD_NAME" >/tmp/xoassets-emulator.log 2>&1 &
  DEVICE_ID=""
fi

for i in $(seq 1 90); do
  if [ -z "$DEVICE_ID" ]; then
    DEVICE_ID="$($ADB devices | awk '/emulator-.*device/{print $1; exit}')"
  fi

  if [ -n "$DEVICE_ID" ] && [ "$("$ADB" -s "$DEVICE_ID" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
    exit 0
  fi

  sleep 2
done

cat /tmp/xoassets-emulator.log 2>/dev/null || true
exit 1
