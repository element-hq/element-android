# Jitsi in Element Android

<!--- TOC -->

* [Native Jitsi SDK](#native-jitsi-sdk)
  * [How to build the Jitsi Meet SDK](#how-to-build-the-jitsi-meet-sdk)
    * [Jitsi version](#jitsi-version)
    * [Run the build script](#run-the-build-script)
    * [Link with the new generated library](#link-with-the-new-generated-library)
    * [Sanity tests](#sanity-tests)
    * [Export the build library](#export-the-build-library)

<!--- END -->

Native Jitsi support has been added to Element Android by the PR [#1914](https://github.com/element-hq/element-android/pull/1914). The description of the PR contains some documentation about the behaviour in each possible room configuration.

Also, ensure to have a look on [the documentation from Element Web](https://github.com/element-hq/element-web/blob/develop/docs/jitsi.md)

The official documentation about how to integrate the Jitsi SDK in an Android app is available here: https://jitsi.github.io/handbook/docs/dev-guide/dev-guide-android-sdk.

## Native Jitsi SDK

The Jitsi SDK is built by ourselves with the flag LIBRE_BUILD, to be able to be integrated on the F-Droid version of Element Android.

The generated maven repository is then host in the project https://github.com/element-hq/jitsi_libre_maven

### How to build the Jitsi Meet SDK

#### Jitsi version

Update the script `./tools/jitsi/build_jitsi_libs.sh` with the tag of the project `https://github.com/jitsi/jitsi-meet`.

Latest tag can be found from this page: https://github.com/jitsi/jitsi-meet-release-notes/blob/master/CHANGELOG-MOBILE-SDKS.md

Currently we are building the version with the tag `mobile-sdk-10.2.0`.

#### Run the build script

At the root of the Element Android, run the following script:

```shell script
./tools/jitsi/build_jitsi_libs.sh
```

It will build the Jitsi Meet Android library and put every generated files in the folder `/tmp/jitsi`

#### Link with the new generated library

- Update the file `./build.gradle` to use the previously created local Maven repository. Currently we have this line:

```groovy
url "https://github.com/element-hq/jitsi_libre_maven/raw/main/mobile-sdk-10.2.0"
```

You can uncomment and update the line starting with `// url "file://...` and comment the line starting with `url`, to test the library using the locally generated Maven repository.

- Update the dependency of the Jitsi Meet library in the file `./vector/build.gradle`. Currently we have this line:

```groovy
api('org.jitsi.react:jitsi-meet-sdk:10.2.0')
```

- Perform a gradle sync and build the project
- Perform test

#### Sanity tests

In order to validate that the upgrade of the Jitsi and WebRTC dependency does not break anything, the following sanity tests have to be performed, using two devices:
- Make 1-1 audio call (so using WebRTC)
- Make 1-1 video call (so using WebRTC)
- Create and join a conference call with audio only (so using Jitsi library). Leave the conference. Join it again.
- Create and join a conference call with audio and video (so using Jitsi library) Leave the conference. Join it again.

#### Export the build library

If all the tests are passed, you can export the generated Jitsi library to our Maven repository.

- Clone the project https://github.com/element-hq/jitsi_libre_maven.
- Create a new folder with the version name.
- Copy every generated files form `/tmp/jitsi` to the folder you have just created.
- Commit and push the change on https://github.com/element-hq/jitsi_libre_maven.
- Update the file `./build.gradle` to use the previously created Maven repository. Currently we have this line:

```groovy
url "https://github.com/element-hq/jitsi_libre_maven/raw/main/mobile-sdk-10.2.0"
```

- Build the project and perform the sanity tests again.

- Create a PR for project Element Android and add a changelog file `<PR_NUMBER>.misc` to notify about the library upgrade.
