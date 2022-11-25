#!/usr/bin/env bash

#
# Copyright (c) 2022 New Vector Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Ignore any error to not stop the script
set +e

printf "\n"
printf "================================================================================\n"
printf "|                    Welcome to the release script!                            |\n"
printf "================================================================================\n"

releaseScriptLocation="${RELEASE_SCRIPT_PATH}"

if [[ -z "${releaseScriptLocation}" ]]; then
    printf "Fatal: RELEASE_SCRIPT_PATH is not defined in the environment. Please set to the path of your local file 'releaseElement2.sh'.\n"
    exit 1
fi

releaseScriptFullPath="${releaseScriptLocation}/releaseElement2.sh"

if [[ ! -f ${releaseScriptFullPath} ]]; then
  printf "Fatal: release script not found at ${releaseScriptFullPath}.\n"
  exit 1
fi

# Guessing version to propose a default version
versionMajorCandidate=`grep "ext.versionMajor" ./vector-app/build.gradle | cut  -d " " -f3`
versionMinorCandidate=`grep "ext.versionMinor" ./vector-app/build.gradle | cut  -d " " -f3`
versionPatchCandidate=`grep "ext.versionPatch" ./vector-app/build.gradle | cut  -d " " -f3`
versionCandidate="${versionMajorCandidate}.${versionMinorCandidate}.${versionPatchCandidate}"

printf "\n"
read -p "Please enter the release version (example: ${versionCandidate}). Just press enter if ${versionCandidate} is correct. " version
version=${version:-${versionCandidate}}

# extract major, minor and patch for future use
versionMajor=`echo ${version} | cut  -d "." -f1`
versionMinor=`echo ${version} | cut  -d "." -f2`
versionPatch=`echo ${version} | cut  -d "." -f3`
nextPatchVersion=$((versionPatch + 2))

printf "\n================================================================================\n"
printf "Ensuring main and develop branches are up to date...\n"

git checkout main
git pull
git checkout develop
git pull

printf "\n================================================================================\n"
printf "Starting the release ${version}\n"
git flow release start ${version}

# Note: in case the release is already started and the script is started again, checkout the release branch again.
ret=$?
if [[ $ret -ne 0 ]]; then
  printf "Mmh, it seems that the release is already started. Checking out the release branch...\n"
  git checkout "release/${version}"
fi

# Ensure version is OK
cp ./vector-app/build.gradle ./vector-app/build.gradle.bak
sed "s/ext.versionMajor = .*/ext.versionMajor = ${versionMajor}/" ./vector-app/build.gradle.bak > ./vector-app/build.gradle
sed "s/ext.versionMinor = .*/ext.versionMinor = ${versionMinor}/" ./vector-app/build.gradle     > ./vector-app/build.gradle.bak
sed "s/ext.versionPatch = .*/ext.versionPatch = ${patchVersion}/" ./vector-app/build.gradle.bak > ./vector-app/build.gradle
rm ./vector-app/build.gradle.bak
cp ./matrix-sdk-android/build.gradle ./matrix-sdk-android/build.gradle.bak
sed "s/\"SDK_VERSION\", .*$/\"SDK_VERSION\", \"\\\\\"${version}\\\\\"\"/" ./matrix-sdk-android/build.gradle.bak > ./matrix-sdk-android/build.gradle
rm ./matrix-sdk-android/build.gradle.bak

# This commit may have no effect because generally we do not change the version during the release.
git commit -a -m "Setting version for the release ${version}"

printf "\n================================================================================\n"
read -p "Please check the crashes from the PlayStore. You can commit fixes if any on the release branch. Press enter when it's done."

printf "\n================================================================================\n"
read -p "Please check the rageshake with the current dev version: https://github.com/matrix-org/element-android-rageshakes/labels/${version}-dev. You can commit fixes if any on the release branch. Press enter when it's done."

printf "\n================================================================================\n"
read -p "Please make sure an emulator is running and press enter when it is ready."

printf "\n================================================================================\n"
printf "Checking if Synapse is running...\n"
httpCode=`curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/_matrix/static`

if [[ ${httpCode} -ne "302" ]]; then
  read -p "Please make sure Synapse is running (open http://127.0.0.1:8080) and press enter when it is ready."
else
  printf "Synapse is running!\n"
fi

printf "\n================================================================================\n"
printf "Uninstalling previous test app if any...\n"
adb -e uninstall im.vector.app.debug.test

printf "\n================================================================================\n"
printf "Running the integration test UiAllScreensSanityTest.allScreensTest()...\n"
./gradlew connectedGplayDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=im.vector.app.ui.UiAllScreensSanityTest

printf "\n================================================================================\n"
printf "Building the app...\n"
./gradlew assembleGplayDebug

printf "\n================================================================================\n"
printf "Uninstalling previous test app if any...\n"
adb -e uninstall im.vector.app.debug

printf "\n================================================================================\n"
printf "Installing the app...\n"
adb -e install ./vector-app/build/outputs/apk/gplay/debug/vector-gplay-arm64-v8a-debug.apk

printf "\n================================================================================\n"
printf "Running the app...\n"
# TODO This does not work, need to be fixed
adb -e shell am start -n im.vector.app.debug/im.vector.app.features.Alias -a android.intent.action.MAIN -c android.intent.category.LAUNCHER

printf "\n================================================================================\n"
# TODO could build and deploy the APK to any emulator
read -p "Create an account on matrix.org and do some smoke tests that the sanity test does not cover like: 1-1 call, 1-1 video call, Jitsi call for instance. Press enter when it's done."

printf "\n================================================================================\n"
printf "Running towncrier...\n"
yes | towncrier build --version "v${version}"

printf "\n================================================================================\n"
read -p "Check the file CHANGES.md consistency. It's possible to reorder items (most important changes first) or change their section if relevant. Also an opportunity to fix some typo, or rewrite things. Do not commit your change. Press enter when it's done."

printf "\n================================================================================\n"
printf "Committing...\n"
git commit -a -m "Changelog for version ${version}"

printf "\n================================================================================\n"
printf "Creating fastlane file...\n"
printf -v versionMajor2Digits "%02d" ${versionMajor}
printf -v versionMinor2Digits "%02d" ${versionMinor}
printf -v versionPatch2Digits "%02d" ${versionPatch}
fastlaneFile="4${versionMajor2Digits}${versionMinor2Digits}${versionPatch2Digits}0.txt"
fastlanePathFile="./fastlane/metadata/android/en-US/changelogs/${fastlaneFile}"
printf "Main changes in this version: TODO.\nFull changelog: https://github.com/vector-im/element-android/releases" > ${fastlanePathFile}

read -p "I have created the file ${fastlanePathFile}, please edit it and press enter when it's done."
git commit -a -m "Adding fastlane file for version ${version}"

printf "\n================================================================================\n"
# We could propose to push the branch and create a PR
read -p "(optional) Push the branch and start a draft PR (will not be merged), to check that the CI is happy with all the changes. Press enter when it's done."

printf "\n================================================================================\n"
printf "OK, finishing the release...\n"
git flow release finish "${version}"

printf "\n================================================================================\n"
read -p "Done, push the branch 'main' and the new tag (yes/no) default to yes? " doPush
doPush=${doPush:-yes}

if [ ${doPush} == "yes" ]; then
  printf "Pushing branch 'main' and tag 'v${version}'...\n"
  git push origin main
  git push origin "v${version}"
else
    printf "Not pushing, do not forget to push manually!\n"
fi

printf "\n================================================================================\n"
printf "Checking out develop...\n"
git checkout develop

# Set next version
printf "\n================================================================================\n"
printf "Setting next version on file './vector-app/build.gradle'...\n"
nextPatchVersion=$((versionPatch + 2))
cp ./vector-app/build.gradle ./vector-app/build.gradle.bak
sed "s/ext.versionPatch = .*/ext.versionPatch = ${nextPatchVersion}/" ./vector-app/build.gradle.bak > ./vector-app/build.gradle
rm ./vector-app/build.gradle.bak

printf "\n================================================================================\n"
printf "Setting next version on file './matrix-sdk-android/build.gradle'...\n"
nextVersion="${versionMajor}.${versionMinor}.${nextPatchVersion}"
cp ./matrix-sdk-android/build.gradle ./matrix-sdk-android/build.gradle.bak
sed "s/\"SDK_VERSION\", .*$/\"SDK_VERSION\", \"\\\\\"${nextVersion}\\\\\"\"/" ./matrix-sdk-android/build.gradle.bak > ./matrix-sdk-android/build.gradle
rm ./matrix-sdk-android/build.gradle.bak

printf "\n================================================================================\n"
read -p "I have updated the versions to prepare the next release, please check that the change are correct and press enter so I can commit."

printf "Committing...\n"
git commit -a -m 'version++'

printf "\n================================================================================\n"
read -p "Done, push the branch 'develop' (yes/no) default to yes? (A rebase may be necessary in case develop got new commits)" doPush
doPush=${doPush:-yes}

if [ ${doPush} == "yes" ]; then
  printf "Pushing branch 'develop'...\n"
  git push origin develop
else
    printf "Not pushing, do not forget to push manually!\n"
fi

printf "\n================================================================================\n"
read -p "Wait for the GitHub action https://github.com/vector-im/element-android/actions/workflows/build.yml?query=branch%3Amain to build the 'main' branch. Press enter when it's done."

printf "\n================================================================================\n"
printf "Running the release script...\n"
cd ${releaseScriptLocation}
${releaseScriptFullPath} "v${version}"
cd -

printf "\n================================================================================\n"
apkPath="${releaseScriptLocation}/Element/v${version}/vector-gplay-arm64-v8a-release-signed.apk"
printf "Installing apk on a real device...\n"
adb -d install ${apkPath}

read -p "Please run the APK on your phone to check that the upgrade went well (no init sync, etc.). Press enter when it's done."
# TODO Get the block to copy from towncrier earlier (be may be edited by the release manager)?
read -p "Create the release on gitHub from the tag https://github.com/vector-im/element-android/tags, copy paste the block from the file CHANGES.md. Press enter when it's done."

read -p "Add the 4 signed APKs to the GitHub release. Press enter when it's done."

printf "\n================================================================================\n"
printf "Ping the Android Internal room. Here is an example of message which can be sent:\n\n"
printf "@room Element Android ${version} is ready to be tested. You can get if from https://github.com/vector-im/element-android/releases/tag/v${version}. Please report any feedback here. Thanks!\n\n"
read -p "Press enter when it's done."

printf "\n================================================================================\n"
printf "Congratulation! Kudos for using this script! Have a nice day!\n"
printf "================================================================================\n"
