/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import java.util.Timer
import java.util.TimerTask
import kotlin.time.Duration

/**
 * Store an object T for a specific period of time.
 * @param T type of the data to store
 * @property delay delay to keep the data
 */
open class TemporaryStore<T>(private val delay: Duration) {

    private var timer: Timer? = null

    var data: T? = null
        set(value) {
            timer?.cancel()
            field = value
            if (value != null) {
                timer = Timer().also {
                    it.schedule(object : TimerTask() {
                        override fun run() {
                            field = null
                        }
                    }, delay.inWholeMilliseconds)
                }
            }
        }
}
