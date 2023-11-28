# Nightly builds

<!--- TOC -->

* [Configuration](#configuration)
* [How to register to get nightly build](#how-to-register-to-get-nightly-build)
* [Build nightly manually](#build-nightly-manually)

<!--- END -->

## Configuration

The nightly build will contain what's on develop, in release mode, for Gplay variant. It is signed using a dedicated signature, and has a dedicated appId (`im.vector.app.nightly`), so it can be installed along with the production version of Element Android. The only other difference compared to Element Android is a different app icon background. We do not want to change the app name since it will also affect some strings in the app, and we do want to do that.

Nightly builds are built and released to Firebase every days, and automatically.

This is recommended to exclusively use this app, with your main account, instead of Element Android, and fallback to Element Android just in case of regression, to discover as soon as possible any regression, and report it to the team. To avoid double notification, you may want to disable the notification from the Element Android production version. Just open Element Android, navigate to `Settings/Notifications` and uncheck `Enable notifications for this session`.

*Note:* Due to a limitation of Firebase, the nightly build is the universal build, which means that the size of the APK is a bit bigger, but this should not have any other side effect.

## How to register to get nightly build

Provide your email to the Android team, who will add it to the list "External testers" on Firebase. You will then receive an invite on the provided email.

Follow the instructions on the email to install the latest nightly build. This is not clear yet if new nightly build will be automatically installed or not.

## Build nightly manually

Nightly build can be built manually from your computer. You will need to retrieved some secrets from Passbolt and add them to your file `~/.gradle/gradle.properties`:

```
signing.element.nightly.storePassword=VALUE_FROM_PASSBOLT
signing.element.nightly.keyId=VALUE_FROM_PASSBOLT
signing.element.nightly.keyPassword=VALUE_FROM_PASSBOLT
```

You will also need to add the environment variable `FIREBASE_TOKEN`:

```sh
export FIREBASE_TOKEN=VALUE_FROM_PASSBOLT
```

Then you can run the following commands (which are also used in the file for [the GitHub action](../.github/workflows/nightly.yml)):

```sh
git checkout develop
mv towncrier.toml towncrier.toml.bak
sed 's/CHANGES\.md/CHANGES_NIGHTLY\.md/' towncrier.toml.bak > towncrier.toml
rm towncrier.toml.bak
yes n | towncrier build --version nightly
./gradlew assembleGplayNightly appDistributionUploadNightly $CI_GRADLE_ARG_PROPERTIES
```

Then you can reset the change on the codebase.
