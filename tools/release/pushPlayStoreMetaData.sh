#!/usr/bin/env bash

#
# Copyright (c) 2021 New Vector Ltd
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

# Fastlane / PlayStore require longDescription and shortDescription file to be set, so copy the default one for
echo "Copying default description when missing"
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

if [[ -f "./fastlane/metadata/android/vi/full_description.txt" ]]; then
  echo "It appears that file ./fastlane/metadata/android/vi/full_description.txt now exists. This can be removed."
  removeFullDes_vi=0
else
  echo "Copy default full description to ./fastlane/metadata/android/vi"
  cp ./fastlane/metadata/android/en-US/full_description.txt ./fastlane/metadata/android/vi
  removeFullDes_vi=1
fi

# Run fastlane
echo "Run fastlane to push to the PlaysStore"
fastlane deployMeta

echo "Cleanup"
mv ./fastlane_tmp/* ./fastlane/metadata/android/

# Delete the tmp folder (should be empty)
rmdir ./fastlane_tmp

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

if [[ ${removeFullDes_vi} -eq 1 ]]; then
  rm ./fastlane/metadata/android/vi/full_description.txt
fi

echo "Success!"
