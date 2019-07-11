#!/usr/bin/env bash

# Ref: https://developer.android.com/training/monitoring-device-state/doze-standby#testing_doze

echo "Enable doze mode"
echo "adb shell dumpsys deviceidle force-idle"
adb shell dumpsys deviceidle force-idle
