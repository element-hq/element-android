/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
