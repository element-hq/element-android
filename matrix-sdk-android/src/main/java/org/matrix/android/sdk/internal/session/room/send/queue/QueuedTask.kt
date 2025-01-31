/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.send.queue

import org.matrix.android.sdk.api.util.Cancelable
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * @property queueIdentifier String value to identify a unique Queue
 * @property taskIdentifier String value to identify a unique Task. Should be different from queueIdentifier
 */
internal abstract class QueuedTask(
        val queueIdentifier: String,
        val taskIdentifier: String
) : Cancelable {

    override fun toString() = "${javaClass.simpleName} queueIdentifier: $queueIdentifier, taskIdentifier:  $taskIdentifier)"

    var retryCount = AtomicInteger(0)

    private var hasBeenCancelled: Boolean = false

    suspend fun execute() {
        if (!isCancelled()) {
            Timber.v("Execute: $this start")
            doExecute()
            Timber.v("Execute: $this finish")
        }
    }

    abstract suspend fun doExecute()

    abstract fun onTaskFailed()

    open fun isCancelled() = hasBeenCancelled

    final override fun cancel() {
        hasBeenCancelled = true
    }
}
