#!/bin/bash

set -e

mydir="$(dirname "$(realpath "$0")")"
source "$mydir/merge_helpers.sh"

# Require clean git state
require_clean_git

# Oposite of restore_sc in post_merge.sh
restore_upstream() {
    local f="$(basename "$1")"
    local path="$(dirname "$1")"
    local sc_f="tmp_sc_$f"
    local upstream_f="upstream_$f"
    mv "$path/$f" "$path/$sc_f"
    if [ -e "$path/$upstream_f" ]; then
        mv "$path/$upstream_f" "$path/$f"
    fi
}

revert_last 'Resolve required manual intervention in german strings'
revert_last 'Automatic SchildiChat string correction'

# Keep in sync with post_merge.sh!
restore_upstream .github
restore_upstream fastlane
restore_upstream README.md

git add -A
git commit -m "[TMP] Automatic upstream merge preparation"
