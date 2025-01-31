/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import androidx.annotation.WorkerThread
import java.io.File
import java.io.InputStream

/**
 * Save an input stream to a file with Okio.
 */
@WorkerThread
internal fun writeToFile(inputStream: InputStream, outputFile: File) {
    // Ensure the parent folder exists, else it will crash
    outputFile.parentFile?.mkdirs()

    outputFile.outputStream().use {
        inputStream.copyTo(it)
    }
}
