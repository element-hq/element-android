#
# Copyright (c) 2022 New Vector Ltd
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

#! /bin/bash

# Compare the output of `git ls-files ':(attr:filter=lfs)'` against `git lfs ls-files`
# If there's no diff we assume the files have been committed using git lfs
diff <(git ls-files ':(attr:filter=lfs)' | sort) <(git lfs ls-files -n | sort) >/dev/null

ret=$?

if [[ $ret -ne 0 ]]; then
  echo >&2 "Detected files committed without using Git LFS."
  echo >&2 "Install git lfs (eg brew install git-lfs) and run 'git lfs install --local' within the root repository directory and re-commit your files."
  exit 1
fi


