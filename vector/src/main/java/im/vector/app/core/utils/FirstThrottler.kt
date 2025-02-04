/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.utils

import android.os.SystemClock

/**
 * Simple ThrottleFirst
 * See https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/throttleFirst.png
 */
class FirstThrottler(private val minimumInterval: Long = 800) {
    private var lastDate = 0L

    sealed class CanHandlerResult {
        object Yes : CanHandlerResult()
        data class No(val shouldWaitMillis: Long) : CanHandlerResult()

        fun waitMillis(): Long {
            return when (this) {
                Yes -> 0
                is No -> shouldWaitMillis
            }
        }
    }

    fun canHandle(): CanHandlerResult {
        val now = SystemClock.elapsedRealtime()
        val delaySinceLast = now - lastDate
        if (delaySinceLast > minimumInterval) {
            lastDate = now
            return CanHandlerResult.Yes
        }

        // Too soon
        return CanHandlerResult.No(minimumInterval - delaySinceLast)
    }
}
