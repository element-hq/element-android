#!/usr/bin/env bash

# Copyright 2021-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
# Please see LICENSE files in the repository root for full details.

if ! command -v gradle-profiler &> /dev/null
then
    echo "gradle-profiler could not be found https://github.com/gradle/gradle-profiler"
    exit
fi

gradle-profiler \
  --benchmark \
  --project-dir . \
  --scenario-file tools/benchmark/benchmark.profile \
  --output-dir benchmark-out/output \
  --gradle-user-home benchmark-out/gradle-home \
  --warmups 3 \
  --iterations 3 \
  $1
