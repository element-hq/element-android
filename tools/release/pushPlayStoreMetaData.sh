#!/usr/bin/env bash

# Copyright 2021-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
# Please see LICENSE files in the repository root for full details.

set +e

# Fastlane / PlayStore is not happy if the folder name for local is not supported by the PlayStore. So temporary move them before running fast lane
# List of supported languages: https://support.google.com/googleplay/android-developer/answer/9844778?hl=en#zippy=%2Cview-list-of-available-languages

echo "Ignoring some languages not supported by the PlayStore"
mkdir ./fastlane_tmp
mv ./fastlane/metadata/android/eo  ./fastlane_tmp
mv ./fastlane/metadata/android/fy  ./fastlane_tmp
mv ./fastlane/metadata/android/ga  ./fastlane_tmp
mv ./fastlane/metadata/android/kab ./fastlane_tmp
mv ./fastlane/metadata/android/nb  ./fastlane_tmp

# Fastlane / PlayStore require longDescription and shortDescription file to be set, so copy the default
# one for languages where they are missing
echo "Copying default description when missing"
if [[ -f "./fastlane/metadata/android/nl-NL/full_description.txt" ]]; then
  echo "It appears that file ./fastlane/metadata/android/nl-NL/full_description.txt now exists. This can be removed."
  removeFullDes_nl=0
else
  echo "Copy default full description to ./fastlane/metadata/android/nl-NL"
  cp ./fastlane/metadata/android/en-US/full_description.txt ./fastlane/metadata/android/nl-NL
  removeFullDes_nl=1
fi

if [[ -f "./fastlane/metadata/android/ro/full_description.txt" ]]; then
  echo "It appears that file ./fastlane/metadata/android/ro/full_description.txt now exists. This can be removed."
  removeFullDes_ro=0
else
  echo "Copy default full description to ./fastlane/metadata/android/ro"
  cp ./fastlane/metadata/android/en-US/full_description.txt ./fastlane/metadata/android/ro
  removeFullDes_ro=1
fi

if [[ -f "./fastlane/metadata/android/si-LK/full_description.txt" ]]; then
  echo "It appears that file ./fastlane/metadata/android/si-LK/full_description.txt now exists. This can be removed."
  removeFullDes_si=0
else
  echo "Copy default full description to ./fastlane/metadata/android/si-LK"
  cp ./fastlane/metadata/android/en-US/full_description.txt ./fastlane/metadata/android/si-LK
  removeFullDes_si=1
fi

if [[ -f "./fastlane/metadata/android/si-LK/short_description.txt" ]]; then
  echo "It appears that file ./fastlane/metadata/android/si-LK/short_description.txt now exists. This can be removed."
  removeShortDes_si=0
else
  echo "Copy default short description to ./fastlane/metadata/android/si-LK"
  cp ./fastlane/metadata/android/en-US/short_description.txt ./fastlane/metadata/android/si-LK
  removeShortDes_si=1
fi

if [[ -f "./fastlane/metadata/android/th/full_description.txt" ]]; then
  echo "It appears that file ./fastlane/metadata/android/th/full_description.txt now exists. This can be removed."
  removeFullDes_th=0
else
  echo "Copy default full description to ./fastlane/metadata/android/th"
  cp ./fastlane/metadata/android/en-US/full_description.txt ./fastlane/metadata/android/th
  removeFullDes_th=1
fi

if [[ -f "./fastlane/metadata/android/az-AZ/full_description.txt" ]]; then
  echo "It appears that file ./fastlane/metadata/android/az-AZ/full_description.txt now exists. This can be removed."
  removeFullDes_az=0
else
  echo "Copy default full description to ./fastlane/metadata/android/az-AZ"
  cp ./fastlane/metadata/android/en-US/full_description.txt ./fastlane/metadata/android/az-AZ
  removeFullDes_az=1
fi

# Run fastlane
echo "Run fastlane to push to the PlaysStore"
fastlane deployMeta

echo "Cleanup"
mv ./fastlane_tmp/* ./fastlane/metadata/android/

# Delete the tmp folder (should be empty)
rmdir ./fastlane_tmp

if [[ ${removeFullDes_nl} -eq 1 ]]; then
  rm ./fastlane/metadata/android/nl-NL/full_description.txt
fi

if [[ ${removeFullDes_ro} -eq 1 ]]; then
  rm ./fastlane/metadata/android/ro/full_description.txt
fi

if [[ ${removeFullDes_si} -eq 1 ]]; then
  rm ./fastlane/metadata/android/si-LK/full_description.txt
fi

if [[ ${removeShortDes_si} -eq 1 ]]; then
  rm ./fastlane/metadata/android/si-LK/short_description.txt
fi

if [[ ${removeFullDes_th} -eq 1 ]]; then
  rm ./fastlane/metadata/android/th/full_description.txt
fi

if [[ ${removeFullDes_az} -eq 1 ]]; then
  rm ./fastlane/metadata/android/az-AZ/full_description.txt
fi

echo "Success!"
