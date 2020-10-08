/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.api.util

object ContentUtils {
    fun extractUsefulTextFromReply(repliedBody: String): String {
        val lines = repliedBody.lines()
        var wellFormed = repliedBody.startsWith(">")
        var endOfPreviousFound = false
        val usefullines = ArrayList<String>()
        lines.forEach {
            if (it == "") {
                endOfPreviousFound = true
                return@forEach
            }
            if (!endOfPreviousFound) {
                wellFormed = wellFormed && it.startsWith(">")
            } else {
                usefullines.add(it)
            }
        }
        return usefullines.joinToString("\n").takeIf { wellFormed } ?: repliedBody
    }

    fun extractUsefulTextFromHtmlReply(repliedBody: String): String {
        if (repliedBody.startsWith("<mx-reply>")) {
            val closingTagIndex = repliedBody.lastIndexOf("</mx-reply>")
            if (closingTagIndex != -1) {
                return repliedBody.substring(closingTagIndex + "</mx-reply>".length).trim()
            }
        }
        return repliedBody
    }
}
