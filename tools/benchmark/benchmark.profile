# Copyright 2021-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
# Please see LICENSE files in the repository root for full details.

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
