#!/bin/bash

set -e

mydir="$(dirname "$(realpath "$0")")"
source "$mydir/merge_helpers.sh"

pushd "$mydir" > /dev/null

# Require clean git state
uncommitted=`git status --porcelain`
if [ ! -z "$uncommitted" ]; then
    echo "Uncommitted changes are present, please commit first!"
    exit 1
fi

mydir="."

# Element -> SchildiChat
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|Element|SchildiChat|g' '{}' \;
# Restore Element where it makes sense
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's/SchildiChat \(Web\|iOS\|Desktop\)/Element \1/g' '{}' \;
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|SchildiChat Matrix Services|Element Matrix Services|g' '{}' \;
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|\("use_latest_riot">.*\)SchildiChat\(.*</string>\)|\1Element\2|g' '{}' \;
find "$mydir/vector/src/main/res" -name strings.xml -exec \
    sed -i 's|\("use_other_session_content_description">.*\)SchildiChat\(.*SchildiChat.*</string>\)|\1SchildiChat/Element\2|' '{}' \;

unpatched_strings_file=.tmp_unpatched_strings
new_patched_strings_file=.tmp_new_patched_strings

patch_file_updated=0

# Requires manual intervention for correct grammar
#for strings_de in "$mydir/vector/src/main/res/values-de/strings.xml" "$mydir/matrix-sdk-android/src/main/res/values-de/strings.xml"; do
for strings_de in "$mydir/vector/src/main/res/values-de/strings.xml"; do
    echo "Apply known language fixes to $strings_de..."
    source ./correct_strings_de.sh
    while grep -q "wolpertinger\|schlumpfwesen" "$strings_de"; do
        grep "wolpertinger\|schlumpfwesen" "$strings_de" | sed "s|.*>\\(.*\\)<.*|\1|" > "$unpatched_strings_file"
        echo "Unpatched strings discovered, please shorten detection strings as appropriate:"
        await_edit "$unpatched_strings_file"
        cp "$unpatched_strings_file" "$new_patched_strings_file"
        echo "Now enter the patched strings:"
        await_edit "$new_patched_strings_file"
        echo "Rewriting patch file..."
        while read old_str <&3 && read new_str <&4; do
            echo "sed -i 's|$old_str|$new_str|g' \"\$strings_de\"" >> ./correct_strings_de.sh
        done 3<"$unpatched_strings_file" 4<"$new_patched_strings_file"
        rm "$unpatched_strings_file" "$new_patched_strings_file"
        echo "Re-applying language fix..."
        source ./correct_strings_de.sh
        patch_file_updated=1
    done
done

if ((patch_file_updated)); then
    git add ./correct_strings_de.sh
    git commit -m "Update string correction"
fi

# Remove Triple-T stuff to avoid using them in F-Droid
rm -rf "$mydir/vector/src/main/play/listings"

git add -A
git commit -m "Automatic SchildiChat string correction"

popd > /dev/null

echo "Seems like language is up-to-date :)"
