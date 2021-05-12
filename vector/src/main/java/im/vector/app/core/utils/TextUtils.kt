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

package im.vector.app.core.utils

import android.content.Context
import android.os.Build
import android.text.format.Formatter
import org.threeten.bp.Duration
import java.util.TreeMap

object TextUtils {

    private val suffixes = TreeMap<Int, String>().also {
        it[1000] = "k"
        it[1000000] = "M"
        it[1000000000] = "G"
    }

    fun formatCountToShortDecimal(value: Int): String {
        try {
            if (value < 0) return "-" + formatCountToShortDecimal(-value)
            if (value < 1000) return value.toString() // deal with easy case

            val e = suffixes.floorEntry(value)
            val divideBy = e?.key
            val suffix = e?.value

            val truncated = value / (divideBy!! / 10) // the number part of the output times 10
            val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
            return if (hasDecimal) "${truncated / 10.0}$suffix" else "${truncated / 10}$suffix"
        } catch (t: Throwable) {
            return value.toString()
        }
    }

    /**
     * Since Android O, the system considers that 1ko = 1000 bytes instead of 1024 bytes. We want to avoid that for the moment.
     */
    fun formatFileSize(context: Context, sizeBytes: Long, useShortFormat: Boolean = false): String {
        val normalizedSize = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            sizeBytes
        } else {
            // First convert the size
            when {
                sizeBytes < 1024               -> sizeBytes
                sizeBytes < 1024 * 1024        -> sizeBytes * 1000 / 1024
                sizeBytes < 1024 * 1024 * 1024 -> sizeBytes * 1000 / 1024 * 1000 / 1024
                else                           -> sizeBytes * 1000 / 1024 * 1000 / 1024 * 1000 / 1024
            }
        }

        return if (useShortFormat) {
            Formatter.formatShortFileSize(context, normalizedSize)
        } else {
            Formatter.formatFileSize(context, normalizedSize)
        }
    }

    fun formatDuration(duration: Duration): String {
        val hours = duration.seconds / 3600
        val minutes = (duration.seconds % 3600) / 60
        val seconds = duration.seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
