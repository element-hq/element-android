#!/usr/bin/env bash

########
# This script build the Jitsi library with LIBRE_BUILD flag.
# Following instructions from here https://github.com/jitsi/jitsi-meet/tree/master/android#build-and-use-your-own-sdk-artifactsbinaries
# It then export the library in a maven repository, that we host here https://github.com/vector-im/jitsi_libre_maven

# exit on any error
set -e

echo
echo "##################################################"
echo "Cloning jitsi-meet repository"
echo "##################################################"

cd ..
rm -rf jitsi-meet
git clone https://github.com/jitsi/jitsi-meet

# We want a libre build!
export LIBRE_BUILD=true

cd jitsi-meet

# This is commit after version 2.2.2, which does not compile
# git checkout 5a934c071a5cbe64de275a25d0ed62d8193cdd03

# Version android-sdk-3.10.0, commit 99e56e229dfa3c490096e37c3e5b76d2a3f23e32
git checkout android-sdk-3.10.0

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
