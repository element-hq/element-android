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

package org.matrix.android.sdk.internal.session.permalinks

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.permalinks.PermalinkService.Companion.MATRIX_TO_URL_BASE
import org.matrix.android.sdk.internal.di.UserId
import javax.inject.Inject

internal class PermalinkFactory @Inject constructor(
        @UserId
        private val userId: String,
        private val viaParameterFinder: ViaParameterFinder
) {

    fun createPermalink(event: Event): String? {
        if (event.roomId.isNullOrEmpty() || event.eventId.isNullOrEmpty()) {
            return null
        }
        return createPermalink(event.roomId, event.eventId)
    }

    fun createPermalink(id: String): String? {
        return if (id.isEmpty()) {
            null
        } else MATRIX_TO_URL_BASE + escape(id)
    }

    fun createRoomPermalink(roomId: String, via: List<String>? = null): String? {
        return if (roomId.isEmpty()) {
            null
        } else {
            buildString {
                append(MATRIX_TO_URL_BASE)
                append(escape(roomId))
                append(
                        via?.takeIf { it.isNotEmpty() }?.let { viaParameterFinder.asUrlViaParameters(it) }
                                ?: viaParameterFinder.computeViaParams(userId, roomId)
                )
            }
        }
    }

    fun createPermalink(roomId: String, eventId: String): String {
        return MATRIX_TO_URL_BASE + escape(roomId) + "/" + escape(eventId) + viaParameterFinder.computeViaParams(userId, roomId)
    }

    fun getLinkedId(url: String): String? {
        val isSupported = url.startsWith(MATRIX_TO_URL_BASE)

        return if (isSupported) {
            url.substring(MATRIX_TO_URL_BASE.length)
        } else null
    }

    /**
     * Escape '/' in id, because it is used as a separator
     *
     * @param id the id to escape
     * @return the escaped id
     */
    private fun escape(id: String): String {
        return id.replace("/", "%2F")
    }

    /**
     * Unescape '/' in id
     *
     * @param id the id to escape
     * @return the escaped id
     */
    private fun unescape(id: String): String {
        return id.replace("%2F", "/")
    }
}
