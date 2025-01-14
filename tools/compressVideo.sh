#!/usr/bin/env bash

# Copyright 2021-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
# Please see LICENSE files in the repository root for full details.

set -e
echo "Converting file $1"
file=$(echo $1 | sed 's/\.[^.]*$//')
ffmpeg -i $1 -filter_complex "[0:v] fps=12,scale=480:-1,split [a][b];[a] palettegen [p];[b][p] paletteuse" $file-tmp.gif
echo "Converting to Gif"
gifsicle -O3 --lossy=80 -o $file.gif $file-tmp.gif
rm $file-tmp.gif
echo "Done, $file.gif has been generated"
