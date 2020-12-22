#!/bin/bash

set -e

mydir="$(dirname "$(realpath "$0")")"
source "$mydir/merge_helpers.sh"

# Require clean git state
require_clean_git

# Revert Schildi's upstream string changes
git checkout `upstream_previous_tag` -- `find "$mydir/vector/src/main/res" -name strings.xml`
git commit -m "Automatic revert to unchanged upstream strings, pt.1"

# Keep in sync with post_merge.sh!
restore_upstream .github
restore_upstream fastlane
restore_upstream README.md

git add -A
git commit -m "[TMP] Automatic upstream merge preparation"
