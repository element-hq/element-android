#!/bin/bash

find_last_commit_for_title() {
    local title="$1"
    git log --oneline --author=SpiritCroc | grep "$title" | head -n 1 | sed 's| .*||'
}

revert_last() {
    local title="$1"
    git revert --no-edit `find_last_commit_for_title "$title"`
}

require_clean_git() {
    uncommitted=`git status --porcelain`
    if [ ! -z "$uncommitted" ]; then
        echo "Uncommitted changes are present, please commit first!"
        exit 1
    fi
}

upstream_latest_tag() {
    git describe --abbrev=0 upstream/master --tags
}
upstream_previous_tag() {
    git describe --abbrev=0 `upstream_latest_tag`~1 --tags
}
downstream_latest_tag() {
    local commit="sc"
    while true; do
        local tag=`git describe --abbrev=0 "$commit" --tags`
        if [[ "$tag" =~ "sc_" ]]; then
            echo "$tag"
            break
        else
            commit="$commit^1"
        fi
    done
}

upstream_diff() {
    local latest_tag=`upstream_latest_tag`
    local previous_tag=`upstream_previous_tag`
    git diff "$previous_tag".."$latest_tag" "$@"
}
upstream_log() {
    local latest_tag=`upstream_latest_tag`
    local previous_tag=`upstream_previous_tag`
    git log "$previous_tag".."$latest_tag" "$@"
}

downstream_upstream_diff() {
    local previous_tag=`upstream_previous_tag`
    local downstream_tag=`downstream_latest_tag`
    git diff "$previous_tag".."$downstream_latest_tag" "$@"
}
