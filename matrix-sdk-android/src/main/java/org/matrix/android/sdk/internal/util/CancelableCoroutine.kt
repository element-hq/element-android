/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import kotlinx.coroutines.Job
import org.matrix.android.sdk.api.util.Cancelable

internal fun Job.toCancelable(): Cancelable {
    return CancelableCoroutine(this)
}

/**
 * Private, use the extension above.
 */
private class CancelableCoroutine(private val job: Job) : Cancelable {

    override fun cancel() {
        if (!job.isCancelled) {
            job.cancel()
        }
    }
}
