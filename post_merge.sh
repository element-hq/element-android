#!/bin/sh

set -e

mydir="$(dirname "$(realpath "$0")")"
source "$mydir/merge_helpers.sh"

# Require clean git state
require_clean_git

# Color corrections | TODO more?
sed -i 's|@color/riotx_accent|?colorAccent|g' vector/src/main/res/layout/*
uncommitted=`git status --porcelain`
if [ -z "$uncommitted" ]; then
    echo "Seems like colors are still fine :)"
else
    git add -A
    git commit -m 'Automatic color correction'
fi

# Keep in sync with pre_merge.sh!
restore_sc README.md
restore_sc fastlane
restore_sc .github

git add -A
git commit -m "Automatic upstream merge postprocessing"

$mydir"/correct_strings.sh
