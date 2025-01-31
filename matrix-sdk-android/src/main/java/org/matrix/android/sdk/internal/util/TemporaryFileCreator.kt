/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

internal class TemporaryFileCreator @Inject constructor(
        private val context: Context
) {
    suspend fun create(): File {
        return withContext(Dispatchers.IO) {
            File.createTempFile(UUID.randomUUID().toString(), null, context.cacheDir)
                    .apply { mkdirs() }
        }
    }
}
