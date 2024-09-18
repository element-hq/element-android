#!/usr/bin/env bash

########
# This script build the Jitsi library with LIBRE_BUILD flag.
# Following instructions from here https://github.com/jitsi/jitsi-meet/tree/master/android#build-and-use-your-own-sdk-artifactsbinaries
# It then export the library in a maven repository, that we host here https://github.com/element-hq/jitsi_libre_maven

# exit on any error
set -e

echo
echo "##################################################"
echo "Cloning jitsi-meet repository"
echo "##################################################"

cd ..
rm -rf jitsi-meet
git clone https://github.com/jitsi/jitsi-meet

# Android SDK
export ANDROID_SDK_ROOT=~/Library/Android/sdk

# We want a libre build!
export LIBRE_BUILD=true

cd jitsi-meet

# Get the latest version from the changelog: https://github.com/jitsi/jitsi-meet-release-notes/blob/master/CHANGELOG-MOBILE-SDKS.md
git checkout mobile-sdk-10.2.0

echo
echo "##################################################"
echo "npm install"
echo "##################################################"

npm install
#make

#echo
#echo "##################################################"
#echo "Build the Android library"
#echo "##################################################"
#
#pushd android
#./gradlew assembleRelease
#popd
#
#echo
#echo "##################################################"
#echo "Bundle with React Native"
#echo "##################################################"
#
#react-native bundle --platform android --dev false --entry-file index.android.js --bundle-output index.android.bundle --assets-dest android/app/src/main/res/

./android/scripts/release-sdk.sh /tmp/jitsi/

# Also copy jsc

mkdir -p /tmp/jitsi/org/webkit/
cp -r ./node_modules/jsc-android/dist/org/webkit/android-jsc /tmp/jitsi/org/webkit/

echo
echo "##################################################"
echo "Release has been done here: /tmp/jitsi/"
echo "##################################################"
