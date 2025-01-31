/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import androidx.work.WorkManager
import org.matrix.android.sdk.api.util.Cancelable
import java.util.UUID

internal class CancelableWork(
        private val workManager: WorkManager,
        private val workId: UUID
) : Cancelable {

    override fun cancel() {
        workManager.cancelWorkById(workId)
    }
}
