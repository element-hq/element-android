#!/usr/bin/env bash

# Ref: https://developer.android.com/training/monitoring-device-state/doze-standby#testing_doze

echo "Exit doze mode"
echo "shell dumpsys deviceidle unforce"
adb shell dumpsys deviceidle unforce

echo "Reactivate device"
echo "shell dumpsys battery reset"
adb shell dumpsys battery reset
