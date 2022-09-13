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

clean_assemble {
  tasks = ["clean", ":vector:assembleGPlayDebug"]
}

clean_assemble_build_cache {
  tasks = ["clean", ":vector:assembleGPlayDebug"]
  gradle-args = ["--build-cache"]
}

clean_assemble_without_cache {
  tasks = ["clean", ":vector:assembleGPlayDebug"]
  gradle-args = ["--no-build-cache"]
}

incremental_assemble_sdk_abi {
  tasks = [":vector:assembleGPlayDebug"]
  apply-abi-change-to = "matrix-sdk-android/src/main/java/org/matrix/android/sdk/api/Matrix.kt"
}

incremental_assemble_sdk_no_abi {
  tasks = [":vector:assembleGPlayDebug"]
  apply-non-abi-change-to = "matrix-sdk-android/src/main/java/org/matrix/android/sdk/api/Matrix.kt"
}
