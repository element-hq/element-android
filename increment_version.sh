#!/bin/bash

set -e

mydir="$(dirname "$(realpath "$0")")"

source "$mydir/merge_helpers.sh"

if [ "$1" = "preview" ]; then
    preview=1
    shift
else
    preview=0
    require_clean_git
fi


if [ "$1" = "test" ]; then
    release_type="test"
    previousTestVersionCode="$2"
else
    release_type="normal"
fi


pushd "$mydir" > /dev/null

last_tag=`downstream_latest_tag`

build_gradle="vector/build.gradle"

get_prop() {
    local prop="$1"
    cat "$build_gradle" | grep "$prop = " | sed "s|$prop = ||"
}
set_prop() {
    local prop="$1"
    local value="$2"
    if grep -q "$prop =" "$build_gradle"; then
        local equals="= "
        local not_equals=""
    else
        local equals=""
        # Don't touch lines that have an equals in it, but not for this prop
        local not_equals="/=/! "
    fi
    sed -i "$not_equals""s|\($prop $equals\).*|\1$value|g" "$build_gradle"
}

# Legacy versioning, based on Element's version codes
#calculate_version_code() {
#    echo "(($versionMajor * 10000 + $versionMinor * 100 + $versionPatch + $scVersion) + 4000000) * 10" | bc
#}


#
# Increase version
#

versionMajor=`get_prop ext.versionMajor`
versionMinor=`get_prop ext.versionMinor`
versionPatch=`get_prop ext.versionPatch`
scVersion=`get_prop ext.scVersion`

previousVersionCode=`grep '^        versionCode ' "$build_gradle" | sed 's|^        versionCode ||'`
# Legacy versioning, based on Element's version codes
#versionCode=`calculate_version_code`
#if [ "$release_type" = "test" ]; then
#    if [ ! -z "$previousTestVersionCode" ]; then
#        previousVersionCode=$((previousVersionCode > previousTestVersionCode ? previousVersionCode : previousTestVersionCode))
#    fi
#    versionCode=$((previousVersionCode + 1))
#elif [ "$versionCode" = "$previousVersionCode" ]; then
#    ((scVersion++))
#    echo "Increase downstream version to $scVersion"
#    versionCode=`calculate_version_code`
#else
#    echo "Upstream version upgrade, no need to change downstream version"
#fi
# New versioning: versionCode incremented independently of versionName, and always increment scVersion
((scVersion++))
if [ "$release_type" = "test" ]; then
    if [ ! -z "$previousTestVersionCode" ]; then
        testVersionCount=$((previousVersionCode > previousTestVersionCode ? 1 : (previousTestVersionCode - previousVersionCode + 1)))
        previousVersionCode=$((previousVersionCode > previousTestVersionCode ? previousVersionCode : previousTestVersionCode))
    else
        testVersionCount=1
    fi
    versionCode=$((previousVersionCode + 1))
else
    versionCode=$((previousVersionCode + 10))
fi


version="$versionMajor.$versionMinor.$versionPatch.sc$scVersion"

if [ "$release_type" = "test" ]; then
    version="$version-test$testVersionCount"
fi

new_tag="sc_v$version"

if ((preview)); then
    echo "versionCode $versionCode"
    echo "versionName $version"
    exit 0
fi

set_prop "ext.scVersion" "$scVersion"
set_prop "versionCode" "$versionCode"
set_prop "versionName" "\"$version\""



#
# Generate changelog
#

changelog_dir=fastlane/metadata/android/en-US/changelogs
changelog_file="$changelog_dir/$versionCode.txt"
mkdir -p "$changelog_dir"
git log --reverse --pretty=format:"- %s" "$last_tag".. --committer="$(git config user.name)" \
    | grep -v 'Automatic revert to unchanged upstream strings' \
    | grep -v 'Automatic upstream merge preparation' \
    | sed "s|Merge tag '\\(.*\\)' into sc|Update codebase to Element \1|" \
    | grep -v "Automatic color correction" \
    | grep -v "Automatic upstream merge postprocessing" \
    | grep -v "Automatic SchildiChat string correction" \
    | grep -v 'merge_helpers\|README\|increment_version' \
    > "$changelog_file"
if [ "$release_type" != "test" ]; then
    $EDITOR "$changelog_file" || true
    read -p "Press enter when changelog is done"
fi

git add -A
if [ "$release_type" = "test" ]; then
    git commit -m "Test version $versionCode"
else
    git commit -m "Increment version"
    git tag "$new_tag"
fi

popd > /dev/null
