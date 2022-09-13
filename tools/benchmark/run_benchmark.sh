#!/usr/bin/env bash

#
# Copyright 2021 New Vector Ltd
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
