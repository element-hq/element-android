#!/usr/bin/env bash

## From https://developer.android.com/develop/ui/views/notifications/notification-permission#test

PACKAGE_NAME=im.vector.app.debug

# App is newly installed on a device that runs Android 13 or higher:

adb shell pm revoke                 ${PACKAGE_NAME} android.permission.POST_NOTIFICATIONS
adb shell pm clear-permission-flags ${PACKAGE_NAME} android.permission.POST_NOTIFICATIONS user-set
adb shell pm clear-permission-flags ${PACKAGE_NAME} android.permission.POST_NOTIFICATIONS user-fixed

# The user keeps notifications enabled when the app is installed on a device that runs 12L or lower,
# then the device upgrades to Android 13 or higher:

# adb shell pm grant                  ${PACKAGE_NAME} android.permission.POST_NOTIFICATIONS
# adb shell pm set-permission-flags   ${PACKAGE_NAME} android.permission.POST_NOTIFICATIONS user-set
# adb shell pm clear-permission-flags ${PACKAGE_NAME} android.permission.POST_NOTIFICATIONS user-fixed

# The user manually disables notifications when the app is installed on a device that runs 12L or lower,
# then the device upgrades to Android 13 or higher:

# adb shell pm revoke                 ${PACKAGE_NAME} android.permission.POST_NOTIFICATIONS
# adb shell pm set-permission-flags   ${PACKAGE_NAME} android.permission.POST_NOTIFICATIONS user-set
# adb shell pm clear-permission-flags ${PACKAGE_NAME} android.permission.POST_NOTIFICATIONS user-fixed
