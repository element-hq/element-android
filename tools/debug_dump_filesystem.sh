#!/usr/bin/env bash

adb shell am broadcast -a im.vector.app.DEBUG_ACTION_DUMP_FILESYSTEM
