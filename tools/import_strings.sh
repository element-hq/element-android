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

echo "Copy strings to SDK"
echo

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
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-zh-rCN/strings.xml ./matrix-sdk-android/src/main/res/values-zh-rCN/strings.xml
cp ../matrix-android-sdk/matrix-sdk/src/main/res/values-zh-rTW/strings.xml ./matrix-sdk-android/src/main/res/values-zh-rTW/strings.xml

echo
echo "Copy strings to Riot"
echo

cp ../riot-android/vector/src/main/res/values/strings.xml        ./app/src/main/res/values/strings.xml
cp ../riot-android/vector/src/main/res/values-ar/strings.xml     ./app/src/main/res/values-ar/strings.xml
cp ../riot-android/vector/src/main/res/values-bg/strings.xml     ./app/src/main/res/values-bg/strings.xml
cp ../riot-android/vector/src/main/res/values-bn-rIN/strings.xml ./app/src/main/res/values-bn-rIN/strings.xml
cp ../riot-android/vector/src/main/res/values-bs/strings.xml     ./app/src/main/res/values-bs/strings.xml
cp ../riot-android/vector/src/main/res/values-ca/strings.xml     ./app/src/main/res/values-ca/strings.xml
cp ../riot-android/vector/src/main/res/values-cs/strings.xml     ./app/src/main/res/values-cs/strings.xml
cp ../riot-android/vector/src/main/res/values-da/strings.xml     ./app/src/main/res/values-da/strings.xml
cp ../riot-android/vector/src/main/res/values-de/strings.xml     ./app/src/main/res/values-de/strings.xml
cp ../riot-android/vector/src/main/res/values-el/strings.xml     ./app/src/main/res/values-el/strings.xml
cp ../riot-android/vector/src/main/res/values-eo/strings.xml     ./app/src/main/res/values-eo/strings.xml
cp ../riot-android/vector/src/main/res/values-es/strings.xml     ./app/src/main/res/values-es/strings.xml
cp ../riot-android/vector/src/main/res/values-es-rMX/strings.xml ./app/src/main/res/values-es-rMX/strings.xml
cp ../riot-android/vector/src/main/res/values-eu/strings.xml     ./app/src/main/res/values-eu/strings.xml
cp ../riot-android/vector/src/main/res/values-fa/strings.xml     ./app/src/main/res/values-fa/strings.xml
cp ../riot-android/vector/src/main/res/values-fi/strings.xml     ./app/src/main/res/values-fi/strings.xml
cp ../riot-android/vector/src/main/res/values-fr/strings.xml     ./app/src/main/res/values-fr/strings.xml
cp ../riot-android/vector/src/main/res/values-fr-rCA/strings.xml ./app/src/main/res/values-fr-rCA/strings.xml
cp ../riot-android/vector/src/main/res/values-gl/strings.xml     ./app/src/main/res/values-gl/strings.xml
cp ../riot-android/vector/src/main/res/values-hu/strings.xml     ./app/src/main/res/values-hu/strings.xml
cp ../riot-android/vector/src/main/res/values-id/strings.xml     ./app/src/main/res/values-id/strings.xml
cp ../riot-android/vector/src/main/res/values-in/strings.xml     ./app/src/main/res/values-in/strings.xml
cp ../riot-android/vector/src/main/res/values-is/strings.xml     ./app/src/main/res/values-is/strings.xml
cp ../riot-android/vector/src/main/res/values-it/strings.xml     ./app/src/main/res/values-it/strings.xml
cp ../riot-android/vector/src/main/res/values-ja/strings.xml     ./app/src/main/res/values-ja/strings.xml
cp ../riot-android/vector/src/main/res/values-ko/strings.xml     ./app/src/main/res/values-ko/strings.xml
cp ../riot-android/vector/src/main/res/values-lv/strings.xml     ./app/src/main/res/values-lv/strings.xml
cp ../riot-android/vector/src/main/res/values-nl/strings.xml     ./app/src/main/res/values-nl/strings.xml
cp ../riot-android/vector/src/main/res/values-nn/strings.xml     ./app/src/main/res/values-nn/strings.xml
cp ../riot-android/vector/src/main/res/values-pl/strings.xml     ./app/src/main/res/values-pl/strings.xml
cp ../riot-android/vector/src/main/res/values-pt/strings.xml     ./app/src/main/res/values-pt/strings.xml
cp ../riot-android/vector/src/main/res/values-pt-rBR/strings.xml ./app/src/main/res/values-pt-rBR/strings.xml
cp ../riot-android/vector/src/main/res/values-ru/strings.xml     ./app/src/main/res/values-ru/strings.xml
cp ../riot-android/vector/src/main/res/values-sk/strings.xml     ./app/src/main/res/values-sk/strings.xml
cp ../riot-android/vector/src/main/res/values-sq/strings.xml     ./app/src/main/res/values-sq/strings.xml
cp ../riot-android/vector/src/main/res/values-te/strings.xml     ./app/src/main/res/values-te/strings.xml
cp ../riot-android/vector/src/main/res/values-th/strings.xml     ./app/src/main/res/values-th/strings.xml
cp ../riot-android/vector/src/main/res/values-tlh/strings.xml    ./app/src/main/res/values-tlh/strings.xml
cp ../riot-android/vector/src/main/res/values-tr/strings.xml     ./app/src/main/res/values-tr/strings.xml
cp ../riot-android/vector/src/main/res/values-uk/strings.xml     ./app/src/main/res/values-uk/strings.xml
cp ../riot-android/vector/src/main/res/values-zh-rCN/strings.xml ./app/src/main/res/values-zh-rCN/strings.xml
cp ../riot-android/vector/src/main/res/values-zh-rTW/strings.xml ./app/src/main/res/values-zh-rTW/strings.xml


echo
echo "Success!"
