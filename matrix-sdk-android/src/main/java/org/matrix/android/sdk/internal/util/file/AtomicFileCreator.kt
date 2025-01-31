/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util.file

import timber.log.Timber
import java.io.File

internal class AtomicFileCreator(private val file: File) {
    val partFile = File(file.parentFile, "${file.name}.part")

    init {
        if (file.exists()) {
            Timber.w("## AtomicFileCreator: target file ${file.path} exists, it should not happen.")
        }
        if (partFile.exists()) {
            Timber.d("## AtomicFileCreator: discard aborted part file ${partFile.path}")
            // No need to delete the file, we will overwrite it
        }
    }

    fun cancel() {
        partFile.delete()
    }

    fun commit() {
        partFile.renameTo(file)
    }
}
