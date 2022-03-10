#!/usr/bin/env bash

#
# Copyright (c) 2021 New Vector Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e
echo "Converting file $1"
file=$(echo $1 | sed 's/\.[^.]*$//')
ffmpeg -i $1 -filter_complex "[0:v] fps=12,scale=480:-1,split [a][b];[a] palettegen [p];[b][p] paletteuse" $file-tmp.gif
echo "Converting to Gif"
gifsicle -O3 --lossy=80 -o $file.gif $file-tmp.gif
rm $file-tmp.gif
echo "Done, $file.gif has been generated"
