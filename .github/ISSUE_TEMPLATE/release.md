---
name: Release
about: Checklist for each release. To be used by the core team only.
title: "[Release] Element Android v"
labels: "\U0001F680 Release"
assignees: bmarty

---

For the example, we are releasing the version 1.1.10

### Before the release

- [ ] Weblate sync, fix lint issue if any (in a dedicated PR)
- [ ] Check the update of the store descriptions (using Google Translate if necessary) to ensure that the changes are acceptable to be published to the stores.

### Do the release

- [ ] Create release with gitflow, branch name `release/1.1.10`
- [ ] Run the script `./tools/release/pushPlayStoreMetaData.sh`. You can check in the GooglePlay console the Activity log to check the effect.
- [ ] Run `./tools/import_emojis.py` and commit the change if any.
- [ ] Run `./tools/import_sas_strings.py` and commit the change if any. If there is no change since a while, ping Travis
- [ ] Check the crashes from the PlayStore
- [ ] Check the rageshake with the current dev version. For instance https://github.com/matrix-org/element-android-rageshakes/labels/1.1.10-dev
- [ ] Run the integration test, and especially `UiAllScreensSanityTest.allScreensTest()`
- [ ] Create an account on matrix.org
- [ ] Run towncrier: `./towncrier --version v1.1.10` (add `--draft` for a preview)
- [ ] Add file for fastlane under ./fastlane/metadata/android/en-US/changelogs
- [ ] Push the branch and start a draft PR (will not be merged), to check that the CI is happy with all the changes.
- [ ] Finish release with gitflow, delete the draft PR
- [ ] Push `main` and the new tag `v1.1.10` to origin
- [ ] Checkout `develop`
- [ ] Increase version in `./vector/build.gradle`
- [ ] Commit and push `develop`
- [ ] Wait for [Buildkite](https://buildkite.com/matrix-dot-org/element-android/builds?branch=main) to build the `main` branch.
- [ ] Run the script `~/scripts/releaseElement.sh`. It will download the APKs from Buildkite check them and sign them.
- [ ] Install the APK on your phone to check that the upgrade went well (no init sync, etc.)
- [ ] Create a new beta release on the GooglePlay console and upload the 4 signed Apks.
- [ ] Check that the version codes are correct
- [ ] Copy the fastlane change to the GooglePlay console in the section en-GB.
- [ ] Push to beta release to 100% of the users
- [ ] Create the release on gitHub [from the tag](https://github.com/vector-im/element-android/tags), copy paste the block from the file CHANGES.md
- [ ] Add the 4 signed APKs to the GitHub release
- [ ] Ping the Android Internal room
- [ ] Add an entry in the internal diary

### Once Live on PlayStore

- [ ] Ping the Android public room and update its topic

### After at least 2 days

- [ ] Check the [rageshakes](https://github.com/matrix-org/element-android-rageshakes/issues)
- [ ] Check the crash reports on the GooglePlay console
- [ ] Check the Android Element room for any reported issues on the new version
- [ ] If all is OK, push to production and notify Markus (Bubu) to release the F-Droid version
- [ ] Ping the Android public room and update its topic with the new available version

### Android SDK2

- [ ] Checkout the `main` branch on Element Android project

#### On the SDK2 project

https://github.com/matrix-org/matrix-android-sdk2

- [ ] Create a release with GitFlow
- [ ] Update the files `./build.gradle` and `./gradle/gradle-wrapper.properties` manually, to use the latest version for the dependency. You can get inspired by the same files on Element Android project.
- [ ] Run the script `./tools/import_from_element.sh`
- [ ] Update the version in `./matrix-sdk-android/build.gradle` and let the script finish to build the library
- [ ] Update the file `CHANGES.md`
- [ ] Finish the release using GitFlow
- [ ] Create the release on GitHub from [the tag](https://github.com/matrix-org/matrix-android-sdk2/tags)
- [ ] Upload the AAR on the GitHub release

### Android SDK2 sample

https://github.com/matrix-org/matrix-android-sdk2-sample

- [ ] Update the dependency to the new version of the SDK2. Jitpack will have to build the AAR, it can take a few minutes. You can check status on https://jitpack.io/#matrix-org/matrix-android-sdk2
- [ ] Build and run the sample, you may have to fix some API break
- [ ] Commit and push directly on `main`

<!-- Note: some scripts are not public because they contain some private keys -->
