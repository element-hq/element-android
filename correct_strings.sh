#!/bin/bash

mydir="$(dirname "$(realpath "$0")")"

find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|Element|SchildiChat|g' '{}' \;
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|SchildiChat Web|Element Web|g' '{}' \;
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|SchildiChat iOS|Element iOS|g' '{}' \;
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|\("use_latest_riot">.*\)SchildiChat\(.*</string>\)|\1Element\2|g' '{}' \;

# Requires manual intervention for correct grammar
sed -i 's|!nnen|wolpertinger|g' "$mydir/vector/src/main/res/values-de/strings.xml"
sed -i 's|!n|schlumpfwesen|g' "$mydir/vector/src/main/res/values-de/strings.xml"
