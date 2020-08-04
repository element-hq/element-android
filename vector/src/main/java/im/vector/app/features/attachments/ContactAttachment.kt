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

package im.vector.app.features.attachments

/**
 * Data class holding values of a picked contact
 * Can be send as a text message waiting for the protocol to handle contact.
 */
data class ContactAttachment(
        val displayName: String,
        val photoUri: String?,
        val phones: List<String> = emptyList(),
        val emails: List<String> = emptyList()
) {

    fun toHumanReadable(): String {
        return buildString {
            append(displayName)
            phones.concatIn(this)
            emails.concatIn(this)
        }
    }

    private fun List<String>.concatIn(stringBuilder: StringBuilder) {
        if (isNotEmpty()) {
            stringBuilder.append("\n")
            for (i in 0 until size - 1) {
                val value = get(i)
                stringBuilder.append(value).append("\n")
            }
            stringBuilder.append(last())
        }
    }
}
