#!/bin/bash

find_last_commit_for_title() {
    local title="$1"
    git log --oneline --author=SpiritCroc | grep "$title" | head -n 1 | sed 's| .*||'
}

revert_last() {
    local title="$1"
    shift
    git revert --no-edit `find_last_commit_for_title "$title"` $@
}

require_clean_git() {
    if [ "$NO_REQUIRE_CLEAN_GIT" = "y" ]; then
        return
    fi
    uncommitted=`git status --porcelain`
    if [ ! -z "$uncommitted" ]; then
        echo "Uncommitted changes are present, please commit first!"
        exit 1
    fi
}

upstream_latest_tag() {
    git describe --abbrev=0 upstream/main --tags
}
upstream_previous_tag() {
    #git describe --abbrev=0 `upstream_latest_tag`~1 --tags
    #downstream_latest_tag | sed 's|sc_\(v.*\).sc.*|\1|'
    git log | grep "Merge tag 'v.*' into sc" | head -n 1 |sed "s|.*Merge tag '\\(v.*\\)' into sc|\1|"
}
downstream_latest_tag() {
    local commit="HEAD"
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


# Opposite to restore_sc
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

# Oposite to restore_upstream
restore_sc() {
    local f="$(basename "$1")"
    local path="$(dirname "$1")"
    local sc_f="tmp_sc_$f"
    local upstream_f="upstream_$f"
    if [ -e "$path/$f" ]; then
        mv "$path/$f" "$path/$upstream_f"
    fi
    if [ -e "$path/$sc_f" ]; then
        mv "$path/$sc_f" "$path/$f"
    fi
}

await_edit() {
    local f="$1"
    shift 1
    if [ ! -z "$GRAPHICAL_EDITOR" ]; then
        $GRAPHICAL_EDITOR "$f"
        read -p "Press enter when done"
    elif [ ! -z "$VISUAL" ]; then
        $VISUAL "$f"
    elif [ ! -z "$EDITOR" ]; then
        $EDITOR "$f"
    else
        read -p "No editor set, please edit $f manually, and press enter when done"
    fi
}
