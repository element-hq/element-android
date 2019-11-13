#!/usr/bin/env bash

#
# Copyright 2018 New Vector Ltd
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

# Exit on any error
set -e

echo
echo "Copy strings to SDK"

cp ../matrix-android-sdk/matrix-sdk/src/main/res/values/strings.xml        ./matrix-sdk-android/src/main/res/values/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-ar/strings.xml     ./matrix-sdk-android/src/main/res/values-ar/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-bg/strings.xml     ./matrix-sdk-android/src/main/res/values-bg/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-bs/strings.xml     ./matrix-sdk-android/src/main/res/values-bs/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-ca/strings.xml     ./matrix-sdk-android/src/main/res/values-ca/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-cs/strings.xml     ./matrix-sdk-android/src/main/res/values-cs/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-da/strings.xml     ./matrix-sdk-android/src/main/res/values-da/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-de/strings.xml     ./matrix-sdk-android/src/main/res/values-de/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-el/strings.xml     ./matrix-sdk-android/src/main/res/values-el/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-eo/strings.xml     ./matrix-sdk-android/src/main/res/values-eo/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-en-rGB/strings.xml ./matrix-sdk-android/src/main/res/values-en-rGB/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-es/strings.xml     ./matrix-sdk-android/src/main/res/values-es/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-es-rMX/strings.xml ./matrix-sdk-android/src/main/res/values-es-rMX/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-eu/strings.xml     ./matrix-sdk-android/src/main/res/values-eu/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-fi/strings.xml     ./matrix-sdk-android/src/main/res/values-fi/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-fr/strings.xml     ./matrix-sdk-android/src/main/res/values-fr/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-gl/strings.xml     ./matrix-sdk-android/src/main/res/values-gl/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-hu/strings.xml     ./matrix-sdk-android/src/main/res/values-hu/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-id/strings.xml     ./matrix-sdk-android/src/main/res/values-id/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-in/strings.xml     ./matrix-sdk-android/src/main/res/values-in/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-is/strings.xml     ./matrix-sdk-android/src/main/res/values-is/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-it/strings.xml     ./matrix-sdk-android/src/main/res/values-it/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-ja/strings.xml     ./matrix-sdk-android/src/main/res/values-ja/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-ko/strings.xml     ./matrix-sdk-android/src/main/res/values-ko/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-lv/strings.xml     ./matrix-sdk-android/src/main/res/values-lv/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-nl/strings.xml     ./matrix-sdk-android/src/main/res/values-nl/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-nn/strings.xml     ./matrix-sdk-android/src/main/res/values-nn/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-pl/strings.xml     ./matrix-sdk-android/src/main/res/values-pl/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-pt/strings.xml     ./matrix-sdk-android/src/main/res/values-pt/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-pt-rBR/strings.xml ./matrix-sdk-android/src/main/res/values-pt-rBR/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-ru/strings.xml     ./matrix-sdk-android/src/main/res/values-ru/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-sk/strings.xml     ./matrix-sdk-android/src/main/res/values-sk/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-sq/strings.xml     ./matrix-sdk-android/src/main/res/values-sq/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-te/strings.xml     ./matrix-sdk-android/src/main/res/values-te/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-th/strings.xml     ./matrix-sdk-android/src/main/res/values-th/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-uk/strings.xml     ./matrix-sdk-android/src/main/res/values-uk/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-vls/strings.xml    ./matrix-sdk-android/src/main/res/values-vls/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-zh-rCN/strings.xml ./matrix-sdk-android/src/main/res/values-zh-rCN/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-zh-rTW/strings.xml ./matrix-sdk-android/src/main/res/values-zh-rTW/strings.xml

echo
echo "Copy strings to RiotX"

cp ../riot-android/vector/src/main/res/values/strings.xml           ./vector/src/main/res/values/strings.xml
cp ../riot-android/vector/src/main/res/values-ar/strings.xml        ./vector/src/main/res/values-ar/strings.xml
cp ../riot-android/vector/src/main/res/values-b+sr+Latn/strings.xml ./vector/src/main/res/values-b+sr+Latn/strings.xml
cp ../riot-android/vector/src/main/res/values-bg/strings.xml        ./vector/src/main/res/values-bg/strings.xml
cp ../riot-android/vector/src/main/res/values-bn-rIN/strings.xml    ./vector/src/main/res/values-bn-rIN/strings.xml
cp ../riot-android/vector/src/main/res/values-bs/strings.xml        ./vector/src/main/res/values-bs/strings.xml
cp ../riot-android/vector/src/main/res/values-ca/strings.xml        ./vector/src/main/res/values-ca/strings.xml
cp ../riot-android/vector/src/main/res/values-cs/strings.xml        ./vector/src/main/res/values-cs/strings.xml
cp ../riot-android/vector/src/main/res/values-da/strings.xml        ./vector/src/main/res/values-da/strings.xml
cp ../riot-android/vector/src/main/res/values-de/strings.xml        ./vector/src/main/res/values-de/strings.xml
cp ../riot-android/vector/src/main/res/values-el/strings.xml        ./vector/src/main/res/values-el/strings.xml
cp ../riot-android/vector/src/main/res/values-eo/strings.xml        ./vector/src/main/res/values-eo/strings.xml
cp ../riot-android/vector/src/main/res/values-es/strings.xml        ./vector/src/main/res/values-es/strings.xml
cp ../riot-android/vector/src/main/res/values-es-rMX/strings.xml    ./vector/src/main/res/values-es-rMX/strings.xml
cp ../riot-android/vector/src/main/res/values-eu/strings.xml        ./vector/src/main/res/values-eu/strings.xml
cp ../riot-android/vector/src/main/res/values-fa/strings.xml        ./vector/src/main/res/values-fa/strings.xml
cp ../riot-android/vector/src/main/res/values-fi/strings.xml        ./vector/src/main/res/values-fi/strings.xml
cp ../riot-android/vector/src/main/res/values-fr/strings.xml        ./vector/src/main/res/values-fr/strings.xml
cp ../riot-android/vector/src/main/res/values-fr-rCA/strings.xml    ./vector/src/main/res/values-fr-rCA/strings.xml
cp ../riot-android/vector/src/main/res/values-gl/strings.xml        ./vector/src/main/res/values-gl/strings.xml
cp ../riot-android/vector/src/main/res/values-hu/strings.xml        ./vector/src/main/res/values-hu/strings.xml
cp ../riot-android/vector/src/main/res/values-id/strings.xml        ./vector/src/main/res/values-id/strings.xml
cp ../riot-android/vector/src/main/res/values-in/strings.xml        ./vector/src/main/res/values-in/strings.xml
cp ../riot-android/vector/src/main/res/values-is/strings.xml        ./vector/src/main/res/values-is/strings.xml
cp ../riot-android/vector/src/main/res/values-it/strings.xml        ./vector/src/main/res/values-it/strings.xml
cp ../riot-android/vector/src/main/res/values-ja/strings.xml        ./vector/src/main/res/values-ja/strings.xml
cp ../riot-android/vector/src/main/res/values-ko/strings.xml        ./vector/src/main/res/values-ko/strings.xml
cp ../riot-android/vector/src/main/res/values-lv/strings.xml        ./vector/src/main/res/values-lv/strings.xml
cp ../riot-android/vector/src/main/res/values-nl/strings.xml        ./vector/src/main/res/values-nl/strings.xml
cp ../riot-android/vector/src/main/res/values-nn/strings.xml        ./vector/src/main/res/values-nn/strings.xml
cp ../riot-android/vector/src/main/res/values-pl/strings.xml        ./vector/src/main/res/values-pl/strings.xml
cp ../riot-android/vector/src/main/res/values-pt/strings.xml        ./vector/src/main/res/values-pt/strings.xml
cp ../riot-android/vector/src/main/res/values-pt-rBR/strings.xml    ./vector/src/main/res/values-pt-rBR/strings.xml
cp ../riot-android/vector/src/main/res/values-ro/strings.xml        ./vector/src/main/res/values-ro/strings.xml
cp ../riot-android/vector/src/main/res/values-ru/strings.xml        ./vector/src/main/res/values-ru/strings.xml
cp ../riot-android/vector/src/main/res/values-sk/strings.xml        ./vector/src/main/res/values-sk/strings.xml
cp ../riot-android/vector/src/main/res/values-sq/strings.xml        ./vector/src/main/res/values-sq/strings.xml
cp ../riot-android/vector/src/main/res/values-sr/strings.xml        ./vector/src/main/res/values-sr/strings.xml
cp ../riot-android/vector/src/main/res/values-te/strings.xml        ./vector/src/main/res/values-te/strings.xml
cp ../riot-android/vector/src/main/res/values-th/strings.xml        ./vector/src/main/res/values-th/strings.xml
cp ../riot-android/vector/src/main/res/values-tlh/strings.xml       ./vector/src/main/res/values-tlh/strings.xml
cp ../riot-android/vector/src/main/res/values-tr/strings.xml        ./vector/src/main/res/values-tr/strings.xml
cp ../riot-android/vector/src/main/res/values-uk/strings.xml        ./vector/src/main/res/values-uk/strings.xml
cp ../riot-android/vector/src/main/res/values-vls/strings.xml       ./vector/src/main/res/values-vls/strings.xml
cp ../riot-android/vector/src/main/res/values-zh-rCN/strings.xml    ./vector/src/main/res/values-zh-rCN/strings.xml
cp ../riot-android/vector/src/main/res/values-zh-rTW/strings.xml    ./vector/src/main/res/values-zh-rTW/strings.xml

echo
echo "Success!"
