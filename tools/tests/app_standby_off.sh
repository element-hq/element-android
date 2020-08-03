#!/usr/bin/env bash

# Ref: https://developer.android.com/training/monitoring-device-state/doze-standby#testing_your_app_with_app_standby

echo "Standby OFF"
echo "adb shell dumpsys battery reset"
adb shell dumpsys battery reset

echo "adb shell am set-inactive im.vector.app false"
adb shell am set-inactive im.vector.app false
