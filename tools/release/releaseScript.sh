#!/usr/bin/env bash

# Copyright 2022-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
# Please see LICENSE files in the repository root for full details.

# Ignore any error to not stop the script
set +e

printf "\n================================================================================\n"
printf "|                    Welcome to the release script!                            |\n"
printf "================================================================================\n"

printf "Checking environment...\n"
envError=0

# Path of the key store (it's a file)
keyStorePath="${ELEMENT_KEYSTORE_PATH}"
if [[ -z "${keyStorePath}" ]]; then
    printf "Fatal: ELEMENT_KEYSTORE_PATH is not defined in the environment.\n"
    envError=1
fi
# Keystore password
keyStorePassword="${ELEMENT_KEYSTORE_PASSWORD}"
if [[ -z "${keyStorePassword}" ]]; then
    printf "Fatal: ELEMENT_KEYSTORE_PASSWORD is not defined in the environment.\n"
    envError=1
fi
# Key password
keyPassword="${ELEMENT_KEY_PASSWORD}"
if [[ -z "${keyPassword}" ]]; then
    printf "Fatal: ELEMENT_KEY_PASSWORD is not defined in the environment.\n"
    envError=1
fi
# GitHub token
gitHubToken="${ELEMENT_GITHUB_TOKEN}"
if [[ -z "${gitHubToken}" ]]; then
    printf "Fatal: ELEMENT_GITHUB_TOKEN is not defined in the environment.\n"
    envError=1
fi
# Android home
androidHome="${ANDROID_HOME}"
if [[ -z "${androidHome}" ]]; then
    printf "Fatal: ANDROID_HOME is not defined in the environment.\n"
    envError=1
fi
# @elementbot:matrix.org matrix token / Not mandatory
elementBotToken="${ELEMENT_BOT_MATRIX_TOKEN}"
if [[ -z "${elementBotToken}" ]]; then
    printf "Warning: ELEMENT_BOT_MATRIX_TOKEN is not defined in the environment.\n"
fi

if [ ${envError} == 1 ]; then
  exit 1
fi

buildToolsVersion="35.0.0"
buildToolsPath="${androidHome}/build-tools/${buildToolsVersion}"

if [[ ! -d ${buildToolsPath} ]]; then
    printf "Fatal: ${buildToolsPath} folder not found, ensure that you have installed the SDK version ${buildToolsVersion}.\n"
    exit 1
fi

# Check if git flow is enabled
git flow config >/dev/null 2>&1
if [[ $? == 0 ]]
then
    printf "Git flow is initialized\n"
else
    printf "Git flow is not initialized. Initializing...\n"
    # All default value, just set 'v' for tag prefix
    git flow init -d -t 'v'
fi

printf "OK\n"

printf "\n================================================================================\n"
printf "Ensuring main and develop branches are up to date...\n"

git checkout main
git pull
git checkout develop
git pull

printf "\n================================================================================\n"
# Guessing version to propose a default version
versionMajorCandidate=`grep "ext.versionMajor" ./vector-app/build.gradle | cut  -d " " -f3`
versionMinorCandidate=`grep "ext.versionMinor" ./vector-app/build.gradle | cut  -d " " -f3`
versionPatchCandidate=`grep "ext.versionPatch" ./vector-app/build.gradle | cut  -d " " -f3`
versionCandidate="${versionMajorCandidate}.${versionMinorCandidate}.${versionPatchCandidate}"

read -p "Please enter the release version (example: ${versionCandidate}). Just press enter if ${versionCandidate} is correct. " version
version=${version:-${versionCandidate}}

# extract major, minor and patch for future use
versionMajor=`echo ${version} | cut  -d "." -f1`
versionMinor=`echo ${version} | cut  -d "." -f2`
versionPatch=`echo ${version} | cut  -d "." -f3`
nextPatchVersion=$((versionPatch + 2))

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
sed "s/ext.versionPatch = .*/ext.versionPatch = ${versionPatch}/" ./vector-app/build.gradle.bak > ./vector-app/build.gradle
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
printf "Uninstalling previous debug app if any...\n"
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

# Get the changes to use it to create the GitHub release
changelogUrlEncoded=`git diff CHANGES.md | grep ^+ | tail -n +2 | cut -c2- | jq -sRr @uri | sed s/\(/%28/g | sed s/\)/%29/g`

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
printf "Main changes in this version: TODO.\nFull changelog: https://github.com/element-hq/element-android/releases" > ${fastlanePathFile}

read -p "I have created the file ${fastlanePathFile}, please edit it and press enter when it's done."
git add ${fastlanePathFile}
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
printf "Wait for the GitHub action https://github.com/element-hq/element-android/actions/workflows/build.yml?query=branch%%3Amain to build the 'main' branch.\n"
read -p "After GHA is finished, please enter the artifact URL (for 'vector-gplay-release-unsigned'): " artifactUrl

printf "\n================================================================================\n"
printf "Downloading the artifact...\n"

# Download files
targetPath="./tmp/Element/${version}"

# Ignore error
set +e

python3 ./tools/release/download_github_artifacts.py \
    --token ${gitHubToken} \
    --artifactUrl ${artifactUrl} \
    --directory ${targetPath} \
    --ignoreErrors

# Do not ignore error
set -e

printf "\n================================================================================\n"
printf "Unzipping the artifact...\n"

unzip ${targetPath}/vector-gplay-release-unsigned.zip -d ${targetPath}

# Flatten folder hierarchy
mv ${targetPath}/gplay/release/* ${targetPath}
rm -rf ${targetPath}/gplay

printf "\n================================================================================\n"
printf "Signing the APKs...\n"

cp ${targetPath}/vector-gplay-arm64-v8a-release-unsigned.apk \
   ${targetPath}/vector-gplay-arm64-v8a-release-signed.apk
./tools/release/sign_apk_unsafe.sh \
    ${keyStorePath} \
    ${targetPath}/vector-gplay-arm64-v8a-release-signed.apk \
    ${keyStorePassword} \
    ${keyPassword}

cp ${targetPath}/vector-gplay-armeabi-v7a-release-unsigned.apk \
   ${targetPath}/vector-gplay-armeabi-v7a-release-signed.apk
./tools/release/sign_apk_unsafe.sh \
    ${keyStorePath} \
    ${targetPath}/vector-gplay-armeabi-v7a-release-signed.apk \
    ${keyStorePassword} \
    ${keyPassword}

cp ${targetPath}/vector-gplay-x86-release-unsigned.apk \
   ${targetPath}/vector-gplay-x86-release-signed.apk
./tools/release/sign_apk_unsafe.sh \
    ${keyStorePath} \
    ${targetPath}/vector-gplay-x86-release-signed.apk \
    ${keyStorePassword} \
    ${keyPassword}

cp ${targetPath}/vector-gplay-x86_64-release-unsigned.apk \
   ${targetPath}/vector-gplay-x86_64-release-signed.apk
./tools/release/sign_apk_unsafe.sh \
    ${keyStorePath} \
    ${targetPath}/vector-gplay-x86_64-release-signed.apk \
    ${keyStorePassword} \
    ${keyPassword}

# Ref: https://docs.fastlane.tools/getting-started/android/beta-deployment/#uploading-your-app
# set SUPPLY_APK_PATHS="${targetPath}/vector-gplay-arm64-v8a-release-unsigned.apk,${targetPath}/vector-gplay-armeabi-v7a-release-unsigned.apk,${targetPath}/vector-gplay-x86-release-unsigned.apk,${targetPath}/vector-gplay-x86_64-release-unsigned.apk"
#
# ./fastlane beta

printf "\n================================================================================\n"
printf "Please check the information below:\n"

printf "File vector-gplay-arm64-v8a-release-signed.apk:\n"
${buildToolsPath}/aapt dump badging ${targetPath}/vector-gplay-arm64-v8a-release-signed.apk | grep package
printf "File vector-gplay-armeabi-v7a-release-signed.apk:\n"
${buildToolsPath}/aapt dump badging ${targetPath}/vector-gplay-armeabi-v7a-release-signed.apk | grep package
printf "File vector-gplay-x86-release-signed.apk:\n"
${buildToolsPath}/aapt dump badging ${targetPath}/vector-gplay-x86-release-signed.apk | grep package
printf "File vector-gplay-x86_64-release-signed.apk:\n"
${buildToolsPath}/aapt dump badging ${targetPath}/vector-gplay-x86_64-release-signed.apk | grep package

printf "\n"
read -p "Does it look correct? Press enter when it's done."

printf "\n================================================================================\n"
read -p "Installing apk on a real device, press enter when a real device is connected. "
apkPath="${targetPath}/vector-gplay-arm64-v8a-release-signed.apk"
# Ignore error
set +e
adb -d install ${apkPath}
# Do not ignore error
set -e

read -p "Please run the APK on your phone to check that the upgrade went well (no init sync, etc.). Press enter when it's done."

printf "\n================================================================================\n"
githubCreateReleaseLink="https://github.com/element-hq/element-android/releases/new?tag=v${version}&title=Element%20Android%20v${version}&body=${changelogUrlEncoded}"
printf "Creating the release on gitHub.\n"
printf -- "Open this link: %s\n" ${githubCreateReleaseLink}
printf "Then\n"
printf " - click on the 'Generate releases notes' button\n"
printf " - Add the 4 signed APKs to the GitHub release. They are located at ${targetPath}\n"
read -p ". Press enter when it's done. "

printf "\n================================================================================\n"
printf "Message for the Android internal room:\n\n"
message="@room Element Android ${version} is ready to be tested. You can get it from https://github.com/element-hq/element-android/releases/tag/v${version}. Please report any feedback here. Thanks!"
printf "${message}\n\n"

if [[ -z "${elementBotToken}" ]]; then
  read -p "ELEMENT_BOT_MATRIX_TOKEN is not defined in the environment. Cannot send the message for you. Please send it manually, and press enter when it's done "
else
  read -p "Send this message to the room (yes/no) default to yes? " doSend
  doSend=${doSend:-yes}
  if [ ${doSend} == "yes" ]; then
    printf "Sending message...\n"
    transactionId=`openssl rand -hex 16`
    # Element Android internal
    matrixRoomId="!LiSLXinTDCsepePiYW:matrix.org"
    curl -X PUT --data $"{\"msgtype\":\"m.text\",\"body\":\"${message}\"}" -H "Authorization: Bearer ${elementBotToken}" https://matrix-client.matrix.org/_matrix/client/r0/rooms/${matrixRoomId}/send/m.room.message/\$local.${transactionId}
  else
    printf "Message not sent, please send it manually!\n"
  fi
fi

printf "\n================================================================================\n"
printf "Congratulation! Kudos for using this script! Have a nice day!\n"
printf "================================================================================\n"
