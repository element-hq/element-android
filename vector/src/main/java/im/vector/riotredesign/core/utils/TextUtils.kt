/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.core.utils

import java.util.*

object TextUtils {

    private val suffixes = TreeMap<Int, String>().also {
        it.put(1000, "k")
        it.put(1000000, "M")
        it.put(1000000000, "G")
    }

    fun formatCountToShortDecimal(value: Int): String {
        try {
            if (value < 0) return "-" + formatCountToShortDecimal(-value)
            if (value < 1000) return value.toString() //deal with easy case

            val e = suffixes.floorEntry(value)
            val divideBy = e.key
            val suffix = e.value

            val truncated = value / (divideBy!! / 10) //the number part of the output times 10
            val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
            return if (hasDecimal) "${truncated / 10.0}$suffix" else "${truncated / 10}$suffix"
        } catch (t: Throwable) {
            return value.toString()
        }
    }
}