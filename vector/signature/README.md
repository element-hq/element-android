
## Debug signature

Buildkite CI tool uses docker images to build the Android application, and it looks like the debug signature is changed at each build.

So it's not possible for user to upgrade the application with the last build from buildkite without uninstalling the application.

This folder contains a debug signature, and the debug build will uses this signature to build the APK.

The validity of the signature is 30 years. So it has to be replaced before June 2049 :).

More info about the debug signature: https://developer.android.com/studio/publish/app-signing#debug-mode
