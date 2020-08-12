#!/bin/bash

set -e

# Generate legacy icons for custom color icons in drawable-anydpi-v24

pushd "$(dirname "$(realpath "$0")")" > /dev/null

desired_accent=`cat vector/src/main/res/values/colors_sc.xml | grep '<color name="accent_sc">' | sed 's|.*>\(.*\)</.*|\1|'`

echo "Hardcode colorAccent to $desired_accent"

for f in vector/src/main/res/drawable-anydpi-v24/*; do

    target_file="vector/src/main/res/drawable/$(basename "$f")"
    echo "$f -> $target_file"
    sed "s|?\(android:\)\?\(attr/\)\?colorAccent|$desired_accent|g" "$f" > "$target_file"

done


popd > /dev/null
