#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
EMULATOR="$SDK_ROOT/emulator/emulator"
ADB="$SDK_ROOT/platform-tools/adb"
AVD_NAME="${1:-xoassets_api36}"

"$EMULATOR" -avd "$AVD_NAME" >/tmp/xoassets-emulator.log 2>&1 &

for i in $(seq 1 60); do
  if "$ADB" devices | grep -q "emulator-"; then
    exit 0
  fi
  sleep 2
done

cat /tmp/xoassets-emulator.log
exit 1
