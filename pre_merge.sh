#!/bin/bash

set -e

# Require clean git state
uncommitted=`git status --porcelain`
if [ ! -z "$uncommitted" ]; then
    echo "Uncommitted changes are present, please commit first!"
    exit 1
fi

find_last_commit_for_title() {
    local title="$1"
    git log --oneline --author=SpiritCroc | grep "$title" | head -n 1 | sed 's| .*||'
}

revert_last() {
    local title="$1"
    git revert --no-edit `find_last_commit_for_title "$title"`
}

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

restore_upstream fastlane
restore_upstream README.md
git add -A
git commit -m "[TMP] Automatic upstream merge preparation"
