#! /bin/bash

# Copyright 2022-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
# Please see LICENSE files in the repository root for full details.
#
# Based on https://cashapp.github.io/paparazzi/#git-lfs

# Compare the output of `git ls-files ':(attr:filter=lfs)'` against `git lfs ls-files`
# If there's no diff we assume the files have been committed using git lfs
diff <(git ls-files ':(attr:filter=lfs)' | sort) <(git lfs ls-files -n | sort) >/dev/null

ret=$?
if [[ $ret -ne 0 ]]; then
  echo >&2 "Detected files committed without using Git LFS."
  echo >&2 "Install git lfs (eg brew install git-lfs) and run 'git lfs install --local' within the root repository directory and re-commit your files."
  exit 1
fi
