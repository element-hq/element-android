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

package org.matrix.android.sdk.api.session.space.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 *  "content": {
 *      "via": ["example.com"],
 *      "order": "abcd",
 *      "default": true
 *  }
 */
@JsonClass(generateAdapter = true)
data class SpaceChildContent(
        /**
         * Key which gives a list of candidate servers that can be used to join the room
         * Children where via is not present are ignored.
         */
        @Json(name = "via") val via: List<String>? = null,
        /**
         * The order key is a string which is used to provide a default ordering of siblings in the room list.
         * (Rooms are sorted based on a lexicographic ordering of order values; rooms with no order come last.
         * orders which are not strings, or do not consist solely of ascii characters in the range \x20 (space) to \x7F (~),
         * or consist of more than 50 characters, are forbidden and should be ignored if received.)
         */
        @Json(name = "order") val order: String? = null,
        /**
         * The auto_join flag on a child listing allows a space admin to list the sub-spaces and rooms in that space which should
         * be automatically joined by members of that space.
         * (This is not a force-join, which are descoped for a future MSC; the user can subsequently part these room if they desire.)
         */
        @Json(name = "auto_join") val autoJoin: Boolean? = false,

        /**
         * If `suggested` is set to `true`, that indicates that the child should be advertised to
         * members of the space by the client. This could be done by showing them eagerly
         * in the room list. This is should be ignored if `auto_join` is set to `true`.
         */
        @Json(name = "suggested") val suggested: Boolean? = false
) {
    /**
     * Orders which are not strings, or do not consist solely of ascii characters in the range \x20 (space) to \x7F (~),
     * or consist of more than 50 characters, are forbidden and should be ignored if received.)
     */
    fun validOrder(): String? {
        return order
                ?.takeIf { it.length <= 50 }
                ?.takeIf { ORDER_VALID_CHAR_REGEX.matches(it) }
    }

    companion object {
        private val ORDER_VALID_CHAR_REGEX = "[ -~]+".toRegex()
    }
}
