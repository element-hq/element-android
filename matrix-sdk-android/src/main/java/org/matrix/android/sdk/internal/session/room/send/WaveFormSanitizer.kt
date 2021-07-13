/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.send

import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ceil

internal class WaveFormSanitizer @Inject constructor() {
    private companion object {
        const val MIN_NUMBER_OF_VALUES = 30
        const val MAX_NUMBER_OF_VALUES = 120

        const val MAX_VALUE = 1024
    }

    /**
     * The array should have no less than 30 elements and no more than 120.
     * List of integers between zero and 1024, inclusive.
     */
    fun sanitize(waveForm: List<Int>?): List<Int>? {
        if (waveForm.isNullOrEmpty()) {
            return null
        }

        // Limit the number of items
        val result = mutableListOf<Int>()
        if (waveForm.size < MIN_NUMBER_OF_VALUES) {
            // Repeat the same value to have at least 30 items
            val repeatTimes = ceil(MIN_NUMBER_OF_VALUES / waveForm.size.toDouble()).toInt()
            waveForm.map { value ->
                repeat(repeatTimes) {
                    result.add(value)
                }
            }
        } else if (waveForm.size > MAX_NUMBER_OF_VALUES) {
            val keepOneOf = ceil(waveForm.size.toDouble() / MAX_NUMBER_OF_VALUES).toInt()
            waveForm.mapIndexed { idx, value ->
                if (idx % keepOneOf == 0) {
                    result.add(value)
                }
            }
        } else {
            result.addAll(waveForm)
        }

        // OK, ensure all items are positive
        val limited = result.map {
            abs(it)
        }

        // Ensure max is not above MAX_VALUE
        val max = limited.maxOrNull() ?: MAX_VALUE

        val final = if (max > MAX_VALUE) {
            // Reduce the range
            limited.map {
                it * MAX_VALUE / max
            }
        } else {
            limited
        }

        return final
    }
}
