# Flipper

<!--- TOC -->

* [Introduction](#introduction)
* [Setup](#setup)
  * [Troubleshoot](#troubleshoot)
    * [No device found issue](#no-device-found-issue)
    * [Diagnostic Activity](#diagnostic-activity)
    * [Other](#other)
* [Links](#links)

<!--- END -->

## Introduction

[Flipper](https://fbflipper.com) is a powerful tool from Meta, which allow to inspect the running application details and states from your computer.

Flipper is configured in the Element Android project to let the developers be able to:
- inspect all the Realm databases content;
- do layout inspection;
- see the crash logs;
- see the logcat;
- see all the network requests;
- see all the SharedPreferences;
- take screenshots and record videos of the device;
- and more!

## Setup

- Install Flipper on your computer. Follow instructions here: https://fbflipper.com/docs/getting-started/index/
- Run the debug version of Element on an emulator or on a real device.

### Troubleshoot

#### No device found issue

The configuration of the Flipper application has to be updated. The issue has been asked and answered here: https://stackoverflow.com/questions/71744103/android-emulator-unable-to-connect-to-flipper/72608113#72608113

#### Diagnostic Activity

Flipper comes with a Diagnostic Activity that you can start from command line using:

```shell
adb shell am start -n im.vector.app.debug/com.facebook.flipper.android.diagnostics.FlipperDiagnosticActivity
```

It provides some log which can help to figure out what's going on client side.

#### Other

https://fbflipper.com/docs/getting-started/troubleshooting/android/ may help.

## Links

- Official Flipper website: https://fbflipper.com
- Realm Plugin for Flipper: https://github.com/kamgurgul/Flipper-Realm
- Dedicated Matrix room: https://matrix.to/#/#unifiedpush:matrix.org
