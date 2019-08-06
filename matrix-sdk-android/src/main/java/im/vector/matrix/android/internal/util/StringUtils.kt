/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.util

import im.vector.matrix.android.api.MatrixPatterns
import timber.log.Timber

/**
 * Convert a string to an UTF8 String
 *
 * @param s the string to convert
 * @return the utf-8 string
 */
fun convertToUTF8(s: String): String? {
    return try {
        val bytes = s.toByteArray(Charsets.UTF_8)
        String(bytes)
    } catch (e: Exception) {
        Timber.e(e, "## convertToUTF8()  failed")
        null
    }
}

/**
 * Convert a string from an UTF8 String
 *
 * @param s the string to convert
 * @return the utf-16 string
 */
fun convertFromUTF8(s: String): String? {
    return try {
        val bytes = s.toByteArray()
        String(bytes, Charsets.UTF_8)
    } catch (e: Exception) {
        Timber.e(e, "## convertFromUTF8()  failed")
        null
    }
}

fun String?.firstLetterOfDisplayName(): String {
    if (this.isNullOrEmpty()) return ""
    val isUserId = MatrixPatterns.isUserId(this)
    val firstLetterIndex = if (isUserId) 1 else 0
    return this[firstLetterIndex].toString().toUpperCase()
}