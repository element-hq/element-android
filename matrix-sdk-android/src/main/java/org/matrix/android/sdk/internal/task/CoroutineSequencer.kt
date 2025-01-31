/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.task

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * This class intends to be used to ensure suspendable methods are played sequentially all the way long.
 */
internal interface CoroutineSequencer {
    /**
     * @param T generic type
     * @param block the suspendable block to execute
     * @return the result of the block
     */
    suspend fun <T> post(block: suspend () -> T): T
}

internal open class SemaphoreCoroutineSequencer : CoroutineSequencer {

    // Permits 1 suspend function at a time.
    private val semaphore = Semaphore(1)

    override suspend fun <T> post(block: suspend () -> T): T {
        return semaphore.withPermit {
            block()
        }
    }
}
