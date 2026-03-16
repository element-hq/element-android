/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.translation

import kotlinx.coroutines.sync.Semaphore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRateLimiter @Inject constructor() {
    private val semaphore = Semaphore(3)

    suspend fun <T> execute(block: suspend () -> T): T {
        semaphore.acquire()
        return try {
            block()
        } finally {
            semaphore.release()
        }
    }
}
