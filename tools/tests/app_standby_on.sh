#!/usr/bin/env bash

# Ref: https://developer.android.com/training/monitoring-device-state/doze-standby#testing_your_app_with_app_standby

echo "Standby ON"
echo "adb shell dumpsys battery unplug"
adb shell dumpsys battery unplug

echo "adb shell am set-inactive im.vector.app true"
adb shell am set-inactive im.vector.app true
