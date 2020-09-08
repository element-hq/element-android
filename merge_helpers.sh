#!/bin/bash

upstream_latest_tag() {
    git describe upstream/master --tags
}
upstream_previous_tag() {
    git describe `upstream_latest_tag`~1 --tags
}
upstream_diff() {
    local latest_tag=`upstream_latest_tag`
    local previous_tag=`upstream_previous_tag`
    git diff "$previous_tag".."$latest_tag" "$@"
}
