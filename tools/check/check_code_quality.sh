#!/usr/bin/env bash

# Copyright 2019-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
# Please see LICENSE files in the repository root for full details.

#######################################################################################################################
# Check drawable quantity
#######################################################################################################################

echo "Check drawable quantity"

numberOfFiles1=`ls -1U ./vector/src/main/res/drawable-hdpi | wc -l | sed  "s/ //g"`
numberOfFiles2=`ls -1U ./vector/src/main/res/drawable-mdpi | wc -l | sed  "s/ //g"`
numberOfFiles3=`ls -1U ./vector/src/main/res/drawable-xhdpi | wc -l | sed  "s/ //g"`
numberOfFiles4=`ls -1U ./vector/src/main/res/drawable-xxhdpi | wc -l | sed  "s/ //g"`
numberOfFiles5=`ls -1U ./vector/src/main/res/drawable-xxxhdpi | wc -l | sed  "s/ //g"`

if [[ ${numberOfFiles1} -eq ${numberOfFiles5} ]] && [[ ${numberOfFiles2} -eq ${numberOfFiles5} ]] && [[ ${numberOfFiles3} -eq ${numberOfFiles5} ]] && [[ ${numberOfFiles4} -eq ${numberOfFiles5} ]]; then
   resultNbOfDrawable=0
   echo "OK"
else
   # Ignore for the moment
   # resultNbOfDrawable=1
   resultNbOfDrawable=0
   echo "ERROR, missing drawable alternative."
fi

echo

#######################################################################################################################
# Search forbidden pattern
#######################################################################################################################

searchForbiddenStringsScript=./tmp/search_forbidden_strings.pl

if [[ -f ${searchForbiddenStringsScript} ]]; then
  echo "${searchForbiddenStringsScript} already there"
else
  mkdir tmp
  echo "Get the script"
  wget https://raw.githubusercontent.com/matrix-org/matrix-dev-tools/develop/bin/search_forbidden_strings.pl -O ${searchForbiddenStringsScript}
fi

if [[ -x ${searchForbiddenStringsScript} ]]; then
  echo "${searchForbiddenStringsScript} is already executable"
else
  echo "Make the script executable"
  chmod u+x ${searchForbiddenStringsScript}
fi

echo
echo "Search for forbidden patterns in code..."

${searchForbiddenStringsScript} ./tools/check/forbidden_strings_in_code.txt \
    ./matrix-sdk-android/src/main/java \
    ./matrix-sdk-android-flow/src/main/java \
    ./library/core-utils/src/main/java \
    ./library/external/jsonviewer/src/main/java \
    ./library/ui-styles/src/main/java \
    ./vector/src/main/java \
    ./vector-app/src/debug/java \
    ./vector-app/src/fdroid/java \
    ./vector-app/src/gplay/java \
    ./vector-app/src/main/java \
    ./vector-app/src/release/java

resultForbiddenStringInCode=$?

echo
echo "Search for forbidden patterns specific for SDK code..."

${searchForbiddenStringsScript} ./tools/check/forbidden_strings_in_code_sdk.txt \
    ./matrix-sdk-android/src \
    ./matrix-sdk-android-flow/src

resultForbiddenStringInCodeSdk=$?

echo
echo "Search for forbidden patterns specific for App code..."

${searchForbiddenStringsScript} ./tools/check/forbidden_strings_in_code_app.txt \
    ./library/core-utils/src/main/java \
    ./library/external/jsonviewer/src/main/java \
    ./library/ui-styles/src/main/java \
    ./vector/src/main/java \
    ./vector-app/src/debug/java \
    ./vector-app/src/fdroid/java \
    ./vector-app/src/gplay/java \
    ./vector-app/src/main/java \
    ./vector-app/src/release/java

resultForbiddenStringInCodeApp=$?

echo
echo "Search for forbidden patterns in resources..."

${searchForbiddenStringsScript} ./tools/check/forbidden_strings_in_resources.txt \
    ./library/ui-styles/src/main/res/values \
    ./vector/src/main/res/color \
    ./vector/src/main/res/layout \
    ./vector/src/main/res/values \
    ./vector/src/main/res/xml \
    ./vector-app/src/main/res/values

resultForbiddenStringInResource=$?

echo
echo "Search for forbidden patterns in layouts..."

${searchForbiddenStringsScript} ./tools/check/forbidden_strings_in_layout.txt \
    ./vector/src/main/res/layout

resultForbiddenStringInLayout=$?

#######################################################################################################################
# Check files with long lines
#######################################################################################################################

checkLongFilesScript=./tmp/check_long_files.pl

if [[ -f ${checkLongFilesScript} ]]; then
  echo "${checkLongFilesScript} already there"
else
  mkdir tmp
  echo "Get the script"
  wget https://raw.githubusercontent.com/matrix-org/matrix-dev-tools/develop/bin/check_long_files.pl -O ${checkLongFilesScript}
fi

if [[ -x ${checkLongFilesScript} ]]; then
  echo "${checkLongFilesScript} is already executable"
else
  echo "Make the script executable"
  chmod u+x ${checkLongFilesScript}
fi

maxLines=2800

echo
echo "Search for kotlin files with more than ${maxLines} lines..."

${checkLongFilesScript} ${maxLines} \
    ./matrix-sdk-android/src/main/java \
    ./matrix-sdk-android-flow/src/main/java \
    ./library/core-utils/src/main/java \
    ./library/external/jsonviewer/src/main/java \
    ./library/ui-styles/src/main/java \
    ./vector/src/androidTest/java \
    ./vector/src/main/java \
    ./vector/src/sharedTest/java \
    ./vector/src/test/java \
    ./vector-app/src/androidTest/java \
    ./vector-app/src/debug/java \
    ./vector-app/src/fdroid/java \
    ./vector-app/src/gplay/java \
    ./vector-app/src/main/java \
    ./vector-app/src/release/java


resultLongFiles=$?

#######################################################################################################################
# search png in drawable folder
#######################################################################################################################

echo
echo "Search for png files in /drawable..."

ls -1U ./vector/src/main/res/drawable/*.png
resultTmp=$?

ls -1U ./vector-app/src/main/res/drawable/*.png
resultTmp2=$?

# Inverse the result, cause no file found is an error for ls but this is what we want!
if [[ ${resultTmp} -eq 0 ]] || [[ ${resultTmp2} -eq 0 ]]; then
   echo "ERROR, png files detected in /drawable"
   resultPngInDrawable=1
else
   echo "OK"
   resultPngInDrawable=0
fi

echo

if [[ ${resultNbOfDrawable} -eq 0 ]] \
   && [[ ${resultForbiddenStringInCode} -eq 0 ]] \
   && [[ ${resultForbiddenStringInCodeSdk} -eq 0 ]] \
   && [[ ${resultForbiddenStringInCodeApp} -eq 0 ]] \
   && [[ ${resultForbiddenStringInResource} -eq 0 ]] \
   && [[ ${resultForbiddenStringInLayout} -eq 0 ]] \
   && [[ ${resultLongFiles} -eq 0 ]] \
   && [[ ${resultPngInDrawable} -eq 0 ]]; then
   echo "MAIN OK"
else
   echo "❌ MAIN ERROR"
   exit 1
fi
